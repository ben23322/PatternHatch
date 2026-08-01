package tj.patternhatch.mixin;

import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.recipes.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 诊断：打印 setupAndConsumeRecipeInputs 结果与机器能量，定位启动失败原因。 */
@Mixin(MultiblockRecipeLogic.class)
public abstract class MixinMultiblockRecipeLogicDebug {

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
