package tj.patternhatch.gui;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

/** Static label drawn with the game font and drop shadow (matches in-game text). */
public class ShadowLabelWidget extends Widget {

    private final String langKey;
    private final int color;

    public ShadowLabelWidget(int x, int y, String langKey, int color) {
        super(new Position(x, y), new Size(80, 10));
        this.langKey = langKey;
        this.color = color;
    }

    @Override
    public void drawInBackground(int mouseX, int mouseY, IRenderContext renderContext) {
        Position pos = getPosition();
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(
                I18n.format(langKey), pos.x, pos.y, color);
    }
}

