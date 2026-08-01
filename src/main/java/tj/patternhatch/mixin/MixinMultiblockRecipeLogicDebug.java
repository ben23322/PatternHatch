package tj.patternhatch.mixin;

import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 诊断：打印 setupAndConsumeRecipeInputs 结果与机器能量，定位启动失败原因。 */
@Mixin(MultiblockRecipeLogic.class)
public abstract class MixinMultiblockRecipeLogicDebug {

    @Inject(method = "setupAndConsumeRecipeInputs", at = @At("HEAD"))
    private void patternhatch$logSetupHead(Recipe recipe, CallbackInfoReturnable<Boolean> cir) {
        try {
            MultiblockRecipeLogic logic = (MultiblockRecipeLogic) (Object) this;
            MetaTileEntity mte = logic.getMetaTileEntity();
            boolean checkRecipe = false;
            String machineClass = mte == null ? "null" : mte.getClass().getSimpleName();
            if (mte instanceof RecipeMapMultiblockController) {
                try {
                    checkRecipe = ((RecipeMapMultiblockController) mte).checkRecipe(recipe, false);
                } catch (Exception e) {
                    checkRecipe = false;
                }
            }
            System.out.println("[PatternHatch] machine setup HEAD recipe=" + recipe.getOutputs()
                    + " machine=" + machineClass
                    + " checkRecipe=" + checkRecipe
                    + " lruCount=" + logic.previousRecipe.getCachedRecipeCount());
        } catch (Exception e) {
            System.out.println("[PatternHatch] machine setup HEAD log failed: " + e);
        }
    }

    @Inject(method = "setupAndConsumeRecipeInputs", at = @At("RETURN"))
    private void patternhatch$logSetup(Recipe recipe, CallbackInfoReturnable<Boolean> cir) {
        try {
            MultiblockRecipeLogic logic = (MultiblockRecipeLogic) (Object) this;
            long stored = logic.getEnergyContainer() != null
                    ? logic.getEnergyContainer().getEnergyStored() : -1;
            long cap = logic.getEnergyContainer() != null
                    ? logic.getEnergyContainer().getEnergyCapacity() : -1;
            System.out.println("[PatternHatch] machine setupAndConsume -> " + cir.getReturnValue()
                    + " recipe=" + recipe.getOutputs()
                    + " EUt=" + recipe.getEUt() + " dur=" + recipe.getDuration()
                    + " energy=" + stored + "/" + cap);
        } catch (Exception e) {
            System.out.println("[PatternHatch] machine setupAndConsume -> " + cir.getReturnValue()
                    + " recipe=" + recipe.getOutputs() + " (energy log failed)");
        }
    }
}
