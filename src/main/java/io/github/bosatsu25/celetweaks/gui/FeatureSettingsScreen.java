package io.github.bosatsu25.celetweaks.gui;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.runtime.FeatureManager;
import io.github.bosatsu25.celetweaks.runtime.FeatureSections;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2 of 3: one feature's settings, generated from the public fields of its config section.
 * Supported field types: boolean, int, double/float, String, enum, {@code List<String>}.
 */
public final class FeatureSettingsScreen extends Screen {
    private final Screen parent;
    private final FeatureDefinition definition;
    private final Object section;
    private int page;
    private Component status = Component.empty();

    public FeatureSettingsScreen(Screen parent, FeatureDefinition definition) {
        super(Component.translatable(definition.nameKey()));
        this.parent = parent;
        this.definition = definition;
        Class<?> type = FeatureSections.sectionType(definition);
        this.section = type == null ? null : CeleConfig.get().section(definition, type);
    }

    @Override
    protected void init() {
        int left = Layout.contentLeft(width);
        int contentWidth = Layout.contentWidth(width);
        int labelWidth = contentWidth / 2;
        int widgetWidth = contentWidth - labelWidth - 4;

        List<Field> fields = fields();
        int perPage = Layout.rowsPerPage(height);
        int pages = Math.max(1, (fields.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int y = Layout.HEADER;
        for (int i = page * perPage; i < Math.min(fields.size(), (page + 1) * perPage); i++) {
            Field field = fields.get(i);
            addWidgetFor(field, left + labelWidth + 4, y, widgetWidth);
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

    private List<Field> fields() {
        List<Field> result = new ArrayList<>();
        if (section == null) return result;
        for (Field field : section.getClass().getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || field.isSynthetic()) continue;
            result.add(field);
        }
        return result;
    }

    private void addWidgetFor(Field field, int x, int y, int widgetWidth) {
        Class<?> type = field.getType();
        try {
            if (type == boolean.class) {
                boolean value = field.getBoolean(section);
                Button button = Button.builder(boolLabel(value), b -> {
                    try {
                        boolean next = !field.getBoolean(section);
                        field.setBoolean(section, next);
                        b.setMessage(boolLabel(next));
                        changed();
                    } catch (IllegalAccessException failure) {
                        throw new IllegalStateException(failure);
                    }
                }).bounds(x, y, widgetWidth, Layout.WIDGET_HEIGHT).build();
                addRenderableWidget(button);
            } else if (type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                Button button = Button.builder(enumLabel(field.get(section)), b -> {
                    try {
                        Object current = field.get(section);
                        int index = 0;
                        for (int i = 0; i < constants.length; i++) if (constants[i] == current) index = i;
                        Object next = constants[(index + 1) % constants.length];
                        field.set(section, next);
                        b.setMessage(enumLabel(next));
                        changed();
                    } catch (IllegalAccessException failure) {
                        throw new IllegalStateException(failure);
                    }
                }).bounds(x, y, widgetWidth, Layout.WIDGET_HEIGHT).build();
                addRenderableWidget(button);
            } else if (type == int.class || type == double.class || type == float.class || type == String.class) {
                EditBox box = new EditBox(font, x, y, widgetWidth, Layout.WIDGET_HEIGHT, Component.literal(field.getName()));
                box.setMaxLength(256);
                box.setValue(String.valueOf(field.get(section)));
                Range range = field.getAnnotation(Range.class);
                box.setResponder(text -> applyText(field, type, range, text, box));
                addRenderableWidget(box);
            } else if (List.class.isAssignableFrom(type) && isStringList(field)) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) field.get(section);
                if (list == null) {
                    list = new ArrayList<>();
                    field.set(section, list);
                }
                List<String> target = list;
                addRenderableWidget(Button.builder(Component.translatable("celetweaks.screen.edit_list", target.size()),
                        b -> CeleScreens.open(new ListEditScreen(this, Component.literal(prettyName(field)), target, this::changed)))
                        .bounds(x, y, widgetWidth, Layout.WIDGET_HEIGHT).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal(String.valueOf(field.get(section))), b -> {})
                        .bounds(x, y, widgetWidth, Layout.WIDGET_HEIGHT).build()).active = false;
            }
        } catch (IllegalAccessException failure) {
            CeleTweaksClient.LOGGER.warn("Cannot expose config field {}", field.getName(), failure);
        }
    }

    private void applyText(Field field, Class<?> type, Range range, String text, EditBox box) {
        try {
            if (type == String.class) {
                field.set(section, text);
            } else {
                double parsed = Double.parseDouble(text.trim());
                if (range != null) parsed = Math.max(range.min(), Math.min(range.max(), parsed));
                if (type == int.class) field.setInt(section, (int) Math.round(parsed));
                else if (type == float.class) field.setFloat(section, (float) parsed);
                else field.setDouble(section, parsed);
            }
            box.setTextColor(0xFFE0E0E0);
            changed();
        } catch (NumberFormatException invalid) {
            box.setTextColor(0xFFFF5555);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private void changed() {
        CeleConfig.get().save();
        FeatureManager.get().notifyToggle(definition, CeleConfig.get().isEnabled(definition));
        status = Component.translatable("celetweaks.screen.saved").withStyle(ChatFormatting.GRAY);
    }

    private static boolean isStringList(Field field) {
        return field.getGenericType() instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments().length == 1
                && parameterized.getActualTypeArguments()[0] == String.class;
    }

    private static Component boolLabel(boolean value) {
        return value ? Component.translatable("celetweaks.on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("celetweaks.off").withStyle(ChatFormatting.RED);
    }

    private static Component enumLabel(Object value) {
        return Component.literal(String.valueOf(value));
    }

    static String prettyName(Field field) {
        StringBuilder out = new StringBuilder();
        for (char c : field.getName().toCharArray()) {
            if (Character.isUpperCase(c)) out.append(' ');
            out.append(out.isEmpty() ? Character.toUpperCase(c) : c);
        }
        return out.toString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Gfx.centeredText(graphics, font, title, width / 2, 10, 0xFFFFFFFF);
        Gfx.centeredText(graphics, font, Component.translatable(definition.descriptionKey()), width / 2, 24, 0xFFA0A0A0);
        if (section == null) {
            Gfx.centeredText(graphics, font, Component.translatable("celetweaks.screen.no_settings"), width / 2, height / 2, 0xFFA0A0A0);
        }
        int left = Layout.contentLeft(width);
        List<Field> fields = fields();
        int perPage = Layout.rowsPerPage(height);
        int y = Layout.HEADER + 6;
        for (int i = page * perPage; i < Math.min(fields.size(), (page + 1) * perPage); i++) {
            Gfx.text(graphics, font, prettyName(fields.get(i)), left, y, 0xFFE0E0E0, false);
            y += Layout.ROW_HEIGHT;
        }
        Gfx.text(graphics, font, status, left + 56, height - Layout.FOOTER + 13, 0xFFA0A0A0, false);
    }

    @Override
    public void onClose() {
        CeleScreens.open(parent);
    }
}
