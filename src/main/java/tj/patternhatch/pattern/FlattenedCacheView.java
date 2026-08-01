package tj.patternhatch.pattern;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 把槽缓存摊平成"每叠 ≤ 64"的多槽视图，模拟普通输入总线形态。
 * 每个切片在构造时按源库存快照记账"剩余可抽取量"（sliceRemaining）：
 * 模拟抽取与真实抽取都按该记账扣减，与源库存的实时数量解耦。
 * 这样并行大批量（跨多个切片）抽取时，真实抽取量 === 模拟校验量，
 * 不会出现"校验 160 通过、实际只抽走 96"导致的漏扣 -> 缓存残余 -> 增产。
 */
public class FlattenedCacheView implements IItemHandlerModifiable {

    private final IItemHandlerModifiable source;
    private final int[] sliceSourceSlot;
    private final int[] sliceRemaining;
    private final int sliceCount;

    public FlattenedCacheView(IItemHandlerModifiable source) {
        this.source = source;
        List<Integer> slots = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack s = source.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            int count = s.getCount();
            int off = 0;
            while (off < count) {
                slots.add(i);
                remaining.add(Math.min(64, count - off));
                off += 64;
            }
        }
        this.sliceCount = Math.max(1, slots.size());
        this.sliceSourceSlot = new int[sliceCount];
        this.sliceRemaining = new int[sliceCount];
        for (int i = 0; i < slots.size(); i++) {
            sliceSourceSlot[i] = slots.get(i);
            sliceRemaining[i] = remaining.get(i);
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
        int rem = sliceRemaining[slot];
        if (rem <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack s = source.getStackInSlot(sliceSourceSlot[slot]);
        if (s.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = s.copy();
        out.setCount(Math.min(rem, Math.min(64, s.getCount())));
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
        int rem = sliceRemaining[slot];
        if (rem <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack s = source.getStackInSlot(sliceSourceSlot[slot]);
        if (s.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, Math.min(rem, Math.min(64, s.getCount())));
        ItemStack out = s.copy();
        out.setCount(take);
        if (!simulate) {
            sliceRemaining[slot] = rem - take;
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
