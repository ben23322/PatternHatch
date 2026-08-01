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
 * 多配方并行机（MultiRecipeMapMultiblockRecipeLogic，如大型压板成型机）：
 * 活动槽驱动时禁用并行放大，按原配方 1 倍执行。
 */
@Mixin(targets = "gregicadditions.machines.multi.simple.MultiRecipeMapMultiblockController$MultiRecipeMapMultiblockRecipeLogic")
public abstract class MixinMultiRecipeMapNoParallel {

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
