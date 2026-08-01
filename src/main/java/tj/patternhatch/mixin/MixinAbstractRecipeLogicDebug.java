package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.AbstractRecipeLogic;
import gregtech.api.recipes.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 诊断：打印机器 findRecipe 的结果，定位"找到配方但不启动"的断点。 */
@Mixin(AbstractRecipeLogic.class)
public abstract class MixinAbstractRecipeLogicDebug {

    @Shadow
    protected abstract IItemHandlerModifiable getInputInventory();

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
        try {
            IItemHandlerModifiable inputs = getInputInventory();
            StringBuilder sb = new StringBuilder();
            if (inputs != null) {
                for (int i = 0; i < inputs.getSlots(); i++) {
                    ItemStack s = inputs.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        sb.append("[").append(i).append("]").append(s.getCount()).append("x")
                                .append(s.getItem().getRegistryName()).append("@")
                                .append(s.getItemDamage()).append(" ");
                    }
                }
            }
            System.out.println("[PatternHatch] super.setupAndConsume -> " + cir.getReturnValue()
                    + " recipe=" + recipe.getOutputs()
                    + " EUt=" + recipe.getEUt() + " dur=" + recipe.getDuration()
                    + " actualInput(" + (inputs != null ? inputs.getSlots() : -1) + ")=" + sb);
        } catch (Exception e) {
            System.out.println("[PatternHatch] super.setupAndConsume -> " + cir.getReturnValue()
                    + " recipe=" + recipe.getOutputs() + " (input log failed)");
        }
    }
}
