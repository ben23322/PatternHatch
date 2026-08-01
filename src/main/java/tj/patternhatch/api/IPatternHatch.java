package tj.patternhatch.api;

import net.minecraftforge.items.IItemHandler;
import tj.patternhatch.pattern.PatternSlotEntry;

import java.util.List;

/** 样板仓部件暴露给兼容层（Mixin）的接口。 */
public interface IPatternHatch {

    List<PatternSlotEntry> getPatternSlots();

    IItemHandler getCatalystInventory();

    IItemHandler getCircuitInventory();

    void markDirty();

    /** 把全部样板槽缓存送回 ME 网络（放不下的留在缓存）。 */
    void returnCacheToAE();
}
