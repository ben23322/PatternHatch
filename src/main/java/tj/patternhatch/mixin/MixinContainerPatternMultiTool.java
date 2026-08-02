package tj.patternhatch.mixin;

import appeng.helpers.IInterfaceHost;
import co.neeve.nae2.common.containers.ContainerPatternMultiTool;
import co.neeve.nae2.common.enums.PatternMultiToolInventories;
import co.neeve.nae2.common.items.patternmultitool.ObjPatternMultiTool;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tj.patternhatch.metatile.MetaTileEntityPatternHatch;

/**
 * NAE2 多功能样板工具：绑定样板仓时默认显示"工具库存"视图——
 * 左边直接列出工具里存的样板，方便快速把样板放入仓室；
 * 想查看仓室 36 槽时点左上切换按钮（INV_SWITCH）即可。
 */
@Mixin(value = ContainerPatternMultiTool.class)
public abstract class MixinContainerPatternMultiTool {

    @Shadow
    private IInterfaceHost iface;

    @Shadow
    public PatternMultiToolInventories viewingInventory;

    @Shadow
    private void addSlots() {
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void patternhatch$defaultToToolPatterns(InventoryPlayer ip, ObjPatternMultiTool te, CallbackInfo ci) {
        try {
            if (this.iface instanceof MetaTileEntityPatternHatch
                    && this.viewingInventory == PatternMultiToolInventories.INTERFACE) {
                this.viewingInventory = PatternMultiToolInventories.PMT;
                this.addSlots();
            }
        } catch (Exception ignored) {
        }
    }
}
