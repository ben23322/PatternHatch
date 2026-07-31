package tj.patternhatch.mixin;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import com.google.common.collect.ImmutableSet;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import tj.patternhatch.metatile.MetaTileEntityPatternHatch;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * 给 MetaTileEntityHolder 附加 IInterfaceHost：AE 网格按机器类发现样板提供者，
 * 机器节点是 Holder，所以必须让 Holder 实现该接口并委托给仓室 MTE。
 */
@Mixin(MetaTileEntityHolder.class)
public abstract class MixinMetaTileEntityHolder implements IInterfaceHost {

    private DualityInterface patternhatch$emptyDuality;

    private static final IConfigManager EMPTY_CONFIG = new IConfigManager() {
        private final EnumMap<Settings, Enum<?>> values = new EnumMap<>(Settings.class);

        @Override
        public java.util.Set<Settings> getSettings() {
            return values.keySet();
        }

        @Override
        public void registerSetting(Settings setting, Enum<?> defaultValue) {
            values.put(setting, defaultValue);
        }

        @Override
        public Enum<?> getSetting(Settings setting) {
            return values.get(setting);
        }

        @Override
        public Enum<?> putSetting(Settings setting, Enum<?> value) {
            return values.put(setting, value);
        }

        @Override
        public void writeToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        }

        @Override
        public void readFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
        }
    };

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

    @Override
    public DualityInterface getInterfaceDuality() {
        IInterfaceHost host = patternhatch$getHost();
        if (host != null) {
            return host.getInterfaceDuality();
        }
        if (patternhatch$emptyDuality == null) {
            MetaTileEntity mte = patternhatch$getMetaTileEntity();
            if (mte != null && mte.getProxy() != null) {
                patternhatch$emptyDuality = new DualityInterface(mte.getProxy(), this);
            }
        }
        return patternhatch$emptyDuality;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        IInterfaceHost host = patternhatch$getHost();
        return host != null ? host.getTargets() : EnumSet.noneOf(EnumFacing.class);
    }

    @Override
    public TileEntity getTileEntity() {
        return (TileEntity) (Object) this;
    }

    @Override
    public void saveChanges() {
        IInterfaceHost host = patternhatch$getHost();
        if (host != null) {
            host.saveChanges();
        }
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrades) {
        return 0;
    }

    @Override
    public TileEntity getTile() {
        return (TileEntity) (Object) this;
    }

    @Override
    public IConfigManager getConfigManager() {
        return EMPTY_CONFIG;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        return null;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.of();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink input, IAEItemStack acceptedItems, Actionable mode) {
        return acceptedItems;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
    }

    @Override
    public IGridNode getActionableNode() {
        return ((MetaTileEntityHolder) (Object) this).getActionableNode();
    }
}
