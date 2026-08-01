package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 诊断：直接探测 Recipe.matchesFound/matchesItems 的失败点。 */
@Mixin(Recipe.class)
public abstract class MixinRecipeDebug {

    @Inject(method = "matchesFound", at = @At("RETURN"))
    private void patternhatch$logMatchesFound(boolean consume, IItemHandlerModifiable inputs,
                                              IMultipleTankHandler tanks,
                                              CallbackInfoReturnable<Boolean> cir) {
        Recipe r = (Recipe) (Object) this;
        System.out.println("[PatternHatch] Recipe.matchesFound -> " + cir.getReturnValue()
                + " consume=" + consume
                + " recipe=" + r.getOutputs()
                + " inputSlots=" + (inputs != null ? inputs.getSlots() : -1));
    }

    @Inject(method = "matchesItems", at = @At("RETURN"))
    private void patternhatch$logMatchesItems(IItemHandlerModifiable inputs,
                                              CallbackInfoReturnable<Boolean> cir) {
        Recipe r = (Recipe) (Object) this;
        if (!cir.getReturnValue()) {
            StringBuilder view = new StringBuilder();
            if (inputs != null) {
                for (int i = 0; i < inputs.getSlots(); i++) {
                    ItemStack s = inputs.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        view.append("[").append(i).append("]").append(s.getCount()).append("x")
                                .append(s.getItem().getRegistryName()).append("@")
                                .append(s.getItemDamage()).append(" ");
                    }
                }
            }
            System.out.println("[PatternHatch] matchesItems FALSE recipe=" + r.getOutputs()
                    + " recipeInputs=" + r.getInputs()
                    + " view=" + view);
        }
    }
}
