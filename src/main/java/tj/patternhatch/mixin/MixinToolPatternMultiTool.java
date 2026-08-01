package tj.patternhatch.mixin;

import appeng.api.util.AEPartLocation;
import co.neeve.nae2.NAE2;
import co.neeve.nae2.common.items.patternmultitool.ObjPatternMultiTool;
import co.neeve.nae2.common.items.patternmultitool.ToolPatternMultiTool;
import co.neeve.nae2.common.sync.GuiBridge;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.patternhatch.metatile.MetaTileEntityPatternHatch;

/**
 * NAE2 多功能样板工具（Pattern Multi-Tool）兼容：
 * 用工具右键样板仓时，像打开 AE 接口一样打开工具界面，右侧展示样板仓的
 * 36 个样板槽（读写直接作用于样板仓，放/取样板会即时刷新 AE 配方）。
 *
 * 只通过可选 mixin 配置加载（mixins.patternhatch.nae2.json，required=false），
 * 没有装 NAE2 时完全不影响游戏。
 */
@Mixin(value = ToolPatternMultiTool.class)
public abstract class MixinToolPatternMultiTool {

    private static MetaTileEntityPatternHatch patternhatch$getHatch(TileEntity te) {
        if (te instanceof MetaTileEntityHolder) {
            MetaTileEntity mte = ((MetaTileEntityHolder) te).getMetaTileEntity();
            if (mte instanceof MetaTileEntityPatternHatch) {
                return (MetaTileEntityPatternHatch) mte;
            }
        }
        return null;
    }

    @Inject(method = "onItemUseFirst", at = @At("HEAD"), cancellable = true)
    private void patternhatch$onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                             float hitX, float hitY, float hitZ, EnumHand hand,
                                             CallbackInfoReturnable<EnumActionResult> cir) {
        if (player.isSneaking() || world == null || world.isRemote) {
            return;
        }
        MetaTileEntityPatternHatch hatch = patternhatch$getHatch(world.getTileEntity(pos));
        if (hatch == null) {
            return;
        }
        // 直接打开 NAE2 工具界面（绕过 hasPermissions 对未接入网络的 IActionHost 的空指针风险）。
        int guiId = GuiBridge.PATTERN_MULTI_TOOL.ordinal() << 4 | AEPartLocation.INTERNAL.ordinal() | (1 << 3);
        player.openGui(NAE2.instance, guiId, world, pos.getX(), pos.getY(), pos.getZ());
        player.swingArm(hand);
        cir.setReturnValue(EnumActionResult.SUCCESS);
    }

    @Inject(method = "getGuiObject(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lappeng/api/util/AEPartLocation;)"
            + "Lco/neeve/nae2/common/items/patternmultitool/ObjPatternMultiTool;",
            at = @At("HEAD"), cancellable = true)
    private void patternhatch$getGuiObject(ItemStack is, World w, BlockPos bp, AEPartLocation side,
                                           CallbackInfoReturnable<ObjPatternMultiTool> cir) {
        if (w == null || bp == null) {
            return;
        }
        MetaTileEntityPatternHatch hatch = patternhatch$getHatch(w.getTileEntity(bp));
        if (hatch == null) {
            return;
        }
        ObjPatternMultiTool obj = new ObjPatternMultiTool(is);
        obj.setInterface(hatch);
        cir.setReturnValue(obj);
    }
}
