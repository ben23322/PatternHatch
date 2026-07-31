package tj.patternhatch.pattern;

import net.minecraft.nbt.NBTTagCompound;

/** 一个样板槽 = 槽位索引 + 物品隔离缓存 + 流体隔离缓存（样板本体存于 patternInventory）。 */
public class PatternSlotEntry {

    private final int slotIndex;
    private final PatternCacheInventory itemCache;
    private final PatternCacheFluidHandler fluidCache;

    public PatternSlotEntry(int slotIndex) {
        this(slotIndex, new PatternCacheInventory(), new PatternCacheFluidHandler());
    }

    public PatternSlotEntry(int slotIndex, PatternCacheInventory itemCache, PatternCacheFluidHandler fluidCache) {
        this.slotIndex = slotIndex;
        this.itemCache = itemCache;
        this.fluidCache = fluidCache;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public PatternCacheInventory getItemCache() {
        return itemCache;
    }

    public PatternCacheFluidHandler getFluidCache() {
        return fluidCache;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Slot", slotIndex);
        tag.setTag("ItemCache", itemCache.serializeNBT());
        tag.setTag("FluidCache", fluidCache.writeToNBT());
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        itemCache.deserializeNBT(tag.getCompoundTag("ItemCache"));
        fluidCache.readFromNBT(tag.getCompoundTag("FluidCache"));
    }
}

