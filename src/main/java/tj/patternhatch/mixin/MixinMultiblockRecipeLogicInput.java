package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.patternhatch.machine.PatternMachineLogic;

/**
 * 在工作台（MultiblockRecipeLogic）层重定向输入视图：
 * 覆盖所有多方块，包括重写了控制器 getInputInventory/getInputFluidInventory
 * 的机器（这类机器会绕过基类 mixin，导致看不到样板仓活动槽视图、配方不启动）。
 * 有活动槽 → 活动槽视图；无活动槽 → 回退到机器自身输入。
 */
@Mixin(MultiblockRecipeLogic.class)
public abstract class MixinMultiblockRecipeLogicInput {

    @Shadow
    protected MetaTileEntity metaTileEntity;

    @Inject(method = "getInputInventory", at = @At("HEAD"), cancellable = true)
    private void patternhatch$getInputInventory(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        if (metaTileEntity instanceof RecipeMapMultiblockController) {
            RecipeMapMultiblockController rc = (RecipeMapMultiblockController) metaTileEntity;
            cir.setReturnValue(PatternMachineLogic.getInputInventory(rc, rc.getInputInventory()));
        }
    }

    @Inject(method = "getInputTank", at = @At("HEAD"), cancellable = true)
    private void patternhatch$getInputTank(CallbackInfoReturnable<IMultipleTankHandler> cir) {
        if (metaTileEntity instanceof RecipeMapMultiblockController) {
            RecipeMapMultiblockController rc = (RecipeMapMultiblockController) metaTileEntity;
            cir.setReturnValue(PatternMachineLogic.getInputFluidInventory(rc, rc.getInputFluidInventory()));
        }
    }
}
