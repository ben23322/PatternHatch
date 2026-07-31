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
}

