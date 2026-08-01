package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.AbstractRecipeLogic;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 诊断：打印机器 findRecipe 的结果，定位"找到配方但不启动"的断点。 */
@Mixin(AbstractRecipeLogic.class)
public abstract class MixinAbstractRecipeLogicDebug {

    @Inject(method = "findRecipe", at = @At("RETURN"))
    private void patternhatch$logFindRecipe(long maxVoltage, IItemHandlerModifiable inputs,
                                            IMultipleTankHandler fluidInputs, boolean optimize,
                                            CallbackInfoReturnable<Recipe> cir) {
        Recipe r = cir.getReturnValue();
        System.out.println("[PatternHatch] machine findRecipe -> "
                + (r == null ? "null" : String.valueOf(r.getOutputs()))
                + " optimize=" + optimize + " maxVoltage=" + maxVoltage
                + " inputSlots=" + (inputs != null ? inputs.getSlots() : -1)
                + " fluidTanks=" + (fluidInputs != null ? fluidInputs.getTanks() : -1));
    }

    @Inject(method = "setupAndConsumeRecipeInputs", at = @At("RETURN"))
    private void patternhatch$logSuperSetup(Recipe recipe, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("[PatternHatch] super.setupAndConsume -> " + cir.getReturnValue()
                + " recipe=" + recipe.getOutputs()
                + " EUt=" + recipe.getEUt() + " dur=" + recipe.getDuration());
    }
}
