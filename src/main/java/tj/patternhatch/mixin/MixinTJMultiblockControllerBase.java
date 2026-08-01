package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.patternhatch.machine.PatternMachineLogic;

/**
 * TJ 平行机接入补丁：
 * 1) 崩溃保护：getInputBus(index) 直接 getAbilities(IMPORT_ITEMS).get(index)，
 *    无输入总线时抛 IndexOutOfBoundsException 崩服 -> 越界时返回空处理器；
 * 2) 样板仓支持：getImportItemInventory / getImportFluidTank / getInputBus
 *    在有活动样板槽时重定向到该槽的摊平缓存 + 催化剂 + 电路视图，
 *    空闲时回退原输入（普通总线/仓，手动合成照旧）。
 * 使用 @Pseudo + targets：TJ 类缺失时本补丁自动跳过，不影响其他整合包。
 */
@Pseudo
@Mixin(targets = "tj.builder.multicontrollers.TJMultiblockControllerBase")
public abstract class MixinTJMultiblockControllerBase {

    private static final IItemHandlerModifiable EMPTY_ITEMS = new ItemStackHandler(0);

    @Shadow
    protected IItemHandlerModifiable importItemInventory;

    @Shadow
    protected IMultipleTankHandler importFluidTank;

    @Inject(method = "getInputBus", at = @At("HEAD"), cancellable = true)
    private void patternhatch$guardEmptyInputBus(int index, CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        try {
            MultiblockControllerBase controller = (MultiblockControllerBase) (Object) this;
            Object raw = controller.getAbilities(MultiblockAbility.IMPORT_ITEMS);
            IItemHandlerModifiable original;
            if (index < 0 || raw == null || !(raw instanceof java.util.List)
                    || index >= ((java.util.List<?>) raw).size()
                    || !(((java.util.List<?>) raw).get(index) instanceof IItemHandlerModifiable)) {
                original = EMPTY_ITEMS;
            } else {
                original = (IItemHandlerModifiable) ((java.util.List<?>) raw).get(index);
            }
            cir.setReturnValue(PatternMachineLogic.getTJInputBus(controller, index, original));
        } catch (Exception ignored) {
            cir.setReturnValue(EMPTY_ITEMS);
        }
    }

    @Inject(method = "getImportItemInventory", at = @At("HEAD"), cancellable = true)
    private void patternhatch$redirectImportItems(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        try {
            MultiblockControllerBase controller = (MultiblockControllerBase) (Object) this;
            cir.setReturnValue(PatternMachineLogic.getTJInputInventory(controller, this.importItemInventory));
        } catch (Exception ignored) {
            cir.setReturnValue(this.importItemInventory);
        }
    }

    @Inject(method = "getImportFluidTank", at = @At("HEAD"), cancellable = true)
    private void patternhatch$redirectImportFluids(CallbackInfoReturnable<IMultipleTankHandler> cir) {
        try {
            MultiblockControllerBase controller = (MultiblockControllerBase) (Object) this;
            cir.setReturnValue(PatternMachineLogic.getTJInputFluidInventory(controller, this.importFluidTank));
        } catch (Exception ignored) {
            cir.setReturnValue(this.importFluidTank);
        }
    }
}
