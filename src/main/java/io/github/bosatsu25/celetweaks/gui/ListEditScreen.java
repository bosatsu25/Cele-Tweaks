package io.github.bosatsu25.celetweaks.gui;

import io.github.bosatsu25.celetweaks.config.Ids;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Screen 3 of 3: edits a {@code List<String>} of registry ids (blocks, items, entities) or free text. */
public final class ListEditScreen extends Screen {
    private final Screen parent;
    private final List<String> target;
    private final Runnable onChange;
    private int page;
    private EditBox input;

    public ListEditScreen(Screen parent, Component title, List<String> target, Runnable onChange) {
        super(title);
        this.parent = parent;
        this.target = target;
        this.onChange = onChange;
    }

    @Override
    protected void init() {
        int left = Layout.contentLeft(width);
        int contentWidth = Layout.contentWidth(width);

        input = new EditBox(font, left, 26, contentWidth - 64, Layout.WIDGET_HEIGHT, Component.translatable("celetweaks.screen.list_input"));
        input.setMaxLength(256);
        addRenderableWidget(input);
        addRenderableWidget(Button.builder(Component.translatable("celetweaks.screen.add"), b -> add())
                .bounds(left + contentWidth - 60, 26, 60, Layout.WIDGET_HEIGHT).build());

        int perPage = Layout.rowsPerPage(height);
        int pages = Math.max(1, (target.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int y = Layout.HEADER;
        for (int i = page * perPage; i < Math.min(target.size(), (page + 1) * perPage); i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                target.remove(index);
                onChange.run();
                rebuildWidgets();
            }).bounds(left + contentWidth - 24, y, 24, Layout.WIDGET_HEIGHT).build());
            y += Layout.ROW_HEIGHT;
        }

        int footerY = height - Layout.FOOTER + 7;
        Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(left, footerY, 24, Layout.WIDGET_HEIGHT).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(left + 26, footerY, 24, Layout.WIDGET_HEIGHT).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + contentWidth - 80, footerY, 80, Layout.WIDGET_HEIGHT).build());
    }

    private void add() {
        String raw = input.getValue();
        String id = Ids.normalize(raw);
        String entry = id != null ? id : raw.trim();
        if (entry.isEmpty() || target.contains(entry)) return;
        target.add(entry);
        input.setValue("");
        onChange.run();
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Gfx.centeredText(graphics, font, title, width / 2, 10, 0xFFFFFFFF);
        int left = Layout.contentLeft(width);
        int perPage = Layout.rowsPerPage(height);
        int y = Layout.HEADER + 6;
        for (int i = page * perPage; i < Math.min(target.size(), (page + 1) * perPage); i++) {
            Gfx.text(graphics, font, target.get(i), left, y, 0xFFE0E0E0, false);
            y += Layout.ROW_HEIGHT;
        }
    }

    @Override
    public void onClose() {
        CeleScreens.open(parent);
    }
}
