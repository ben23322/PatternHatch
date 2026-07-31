package tj.patternhatch.mixin;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.patternhatch.api.IPatternHatchMachineAccess;
import tj.patternhatch.machine.PatternMachineLogic;

/** 多方块配方控制器补丁：活动槽视图 + workable 访问。 */
@Mixin(RecipeMapMultiblockController.class)
public abstract class MixinRecipeMapMultiblockController implements IPatternHatchMachineAccess {

    @Shadow
    protected MultiblockRecipeLogic recipeMapWorkable;

    @Override
    public MultiblockRecipeLogic patternhatch$getWorkable() {
        return recipeMapWorkable;
    }

    @Inject(method = "checkRecipe", at = @At("HEAD"))
    private void patternhatch$checkRecipe(Recipe recipe, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("[PatternHatch] checkRecipe outputs=" + recipe.getOutputs() + " simulate=" + simulate);
    }

    @Inject(method = "getInputInventory", at = @At("HEAD"), cancellable = true)
    private void patternhatch$inputInventory(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        RecipeMapMultiblockController controller = (RecipeMapMultiblockController) (Object) this;
        cir.setReturnValue(PatternMachineLogic.getInputInventory(controller, cir.getReturnValue()));
    }

    @Inject(method = "getInputFluidInventory", at = @At("HEAD"), cancellable = true)
    private void patternhatch$inputFluidInventory(CallbackInfoReturnable<IMultipleTankHandler> cir) {
        RecipeMapMultiblockController controller = (RecipeMapMultiblockController) (Object) this;
        cir.setReturnValue(PatternMachineLogic.getInputFluidInventory(controller, cir.getReturnValue()));
    }
}

