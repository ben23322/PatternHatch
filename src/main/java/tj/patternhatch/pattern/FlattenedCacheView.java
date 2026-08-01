package tj.patternhatch.pattern;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * 把槽缓存摊平成"每叠 ≤ 64"的多槽视图，模拟普通输入总线的形态。
 * Gregicality 并行机对单槽超大堆叠（如 9000 一叠）的并行比例/消耗处理异常，
 * 而普通总线（64 一叠分散多槽）并行正常；用该视图让模式驱动走与总线一致的路径。
 */
public class FlattenedCacheView implements IItemHandlerModifiable {

    private final IItemHandlerModifiable source;

    public FlattenedCacheView(IItemHandlerModifiable source) {
        this.source = source;
    }

    @Override
    public int getSlots() {
        int slots = 0;
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack s = source.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            slots += (s.getCount() + 63) / 64;
        }
        return Math.max(slots, 1);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int[] pos = locate(slot);
        if (pos == null) {
            return ItemStack.EMPTY;
        }
        ItemStack s = source.getStackInSlot(pos[0]);
        ItemStack out = s.copy();
        int start = pos[1] * 64;
        int take = Math.min(64, s.getCount() - start);
        if (take <= 0) {
            return ItemStack.EMPTY;
        }
        out.setCount(take);
        return out;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int[] pos = locate(slot);
        if (pos == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack s = source.getStackInSlot(pos[0]);
        if (s.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int start = pos[1] * 64;
        int availableInSlice = Math.min(64, s.getCount() - start);
        if (availableInSlice <= 0) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, availableInSlice);
        ItemStack out = s.copy();
        out.setCount(take);
        if (!simulate) {
            source.extractItem(pos[0], take, false);
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int[] pos = locate(slot);
        if (pos != null) {
            source.setStackInSlot(pos[0], stack);
        }
    }

    private int[] locate(int slot) {
        int index = slot;
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack s = source.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            int slices = (s.getCount() + 63) / 64;
            if (index < slices) {
                return new int[]{i, index};
            }
            index -= slices;
        }
        return null;
    }
}
