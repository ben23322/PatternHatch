package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.patternhatch.machine.PatternMachineLogic;

/**
 * 大型并行机（Gregicality LargeSimple 系）在样板仓活动槽驱动时禁用并行：
 * 并行机会按输入量把配方放大（如 48x 压缩板），而样板仓每次只推一份材料，
 * 放大后消耗匹配失败导致机器卡死。活动槽模式按原配方 1 倍执行。
 */
@Mixin(targets = "gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController$LargeSimpleMultiblockRecipeLogic")
public abstract class MixinLargeSimpleNoParallel {

    @Inject(method = "createRecipe", at = @At("HEAD"), cancellable = true)
    private void patternhatch$disableParallel(long maxVoltage, IItemHandlerModifiable inputs,
                                              IMultipleTankHandler tanks, Recipe recipe,
                                              CallbackInfoReturnable<Recipe> cir) {
        try {
            MetaTileEntity mte = ((gregtech.api.capability.impl.AbstractRecipeLogic) (Object) this)
                    .getMetaTileEntity();
            if (mte instanceof RecipeMapMultiblockController
                    && PatternMachineLogic.getActiveInputView((RecipeMapMultiblockController) mte) != null) {
                cir.setReturnValue(recipe);
            }
        } catch (Exception ignored) {
        }
    }
}
