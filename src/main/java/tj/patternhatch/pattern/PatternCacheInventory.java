package tj.patternhatch.pattern;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** 单个样板槽的物品隔离缓存：单格堆叠上限为 1.12.2 上限（int max）。 */
public class PatternCacheInventory extends ItemStackHandler {

    public static final int DEFAULT_SLOTS = 9;

    public PatternCacheInventory() {
        this(DEFAULT_SLOTS);
    }

    public PatternCacheInventory(int size) {
        super(size);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    /** 强制把整叠放入（同物品合并、不触发常规堆叠限制），供 AE pushPattern 使用。 */
    public void forceInsert(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        for (int i = 0; i < getSlots(); i++) {
            ItemStack existing = getStackInSlot(i);
            if (existing.isEmpty()) {
                setStackInSlot(i, copy);
                return;
            }
            if (ItemStack.areItemsEqual(existing, copy) && ItemStack.areItemStackTagsEqual(existing, copy)) {
                long total = (long) existing.getCount() + copy.getCount();
                existing.setCount((int) Math.min(total, Integer.MAX_VALUE));
                onContentsChanged(i);
                return;
            }
        }
        // 缓存已满：M2 决策（丢弃/回流），先打印日志标记
        System.out.println("[PatternHatch] item cache full, dropped " + copy);
    }
}

