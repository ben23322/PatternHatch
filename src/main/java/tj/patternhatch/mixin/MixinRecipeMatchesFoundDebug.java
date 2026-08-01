package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 诊断：探测 matchesFound（唯一重载，完整描述符）。 */
@Mixin(Recipe.class)
public abstract class MixinRecipeMatchesFoundDebug {

    @Inject(method = "matchesFound(ZLnet/minecraftforge/items/IItemHandlerModifiable;"
            + "Lgregtech/api/capability/IMultipleTankHandler;)Z",
            at = @At("RETURN"))
    private void patternhatch$logMatchesFound(boolean consume, IItemHandlerModifiable inputs,
                                              IMultipleTankHandler tanks,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || consume) {
            Recipe r = (Recipe) (Object) this;
            System.out.println("[PatternHatch] Recipe.matchesFound -> " + cir.getReturnValue()
                    + " consume=" + consume
                    + " recipe=" + r.getOutputs()
                    + " inputSlots=" + (inputs != null ? inputs.getSlots() : -1));
        }
    }
}
