package tj.patternhatch.pattern;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 把槽缓存摊平成"每叠 ≤ 64"的多槽视图，模拟普通输入总线形态。
 * 切片映射在构造时按源库存快照固定一次：抽取过程中源数量变化不会导致
 * 切片位置移位，避免"一次抽取只抽走一部分、剩余被下一批重复使用"的超产 bug。
 */
public class FlattenedCacheView implements IItemHandlerModifiable {

    private final IItemHandlerModifiable source;
    private final int[] sliceSourceSlot;
    private final int[] sliceOffset;
    private final int sliceCount;

    public FlattenedCacheView(IItemHandlerModifiable source) {
        this.source = source;
        List<Integer> slots = new ArrayList<>();
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack s = source.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            int count = s.getCount();
            for (int off = 0; off < count; off += 64) {
                slots.add(i);
                offsets.add(off);
            }
        }
        this.sliceCount = Math.max(1, slots.size());
        this.sliceSourceSlot = new int[sliceCount];
        this.sliceOffset = new int[sliceCount];
        for (int i = 0; i < slots.size(); i++) {
            sliceSourceSlot[i] = slots.get(i);
            sliceOffset[i] = offsets.get(i);
        }
    }

    @Override
    public int getSlots() {
        return sliceCount;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot >= sliceCount) {
            return ItemStack.EMPTY;
        }
        ItemStack s = source.getStackInSlot(sliceSourceSlot[slot]);
        if (s.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int avail = s.getCount() - sliceOffset[slot];
        if (avail <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = s.copy();
        out.setCount(Math.min(64, avail));
        return out;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot >= sliceCount || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack s = source.getStackInSlot(sliceSourceSlot[slot]);
        if (s.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int avail = s.getCount() - sliceOffset[slot];
        if (avail <= 0) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, Math.min(64, avail));
        ItemStack out = s.copy();
        out.setCount(take);
        if (!simulate) {
            source.extractItem(sliceSourceSlot[slot], take, false);
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < sliceCount) {
            source.setStackInSlot(sliceSourceSlot[slot], stack);
        }
    }
}
