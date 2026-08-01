package tj.patternhatch.mixin;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.helpers.IInterfaceHost;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 给 MetaTileEntityHolder 附加 ICraftingProvider：AE 网格按机器类发现样板提供者，
 * 机器节点是 Holder，所以必须让 Holder 实现该接口并委托给仓室 MTE。
 *
 * 注意：这里只能实现 ICraftingProvider，绝不能实现 IInterfaceHost。
 * AE2 的 DualityInterface.isBusy()（阻塞模式）会遍历相邻方块，对相邻的
 * IInterfaceHost 直接调用 getInterfaceDuality()；普通 GT 机器没有 AE proxy
 * （基础 MetaTileEntity.getProxy() 返回 null），会导致返回 null 并触发
 * 服务端 tick 循环空指针崩溃（DualityInterface.isBusy:1228）。
 */
@Mixin(MetaTileEntityHolder.class)
public abstract class MixinMetaTileEntityHolder implements ICraftingProvider {

    private MetaTileEntity patternhatch$getMetaTileEntity() {
        return ((MetaTileEntityHolder) (Object) this).getMetaTileEntity();
    }

    /** 仓室 MTE 本身已实现 IInterfaceHost，这里只做委托。 */
    private IInterfaceHost patternhatch$getHost() {
        MetaTileEntity mte = patternhatch$getMetaTileEntity();
        if (mte instanceof IInterfaceHost) {
            return (IInterfaceHost) mte;
        }
        return null;
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        IInterfaceHost host = patternhatch$getHost();
        if (host != null) {
            host.provideCrafting(craftingTracker);
        }
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting craftingInv) {
        IInterfaceHost host = patternhatch$getHost();
        return host != null && host.pushPattern(patternDetails, craftingInv);
    }

    @Override
    public boolean isBusy() {
        IInterfaceHost host = patternhatch$getHost();
        return host != null && host.isBusy();
    }
}
