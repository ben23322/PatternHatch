package tj.patternhatch.mixin;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防御性补丁（TJ 平行机崩溃保护）：
 * TJ 的 TJMultiblockControllerBase.getInputBus(index) 直接
 * getAbilities(IMPORT_ITEMS).get(index)，distinct 模式下当多方块没有任何输入总线
 * （例如输入总线位置被样板仓等无 IMPORT_ITEMS 能力的部件占用、或结构刚成型还没接总线）
 * 时抛 IndexOutOfBoundsException 把服务器干崩。这里在列表为空时返回空物品处理器，
 * 让机器安静等待输入而不崩溃。
 * 使用 @Pseudo + targets：TJ 类缺失时本补丁自动跳过，不影响其他整合包。
 */
@Pseudo
@Mixin(targets = "tj.builder.multicontrollers.TJMultiblockControllerBase")
public abstract class MixinTJMultiblockControllerBase {

    private static final IItemHandlerModifiable EMPTY_ITEMS = new ItemStackHandler(0);

    @Inject(method = "getInputBus", at = @At("HEAD"), cancellable = true)
    private void patternhatch$guardEmptyInputBus(int index, CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        try {
            MultiblockControllerBase controller = (MultiblockControllerBase) (Object) this;
            if (controller.getAbilities(MultiblockAbility.IMPORT_ITEMS).isEmpty()) {
                cir.setReturnValue(EMPTY_ITEMS);
            }
        } catch (Exception ignored) {
        }
    }
}
