package tj.patternhatch.gui;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.client.gui.Gui;

/** Simple filled rectangle widget (background panels and borders). */
public class FilledRectWidget extends Widget {

    private final int color;

    public FilledRectWidget(int x, int y, int width, int height, int color) {
        super(new Position(x, y), new Size(width, height));
        this.color = color;
    }

    @Override
    public void drawInBackground(int mouseX, int mouseY, IRenderContext renderContext) {
        Position pos = getPosition();
        Size size = getSize();
        Gui.drawRect(pos.x, pos.y, pos.x + size.width, pos.y + size.height, color);
    }
}

