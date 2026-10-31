package io.github.bosatsu25.celetweaks.gui;

import io.github.bosatsu25.celetweaks.catalog.FeatureCategory;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.runtime.FeatureManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Screen 1 of 3: category tabs × feature rows (toggle / hotkey / details). */
public final class FeatureListScreen extends Screen {
    private final Screen parent;
    private FeatureCategory category = FeatureCategory.GUARD;
    private int page;
    private final List<Component> hoverLines = new ArrayList<>();

    public FeatureListScreen(Screen parent) {
        super(Component.translatable("celetweaks.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        hoverLines.clear();
        int left = Layout.contentLeft(width);
        int contentWidth = Layout.contentWidth(width);

        // Category tabs
        FeatureCategory[] categories = FeatureCategory.values();
        int tabWidth = contentWidth / categories.length;
        for (int i = 0; i < categories.length; i++) {
            FeatureCategory tab = categories[i];
            Button button = Button.builder(Component.translatable(tab.nameKey()), b -> {
                category = tab;
                page = 0;
                rebuildWidgets();
            }).bounds(left + i * tabWidth, 26, tabWidth - 2, Layout.WIDGET_HEIGHT).build();
            button.active = tab != category;
            addRenderableWidget(button);
        }

        // Rows
        List<FeatureDefinition> rows = rowsForCategory();
        int perPage = Layout.rowsPerPage(height);
        int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int y = Layout.HEADER;
        int toggleWidth = 44;
        int hotkeyWidth = 90;
        int detailWidth = 24;
        int nameWidth = contentWidth - toggleWidth - hotkeyWidth - detailWidth - 6;
        for (int i = page * perPage; i < Math.min(rows.size(), (page + 1) * perPage); i++) {
            FeatureDefinition definition = rows.get(i);
            int x = left;
            boolean available = definition.isAvailable();
            boolean quarantined = FeatureManager.get().isQuarantined(definition);

            Button name = Button.builder(rowName(definition, available, quarantined),
                    b -> CeleScreens.open(new FeatureSettingsScreen(this, definition)))
                    .bounds(x, y, nameWidth, Layout.WIDGET_HEIGHT).build();
            name.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip(definition, available, quarantined)));
            addRenderableWidget(name);
            x += nameWidth + 2;

            boolean on = CeleConfig.get().isSwitchedOn(definition);
            Button toggle = Button.builder(toggleLabel(on), b -> {
                CeleConfig.get().toggle(definition);
                b.setMessage(toggleLabel(CeleConfig.get().isSwitchedOn(definition)));
            }).bounds(x, y, toggleWidth, Layout.WIDGET_HEIGHT).build();
            toggle.active = available;
            addRenderableWidget(toggle);
            x += toggleWidth + 2;

            String actionId = HotkeyManager.toggleId(definition);
            Button hotkey = Button.builder(HotkeyManager.binding(actionId).displayName(),
                    b -> CeleScreens.open(new HotkeyCaptureScreen(this, actionId)))
                    .bounds(x, y, hotkeyWidth, Layout.WIDGET_HEIGHT).build();
            hotkey.active = available;
            addRenderableWidget(hotkey);
            x += hotkeyWidth + 2;

            Button detail = Button.builder(Component.literal("…"),
                    b -> CeleScreens.open(new FeatureSettingsScreen(this, definition)))
                    .bounds(x, y, detailWidth, Layout.WIDGET_HEIGHT).build();
            addRenderableWidget(detail);
            y += Layout.ROW_HEIGHT;
        }

        // Footer
        int footerY = height - Layout.FOOTER + 7;
        Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(left, footerY, 24, Layout.WIDGET_HEIGHT).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(left + 26, footerY, 24, Layout.WIDGET_HEIGHT).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.translatable("celetweaks.screen.open_settings_key"),
                b -> CeleScreens.open(new HotkeyCaptureScreen(this, HotkeyManager.OPEN_SETTINGS)))
                .bounds(left + 56, footerY, 120, Layout.WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + contentWidth - 80, footerY, 80, Layout.WIDGET_HEIGHT).build());
        hoverLines.add(Component.translatable("celetweaks.screen.page", page + 1, pages));
    }

    private List<FeatureDefinition> rowsForCategory() {
        List<FeatureDefinition> rows = new ArrayList<>();
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            if (definition.category() == category) rows.add(definition);
        }
        return rows;
    }

    private static Component rowName(FeatureDefinition definition, boolean available, boolean quarantined) {
        Component name = Component.translatable(definition.nameKey());
        if (quarantined) return name.copy().withStyle(ChatFormatting.RED);
        if (!available) return name.copy().withStyle(ChatFormatting.DARK_GRAY);
        return name;
    }

    private static Component tooltip(FeatureDefinition definition, boolean available, boolean quarantined) {
        var text = Component.translatable(definition.descriptionKey()).copy();
        if (quarantined) {
            text.append("\n").append(Component.translatable("celetweaks.screen.quarantined").withStyle(ChatFormatting.RED));
        } else if (!definition.implemented()) {
            text.append("\n").append(Component.translatable("celetweaks.screen.not_implemented").withStyle(ChatFormatting.GRAY));
        } else if (!available) {
            text.append("\n").append(Component.translatable("celetweaks.screen.requires",
                    String.join(", ", definition.requiredMods())).withStyle(ChatFormatting.GRAY));
        }
        text.append("\n").append(Component.translatable("celetweaks.risk." + definition.risk().name().toLowerCase())
                .withStyle(ChatFormatting.DARK_AQUA));
        return text;
    }

    private static Component toggleLabel(boolean on) {
        return on ? Component.translatable("celetweaks.on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("celetweaks.off").withStyle(ChatFormatting.RED);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Gfx.centeredText(graphics, font, title, width / 2, 10, 0xFFFFFFFF);
        int left = Layout.contentLeft(width);
        for (Component line : hoverLines) {
            Gfx.text(graphics, font, line, left + 190, height - Layout.FOOTER + 13, 0xFFA0A0A0, false);
        }
    }

    @Override
    public void onClose() {
        CeleScreens.open(parent);
    }
}
