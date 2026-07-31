package tj.patternhatch.gui;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server-synced text widget with mouse-wheel scrolling (for cache summaries). */
public class SyncedTextWidget extends Widget {

    private static final int LINE_HEIGHT = 9;

    private final Supplier<String> textSupplier;
    private final int visibleLines;
    private String text = "";
    private int scrollOffset = 0;

    public SyncedTextWidget(int x, int y, int width, Supplier<String> textSupplier) {
        this(x, y, width, 3, textSupplier);
    }

    public SyncedTextWidget(int x, int y, int width, int visibleLines, Supplier<String> textSupplier) {
        super(new Position(x, y), new Size(width, visibleLines * LINE_HEIGHT));
        this.textSupplier = textSupplier;
        this.visibleLines = visibleLines;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        String newText = textSupplier.get();
        if (!newText.equals(text)) {
            text = newText;
            writeUpdateInfo(1, buffer -> buffer.writeString(text));
        }
    }

    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 1) {
            text = buffer.readString(Short.MAX_VALUE);
            scrollOffset = 0;
        }
    }

    @Override
    public boolean mouseWheelMove(int mouseX, int mouseY, int delta) {
        Position pos = getPosition();
        boolean over = isMouseOverElement(mouseX, mouseY)
                || isMouseOverElement(mouseX + pos.x, mouseY + pos.y)
                || isMouseOverElement(mouseX - pos.x, mouseY - pos.y);
        if (!over) {
            return false;
        }
        int lines = wrap(text).size();
        int maxOffset = Math.max(0, lines - visibleLines);
        if (maxOffset == 0) {
            return false;
        }
        int step = delta > 0 ? -1 : 1;
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + step));
        return true;
    }

    @Override
    public void drawInBackground(int mouseX, int mouseY, IRenderContext renderContext) {
        if (text.isEmpty()) {
            return;
        }
        List<String> lines = wrap(text);
        int start = Math.max(0, Math.min(scrollOffset, lines.size() - visibleLines));
        Position pos = getPosition();
        for (int i = 0; i < visibleLines && start + i < lines.size(); i++) {
            String line = lines.get(start + i);
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(line, pos.x, pos.y + i * LINE_HEIGHT, 0xFFFFFFFF);
        }
        // Scroll bar (only when content overflows)
        int linesTotal = lines.size();
        int maxOffset = Math.max(0, linesTotal - visibleLines);
        if (maxOffset > 0) {
            int barX = pos.x + getSize().width - 3;
            int barTop = pos.y + 1;
            int barHeight = visibleLines * LINE_HEIGHT - 2;
            net.minecraft.client.gui.Gui.drawRect(barX, barTop, barX + 2, barTop + barHeight, 0xAA555555);
            int thumbH = Math.max(6, barHeight * visibleLines / linesTotal);
            int thumbY = barTop + (barHeight - thumbH) * scrollOffset / maxOffset;
            net.minecraft.client.gui.Gui.drawRect(barX, thumbY, barX + 2, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    /** Wrap each entry into visual lines that fit the widget width (full names, no truncation). */
    private List<String> wrap(String text) {
        List<String> visual = new ArrayList<>();
        if (text.isEmpty()) {
            return visual;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int maxWidth = getSize().width - 6;
        for (String entry : text.split("\n")) {
            if (entry.isEmpty()) {
                visual.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < entry.length(); i++) {
                char c = entry.charAt(i);
                if (line.length() > 0 && font.getStringWidth(line.toString() + c) > maxWidth) {
                    visual.add(line.toString());
                    line.setLength(0);
                }
                line.append(c);
            }
            visual.add(line.toString());
        }
        return visual;
    }
}
