package tj.patternhatch.mixin;

import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.patternhatch.machine.PatternMachineLogic;

/** 诊断：打印 setupAndConsumeRecipeInputs 结果与机器能量，定位启动失败原因。 */
@Mixin(MultiblockRecipeLogic.class)
public abstract class MixinMultiblockRecipeLogicDebug {

    @Inject(method = "setupAndConsumeRecipeInputs", at = @At("HEAD"))
    private void patternhatch$logSetupHead(Recipe recipe, CallbackInfoReturnable<Boolean> cir) {
        try {
            MultiblockRecipeLogic logic = (MultiblockRecipeLogic) (Object) this;
            MetaTileEntity mte = logic.getMetaTileEntity();
            String machineClass = mte == null ? "null" : mte.getClass().getSimpleName();
            System.out.println("[PatternHatch] machine setup HEAD recipe=" + recipe.getOutputs()
                    + " machine=" + machineClass
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
            String viewInfo = "";
            if (logic.getMetaTileEntity() instanceof RecipeMapMultiblockController) {
                RecipeMapMultiblockController rc =
                        (RecipeMapMultiblockController) logic.getMetaTileEntity();
                IItemHandlerModifiable view = PatternMachineLogic.getActiveInputView(rc);
                if (view != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < view.getSlots(); i++) {
                        ItemStack s = view.getStackInSlot(i);
                        if (!s.isEmpty()) {
                            sb.append("[").append(i).append("]")
                                    .append(s.getCount()).append("x")
                                    .append(s.getItem().getRegistryName())
                                    .append("@").append(s.getItemDamage())
                                    .append(" ");
                        }
                    }
                    viewInfo = " activeView(" + view.getSlots() + " slots)=" + sb;
                } else {
                    viewInfo = " noActiveSlot";
                }
            }
            System.out.println("[PatternHatch] machine setupAndConsume -> " + cir.getReturnValue()
                    + " recipe=" + recipe.getOutputs()
                    + " EUt=" + recipe.getEUt() + " dur=" + recipe.getDuration()
                    + " energy=" + stored + "/" + cap
                    + viewInfo);
        } catch (Exception e) {
            System.out.println("[PatternHatch] machine setupAndConsume -> " + cir.getReturnValue()
                    + " recipe=" + recipe.getOutputs() + " (energy log failed)");
        }
    }
}
