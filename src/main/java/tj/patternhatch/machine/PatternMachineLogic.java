package tj.patternhatch.machine;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import tj.patternhatch.api.IPatternHatch;
import tj.patternhatch.api.IPatternHatchMachineAccess;
import tj.patternhatch.api.PatternHatchAbilities;
import tj.patternhatch.pattern.FlattenedCacheView;
import tj.patternhatch.pattern.PatternSlotEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import tj.patternhatch.util.PatternHatchDebug;

/**
 * M3 每槽隔离：机器空闲时按样板优先级挑选一个“活动槽”，
 * 把机器的输入视图重定向为该槽缓存（+共享催化剂/电路），
 * 原版配方引擎照常工作，但只消耗活动槽的材料，杜绝串料。
 */
public final class PatternMachineLogic {

    private static final class ActiveSlot {
        final IPatternHatch hatch;
        final int slotIndex;

        ActiveSlot(IPatternHatch hatch, int slotIndex) {
            this.hatch = hatch;
            this.slotIndex = slotIndex;
        }
    }

    /** TJ 平行机活动槽状态：用于检测活动槽/配方图/内容变化，决定是否清配方 LRU。 */
    private static final class TJState {
        int mapIndex = -1;
        int slotIndex = -1;
        Object hatchRef;
        String contentFingerprint = "";
    }

    private static final Map<MultiblockControllerBase, ActiveSlot> ACTIVE = new IdentityHashMap<>();
    private static final Map<MultiblockControllerBase, Integer> IDLE_TICKS = new IdentityHashMap<>();
    /** 活动槽保持锁：缓存暂时为空时继续锁住活动槽，防止机器去吃普通总线导致增产。 */
    private static final Map<MultiblockControllerBase, Integer> HOLD_TICKS = new IdentityHashMap<>();
    private static final IItemHandlerModifiable EMPTY_ITEMS = new ItemStackHandler(0);
    private static final IMultipleTankHandler EMPTY_FLUIDS = new FluidTankList(false, new IFluidTank[0]);
    /** 空闲超过该 tick 数且缓存有残余时，自动弹回 AE（5 秒）。 */
    private static final int IDLE_RETURN_TICKS = 100;
    /** 活动槽保持锁时长（5 秒）：期间机器只等 AE 推料，不回退到普通输入。 */
    private static final int HOLD_TICKS_DEFAULT = 100;
    /** TJ 平行机样板仓支持开关（config: tjParallel.enabled），默认开启。 */
    public static boolean TJ_PARALLEL_ENABLED = true;
    /** 反射方法缓存：避免每 tick 每台机器重复 getMethod。 */
    private static final Map<Class<?>, Map<String, Method>> TJ_METHOD_CACHE = new HashMap<>();
    /** 反射字段缓存（recipeLogic 等 protected 字段）。 */
    private static final Map<Class<?>, Map<String, Field>> TJ_FIELD_CACHE = new HashMap<>();
    /** 每台 TJ 平行机的活动槽状态。 */
    private static final Map<MultiblockControllerBase, TJState> TJ_STATE = new IdentityHashMap<>();

    private PatternMachineLogic() {
    }

    /** 每 tick 由 Mixin 调用：空闲时选择下一个可执行槽位（GTCEu 系 + TJ 平行机双支持）。 */
    public static void onTick(MultiblockControllerBase controller) {
        if (controller instanceof RecipeMapMultiblockController) {
            onTickGTCEu((RecipeMapMultiblockController) controller);
        } else {
            onTickTJ(controller);
        }
    }

