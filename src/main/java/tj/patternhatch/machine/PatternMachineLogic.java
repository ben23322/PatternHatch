package tj.patternhatch.machine;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import tj.patternhatch.api.IPatternHatch;
import tj.patternhatch.api.IPatternHatchMachineAccess;
import tj.patternhatch.api.PatternHatchAbilities;
import tj.patternhatch.pattern.PatternSlotEntry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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

    private static final Map<MultiblockControllerBase, ActiveSlot> ACTIVE = new IdentityHashMap<>();
    private static final IItemHandlerModifiable EMPTY_ITEMS = new ItemStackHandler(0);
    private static final IMultipleTankHandler EMPTY_FLUIDS = new FluidTankList(false, new IFluidTank[0]);

    private PatternMachineLogic() {
    }

    /** 每 tick 由 Mixin 调用：空闲时选择下一个可执行槽位。 */
    public static void onTick(MultiblockControllerBase controller) {
        if (!(controller instanceof RecipeMapMultiblockController)) {
            return;
        }
        RecipeMapMultiblockController rc = (RecipeMapMultiblockController) controller;
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
        if (selected != null) {
            ACTIVE.put(controller, selected);
            try {
                Recipe found = rc.recipeMap.searchRecipe(
                        voltage,
                        buildItemView(selected.hatch, selected.slotIndex),
                        buildFluidView(selected.hatch, selected.slotIndex),
                        minTank,
                        false);
                System.out.println("[PatternHatch] M3 select slot=" + selected.slotIndex
                        + " recipe=" + (found != null ? found.getOutputs() : "null"));
            } catch (Exception ignored) {
                System.out.println("[PatternHatch] M3 select slot=" + selected.slotIndex);
            }
        } else {
            ACTIVE.remove(controller);
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

    /** Mixin 的 getInputFluidInventory 重定向。 */
    public static IMultipleTankHandler getInputFluidInventory(RecipeMapMultiblockController rc, IMultipleTankHandler original) {
        ActiveSlot active = ACTIVE.get(rc);
        if (active != null) {
            return buildFluidView(active.hatch, active.slotIndex);
        }
        // 没有活动槽时回退到机器原有流体输入（普通输入仓），保证手动流体合成可用
        return original != null ? original : EMPTY_FLUIDS;
    }

    private static IItemHandlerModifiable buildItemView(IPatternHatch hatch, int slotIndex) {
        List<IItemHandler> handlers = new ArrayList<>();
        handlers.add(hatch.getPatternSlots().get(slotIndex).getItemCache());
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
