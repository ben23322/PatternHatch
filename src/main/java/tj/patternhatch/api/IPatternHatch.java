package tj.patternhatch.api;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.IFluidTank;
import tj.patternhatch.pattern.PatternSlotEntry;

import java.util.List;

/** 样板仓部件暴露给兼容层（Mixin）的接口。 */
public interface IPatternHatch {

    List<PatternSlotEntry> getPatternSlots();

    IItemHandler getCatalystInventory();

    IItemHandler getCircuitInventory();

    /** 共享流体催化剂罐（配方中"不消耗"的流体催化剂，灌一次永久生效）。 */
    IFluidTank[] getFluidCatalystTanks();

    void markDirty();

    /** 是否装有至少一个样板（用于"完全隔离"判断：有样板就不回退到普通输入）。 */
    boolean hasPatterns();

    /** 是否有缓存残余（物品或流体，用于空闲自动弹回）。 */
    boolean hasCachedItems();

    /** 把全部样板槽缓存送回 ME 网络（放不下的留在缓存）。 */
    void returnCacheToAE();
}