    /** GTCEu 系多方块：RecipeMapMultiblockController 活动槽选择。 */
    private static void onTickGTCEu(RecipeMapMultiblockController rc) {
        MultiblockControllerBase controller = rc;
        List<IPatternHatch> hatches = rc.getAbilities(PatternHatchAbilities.PATTERN_HATCH);
        if (hatches.isEmpty()) {
            ACTIVE.remove(controller);
            return;
        }
        MultiblockRecipeLogic workable = ((IPatternHatchMachineAccess) rc).patternhatch$getWorkable();
        if (workable == null) {
            return;
        }
        // 编程电路（NBT Configuration）配方的“优化哈希查找”不可靠（MapItemStackIngredient 忽略 NBT），
        // 样板仓模式下强制用与 onTick 相同的线性查找，否则带电路的配方机器不识别。
        workable.setUseOptimizedRecipeLookUp(false);
        if (workable.isActive() || !workable.isWorkingEnabled()) {
            return; // 加工中或停用：保持当前活动槽不变
        }
        long voltage = rc.getEnergyContainer() != null ? rc.getEnergyContainer().getInputVoltage() : Long.MAX_VALUE;
        int minTank = minTankCapacity(rc.getOutputFluidInventory());
        ActiveSlot selected = null;
        outer:
        for (IPatternHatch hatch : hatches) {
            for (PatternSlotEntry entry : hatch.getPatternSlots()) {
                if (isSlotEmpty(hatch, entry.getSlotIndex())) {
                    continue; // 空槽不搜索，省开销
                }
                try {
                    Recipe recipe = rc.recipeMap.searchRecipe(
                            voltage,
                            buildItemView(hatch, entry.getSlotIndex()),
                            buildFluidView(hatch, entry.getSlotIndex()),
                            minTank,
                            false);
                    if (recipe != null) {
                        selected = new ActiveSlot(hatch, entry.getSlotIndex());
                        break outer;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        ActiveSlot prev = ACTIVE.get(controller);
        if (selected != null) {
            ACTIVE.put(controller, selected);
            IDLE_TICKS.put(controller, 0);
            HOLD_TICKS.put(controller, HOLD_TICKS_DEFAULT);
            if (prev == null || prev.hatch != selected.hatch || prev.slotIndex != selected.slotIndex) {
                // 活动槽变化：清掉机器的配方 LRU 缓存并强制重搜，
                // 否则会命中与活动槽内容匹配的旧配方（如 22x/48x 压缩钢板）导致启动失败
                try {
                    workable.previousRecipe.clear();
                    workable.forceRecipeRecheck();
                } catch (Exception ignored) {
                }
            }
            try {
                Recipe found = rc.recipeMap.searchRecipe(
                        voltage,
                        buildItemView(selected.hatch, selected.slotIndex),
                        buildFluidView(selected.hatch, selected.slotIndex),
                        minTank,
                        false);
                PatternHatchDebug.log("[PatternHatch] M3 select slot=" + selected.slotIndex
                        + " recipe=" + (found != null ? found.getOutputs() : "null")
                        + " circuit=" + selected.hatch.getCircuitInventory().getStackInSlot(0)
                        + " energy=" + (rc.getEnergyContainer() != null
                        ? rc.getEnergyContainer().getEnergyStored() : -1)
                        + " machine=" + controller.getClass().getSimpleName()
                        + " workable=" + workable.getClass().getSimpleName()
                        + " formed=" + rc.isStructureFormed()
                        + " wEnabled=" + workable.isWorkingEnabled()
                        + " active=" + workable.isActive()
                        + " progress=" + workable.getProgress()
                        + " maxProgress=" + workable.getMaxProgress());
            } catch (Exception ignored) {
                PatternHatchDebug.log("[PatternHatch] M3 select slot=" + selected.slotIndex);
            }
        } else {
            int hold = HOLD_TICKS.getOrDefault(controller, 0);
            if (hold > 0) {
                // 锁住当前活动槽（缓存暂时为空）：机器只等 AE 推下一批，不吃普通总线
                HOLD_TICKS.put(controller, hold - 1);
            } else {
                if (prev != null) {
                    ACTIVE.remove(controller);
                    try {
                        workable.previousRecipe.clear();
                        workable.forceRecipeRecheck();
                    } catch (Exception ignored) {
                    }
                }
                // 空闲自动弹回：机器无活动槽且缓存有残余（如 <9 的尾料）时，自动退回 AE
                int idle = IDLE_TICKS.getOrDefault(controller, 0) + 1;
                IDLE_TICKS.put(controller, idle);
                if (idle >= IDLE_RETURN_TICKS) {
                    IDLE_TICKS.put(controller, 0);
                    boolean returned = false;
                    for (IPatternHatch h : hatches) {
                        if (h.hasCachedItems()) {
                            h.returnCacheToAE();
                            returned = true;
                        }
                    }
                    if (returned) {
                        PatternHatchDebug.log("[PatternHatch] idle auto-return to AE");
                    }
                }
                // 诊断：无活动槽时打印各样板槽缓存残余，便于核对是否清空
                if (hatches.size() > 0 && rc.getTimer() % 200 == 0) {
                    StringBuilder sb = new StringBuilder();
                    for (IPatternHatch h : hatches) {
                        for (PatternSlotEntry e : h.getPatternSlots()) {
                            int total = 0;
                            for (int i = 0; i < e.getItemCache().getSlots(); i++) {
                                if (!e.getItemCache().getStackInSlot(i).isEmpty()) {
                                    total += e.getItemCache().getStackInSlot(i).getCount();
                                }
                            }
                            if (total > 0) {
                                sb.append("slot").append(e.getSlotIndex()).append("=").append(total).append(" ");
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        PatternHatchDebug.log("[PatternHatch] idle cache leftovers: " + sb);
                    }
                }
            }
        }
    }

    /** TJ 平行机（ParallelRecipeMapMultiblockController 等，不继承 RecipeMapMultiblockController）。 */
    private static void onTickTJ(MultiblockControllerBase controller) {
        if (!TJ_PARALLEL_ENABLED) {
            return;
        }
        List<IPatternHatch> hatches = controller.getAbilities(PatternHatchAbilities.PATTERN_HATCH);
        if (hatches.isEmpty()) {
            ACTIVE.remove(controller);
            IDLE_TICKS.remove(controller);
            HOLD_TICKS.remove(controller);
            return;
        }
        if (isTJActive(controller) || !isTJWorkingEnabled(controller)) {
            return; // 加工中或停用：保持当前活动槽不变（hold 冻结，防缓存空档期吃普通总线）
        }
        RecipeMap<?> recipeMap = getTJRecipeMap(controller);
        if (recipeMap == null) {
            return;
        }
        long voltage = getTJVoltage(controller);
        int minTank = minTankCapacity(getTJExportFluidTank(controller));
        ActiveSlot selected = null;
        outer:
        for (IPatternHatch hatch : hatches) {
            for (PatternSlotEntry entry : hatch.getPatternSlots()) {
                if (isSlotEmpty(hatch, entry.getSlotIndex())) {
                    continue;
                }
                try {
                    Recipe recipe = recipeMap.searchRecipe(
                            voltage,
                            buildItemView(hatch, entry.getSlotIndex()),
                            buildFluidView(hatch, entry.getSlotIndex()),
                            minTank,
                            false);
                    if (recipe != null) {
                        selected = new ActiveSlot(hatch, entry.getSlotIndex());
                        break outer;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        ActiveSlot prev = ACTIVE.get(controller);
        if (selected != null) {
            ACTIVE.put(controller, selected);
            IDLE_TICKS.put(controller, 0);
            HOLD_TICKS.put(controller, HOLD_TICKS_DEFAULT);
            // TJ 的 ParallelRecipeLRUCache 按"内容子集匹配"返回最近命中配方：
            // 活动槽切换、模式（配方图）切换、或槽内材料类型变化时，旧配方可能被
            // 新内容顺带满足 -> 机器跑旧配方 -> 合成错误。此时清掉 LRU 强制重搜。
            TJState state = TJ_STATE.computeIfAbsent(controller, k -> new TJState());
            int mapIndex = getTJMapIndex(controller);
            String fingerprint = slotContentFingerprint(selected.hatch, selected.slotIndex);
            boolean stale = state.hatchRef != selected.hatch
                    || state.slotIndex != selected.slotIndex
                    || state.mapIndex != mapIndex
                    || !state.contentFingerprint.equals(fingerprint);
            state.hatchRef = selected.hatch;
            state.slotIndex = selected.slotIndex;
            state.mapIndex = mapIndex;
            state.contentFingerprint = fingerprint;
            if (stale) {
                clearTJRecipeCache(controller);
                PatternHatchDebug.log("[PatternHatch] TJ recipe LRU cleared (slot/map/content changed)");
            }
            PatternHatchDebug.log("[PatternHatch] TJ select slot=" + selected.slotIndex
                    + " machine=" + controller.getClass().getSimpleName()
                    + " hatches=" + hatches.size());
        } else {
            int hold = HOLD_TICKS.getOrDefault(controller, 0);
            if (hold > 0) {
                HOLD_TICKS.put(controller, hold - 1);
            } else {
                if (prev != null) {
                    ACTIVE.remove(controller);
                }
                int idle = IDLE_TICKS.getOrDefault(controller, 0) + 1;
                IDLE_TICKS.put(controller, idle);
                if (idle >= IDLE_RETURN_TICKS) {
                    IDLE_TICKS.put(controller, 0);
                    boolean returned = false;
                    for (IPatternHatch h : hatches) {
                        if (h.hasCachedItems()) {
                            h.returnCacheToAE();
                            returned = true;
                        }
                    }
                    if (returned) {
                        PatternHatchDebug.log("[PatternHatch] TJ idle auto-return to AE");
                    }
                }
            }
        }
    }

    /** Mixin 的 getInputInventory 重定向：有活动槽用该槽视图，有样板仓但无可执行槽用空视图。 */
    public static IItemHandlerModifiable getInputInventory(RecipeMapMultiblockController rc, IItemHandlerModifiable original) {
        ActiveSlot active = ACTIVE.get(rc);
        if (active != null) {
            return buildItemView(active.hatch, active.slotIndex);
        }
        // 没有活动槽时回退到机器原有输入（普通输入总线/输入仓），保证手动合成不受样板仓影响
        return original != null ? original : EMPTY_ITEMS;
    }

    /** 诊断辅助：返回当前活动槽视图（无活动槽返回 null）。 */
    public static IItemHandlerModifiable getActiveInputView(RecipeMapMultiblockController rc) {
        ActiveSlot active = ACTIVE.get(rc);
        if (active != null) {
            return buildItemView(active.hatch, active.slotIndex);
        }
        return null;
    }

    /** Mixin 的 getInputFluidInventory 重定向。 */
    public static IMultipleTankHandler getInputFluidInventory(RecipeMapMultiblockController rc, IMultipleTankHandler original) {
        ActiveSlot active = ACTIVE.get(rc);
        if (active != null) {
            return buildFluidView(active.hatch, active.slotIndex);
        }
        // 没有活动槽时回退到机器原有流体输入（普通输入仓），保证手动流体合成可用
        return original != null ? original : EMPTY_FLUIDS;
    }

    // ---------- TJ 平行机输入重定向 ----------

    /** TJ getImportItemInventory 重定向：有活动槽用该槽视图，空闲回退原合并输入。 */
    public static IItemHandlerModifiable getTJInputInventory(MultiblockControllerBase controller, IItemHandlerModifiable original) {
        if (!TJ_PARALLEL_ENABLED) {
            return original != null ? original : EMPTY_ITEMS;
        }
        ActiveSlot active = ACTIVE.get(controller);
        if (active != null) {
            return buildItemView(active.hatch, active.slotIndex);
        }
        return original != null ? original : EMPTY_ITEMS;
    }

    /** TJ getInputBus 重定向：有活动槽时任何总线索引都返回活动槽视图（distinct 也走样板），空闲回退原总线。 */
    public static IItemHandlerModifiable getTJInputBus(MultiblockControllerBase controller, int index, IItemHandlerModifiable original) {
        if (!TJ_PARALLEL_ENABLED) {
            return original != null ? original : EMPTY_ITEMS;
        }
        ActiveSlot active = ACTIVE.get(controller);
        if (active != null) {
            return buildItemView(active.hatch, active.slotIndex);
        }
        return original != null ? original : EMPTY_ITEMS;
    }

    /** TJ getImportFluidTank 重定向：有活动槽用该槽流体缓存视图，空闲回退原合并流体。 */
    public static IMultipleTankHandler getTJInputFluidInventory(MultiblockControllerBase controller, IMultipleTankHandler original) {
        if (!TJ_PARALLEL_ENABLED) {
            return original != null ? original : EMPTY_FLUIDS;
        }
        ActiveSlot active = ACTIVE.get(controller);
        if (active != null) {
            return buildFluidView(active.hatch, active.slotIndex);
        }
        return original != null ? original : EMPTY_FLUIDS;
    }

    // ---------- TJ 反射辅助（TJ 类不在编译 classpath，运行时按方法名取） ----------

    private static boolean isTJActive(MultiblockControllerBase controller) {
        Object value = invokeTJ(controller, "isActive");
        return value instanceof Boolean && (Boolean) value;
    }

    private static boolean isTJWorkingEnabled(MultiblockControllerBase controller) {
        Object value = invokeTJ(controller, "isWorkingEnabled");
        return !(value instanceof Boolean) || (Boolean) value;
    }

    private static RecipeMap<?> getTJRecipeMap(MultiblockControllerBase controller) {
        Object value = invokeTJ(controller, "getRecipeMap");
        return value instanceof RecipeMap ? (RecipeMap<?>) value : null;
    }

    private static int getTJMapIndex(MultiblockControllerBase controller) {
        Object value = invokeTJ(controller, "getRecipeMapIndex");
        return value instanceof Integer ? (Integer) value : -1;
    }

    private static long getTJVoltage(MultiblockControllerBase controller) {
        Object value = invokeTJ(controller, "getInputEnergyContainer");
        return value instanceof IEnergyContainer ? ((IEnergyContainer) value).getInputVoltage() : Long.MAX_VALUE;
    }

    private static IMultipleTankHandler getTJExportFluidTank(MultiblockControllerBase controller) {
        Object value = invokeTJ(controller, "getExportFluidTank");
        return value instanceof IMultipleTankHandler ? (IMultipleTankHandler) value : null;
    }

    private static Object invokeTJ(MultiblockControllerBase controller, String methodName) {
        try {
            Class<?> clazz = controller.getClass();
            Map<String, Method> cache = TJ_METHOD_CACHE.computeIfAbsent(clazz, k -> new HashMap<>());
            Method method = cache.get(methodName);
            if (method == null) {
                method = clazz.getMethod(methodName);
                cache.put(methodName, method);
            }
            return method.invoke(controller);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 活动槽缓存的内容类型指纹：只含物品种类/流体种类，不含数量（消耗中数量变化不触发重搜）。 */
    private static String slotContentFingerprint(IPatternHatch hatch, int slotIndex) {
        try {
            PatternSlotEntry entry = hatch.getPatternSlots().get(slotIndex);
            java.util.Set<String> types = new java.util.TreeSet<>();
            for (int i = 0; i < entry.getItemCache().getSlots(); i++) {
                net.minecraft.item.ItemStack s = entry.getItemCache().getStackInSlot(i);
                if (!s.isEmpty()) {
                    types.add("i:" + s.getItem().getRegistryName() + "@" + s.getItemDamage());
                }
            }
            for (net.minecraftforge.fluids.IFluidTank tank : entry.getFluidCache().getTanks()) {
                net.minecraftforge.fluids.FluidStack f = tank.getFluid();
                if (f != null) {
                    types.add("f:" + f.getFluid().getName());
                }
            }
            return types.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    /** 反射调用 ParallelRecipeLogic.getRecipeLRUCache().clear()，清掉 TJ 的配方 LRU。 */
    private static void clearTJRecipeCache(MultiblockControllerBase controller) {
        try {
            Class<?> clazz = controller.getClass();
            Map<String, Field> fieldCache = TJ_FIELD_CACHE.computeIfAbsent(clazz, k -> new HashMap<>());
            Field recipeLogicField = fieldCache.get("recipeLogic");
            if (recipeLogicField == null) {
                Class<?> scan = clazz;
                while (scan != null) {
                    try {
                        recipeLogicField = scan.getDeclaredField("recipeLogic");
                        break;
                    } catch (NoSuchFieldException ignored) {
                        scan = scan.getSuperclass();
                    }
                }
                if (recipeLogicField == null) {
                    return;
                }
                recipeLogicField.setAccessible(true);
                fieldCache.put("recipeLogic", recipeLogicField);
            }
            Object logic = recipeLogicField.get(controller);
            if (logic == null) {
                return;
            }
            Method getCache = getTJLogicMethod(logic, "getRecipeLRUCache");
            if (getCache == null) {
                return;
            }
            Object cache = getCache.invoke(logic);
            if (cache == null) {
                return;
            }
            Method clear = getTJLogicMethod(cache, "clear");
            if (clear != null) {
                clear.invoke(cache);
            }
        } catch (Exception ignored) {
        }
    }

    private static Method getTJLogicMethod(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static IItemHandlerModifiable buildItemView(IPatternHatch hatch, int slotIndex) {
        List<IItemHandler> handlers = new ArrayList<>();
        // 摊平成 ≤64 一叠的多槽视图，模拟普通输入总线（并行机对单槽大叠处理异常）
        handlers.add(new FlattenedCacheView(hatch.getPatternSlots().get(slotIndex).getItemCache()));
        handlers.add(hatch.getCatalystInventory());
        handlers.add(hatch.getCircuitInventory());
        return new ItemHandlerList(handlers);
    }

    private static IMultipleTankHandler buildFluidView(IPatternHatch hatch, int slotIndex) {
        return new FluidTankList(false, hatch.getPatternSlots().get(slotIndex).getFluidCache().getTanks());
    }

    private static boolean isSlotEmpty(IPatternHatch hatch, int slotIndex) {
        PatternSlotEntry entry = hatch.getPatternSlots().get(slotIndex);
        for (int i = 0; i < entry.getItemCache().getSlots(); i++) {
            if (!entry.getItemCache().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        for (IFluidTank tank : entry.getFluidCache().getTanks()) {
            if (tank.getFluid() != null && tank.getFluidAmount() > 0) {
                return false;
            }
        }
        return true;
    }

    private static int minTankCapacity(IMultipleTankHandler tanks) {
        if (tanks == null) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < tanks.getTanks(); i++) {
            IFluidTank tank = tanks.getTankAt(i);
            if (tank != null) {
                min = Math.min(min, tank.getCapacity());
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }
}
