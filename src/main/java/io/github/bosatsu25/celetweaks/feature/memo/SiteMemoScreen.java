package io.github.bosatsu25.celetweaks.feature.memo;

import io.github.bosatsu25.celetweaks.gui.CeleScreens;
import io.github.bosatsu25.celetweaks.gui.Gfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** One EditBox per line, paged. Saved when the screen closes. */
public final class SiteMemoScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private final Screen parent;
    private final List<String> lines;
    private int page;

    public SiteMemoScreen(Screen parent) {
        super(Component.translatable("celetweaks.memo.title"));
        this.parent = parent;
        this.lines = SiteMemoFeature.load();
    }

    private int rowsPerPage() {
        return Math.max(1, (height - 70) / ROW_HEIGHT);
    }

    @Override
    protected void init() {
        int width = Math.min(420, this.width - 20);
        int left = (this.width - width) / 2;
        int perPage = rowsPerPage();
        int pages = (SiteMemoFeature.MAX_LINES + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));
        int y = 30;
        for (int i = page * perPage; i < Math.min(SiteMemoFeature.MAX_LINES, (page + 1) * perPage); i++) {
            int index = i;
            EditBox box = new EditBox(font, left + 24, y, width - 24, 18, Component.literal("line " + (i + 1)));
            box.setMaxLength(SiteMemoFeature.MAX_LINE_LENGTH);
            box.setValue(lines.get(i));
            box.setResponder(text -> lines.set(index, text));
            addRenderableWidget(box);
            y += ROW_HEIGHT;
        }
        int footer = this.height - 28;
        Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(left, footer, 24, 20).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(left + 26, footer, 24, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + width - 80, footer, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Gfx.centeredText(graphics, font, title, this.width / 2, 12, 0xFFFFFFFF);
        int width = Math.min(420, this.width - 20);
        int left = (this.width - width) / 2;
        int perPage = rowsPerPage();
        int y = 35;
        for (int i = page * perPage; i < Math.min(SiteMemoFeature.MAX_LINES, (page + 1) * perPage); i++) {
            Gfx.text(graphics, font, String.valueOf(i + 1), left, y, 0xFF808080, false);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void onClose() {
        SiteMemoFeature.save(lines);
        CeleScreens.open(parent);
    }
}
