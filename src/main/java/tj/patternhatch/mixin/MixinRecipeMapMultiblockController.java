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
import tj.patternhatch.util.PatternHatchDebug;

/** 多方块配方控制器补丁：活动槽视图 + workable 访问。 */
@Mixin(RecipeMapMultiblockController.class)
public abstract class MixinRecipeMapMultiblockController implements IPatternHatchMachineAccess {

    @Shadow
    protected MultiblockRecipeLogic recipeMapWorkable;

    /** 控制器真实普通输入库存（输入总线等，不含样板仓缓存）。 */
    @Shadow
    protected IItemHandlerModifiable inputInventory;

    /** 控制器真实普通流体输入（输入仓等，不含样板仓缓存罐）。 */
    @Shadow
    protected IMultipleTankHandler inputFluidInventory;

    @Override
    public MultiblockRecipeLogic patternhatch$getWorkable() {
        return recipeMapWorkable;
    }

    @Inject(method = "checkRecipe", at = @At("HEAD"))
    private void patternhatch$checkRecipe(Recipe recipe, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        PatternHatchDebug.log("[PatternHatch] checkRecipe outputs=" + recipe.getOutputs() + " simulate=" + simulate);
    }

    @Inject(method = "getInputInventory", at = @At("HEAD"), cancellable = true)
    private void patternhatch$inputInventory(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        RecipeMapMultiblockController controller = (RecipeMapMultiblockController) (Object) this;
        // 不能依赖 cir.getReturnValue()（HEAD 注入时恒为 null），
        // 直接读取控制器真实的普通输入库存字段。
        cir.setReturnValue(PatternMachineLogic.getInputInventory(controller, this.inputInventory));
    }

    @Inject(method = "getInputFluidInventory", at = @At("HEAD"), cancellable = true)
    private void patternhatch$inputFluidInventory(CallbackInfoReturnable<IMultipleTankHandler> cir) {
        RecipeMapMultiblockController controller = (RecipeMapMultiblockController) (Object) this;
        cir.setReturnValue(PatternMachineLogic.getInputFluidInventory(controller, this.inputFluidInventory));
    }
}
