package tj.patternhatch.gui;

import appeng.items.misc.ItemEncodedPattern;
import gregtech.api.gui.widgets.SlotWidget;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** 样板槽：只允许放入 AE2 已编码的样板（空槽可放）。 */
public class PatternSlotWidget extends SlotWidget {

    public PatternSlotWidget(IItemHandler itemHandler, int slotIndex, int xPosition, int yPosition) {
        super(itemHandler, slotIndex, xPosition, yPosition, true, true);
    }

    @Override
    public boolean canPutStack(ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof ItemEncodedPattern;
    }
}

