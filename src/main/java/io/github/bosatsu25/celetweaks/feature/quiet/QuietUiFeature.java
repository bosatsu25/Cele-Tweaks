package io.github.bosatsu25.celetweaks.feature.quiet;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.runtime.RuntimeComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Set;

/**
 * Quiet UI shows every team in one neutral grey. Quiet Chat converts each chat colour to the grey of
 * the same brightness, so emphasis survives while hue noise disappears; text, click and hover events
 * are untouched.
 */
public final class QuietUiFeature implements RuntimeComponent {
    @Override
    public String id() { return "quiet"; }

    @Override
    public Set<FeatureDefinition> definitions() {
        return Set.of(FeatureDefinition.QUIET_UI, FeatureDefinition.QUIET_CHAT);
    }

    public static boolean neutralTeams() {
        return CeleConfig.get().isEnabled(FeatureDefinition.QUIET_UI);
    }

    public static boolean quietChat() {
        return CeleConfig.get().isEnabled(FeatureDefinition.QUIET_CHAT);
    }

    /** Copies the component tree, replacing every colour with its luminance-equivalent grey. */
    public static Component toGreyscale(Component source) {
        if (source == null) return null;
        Style style = source.getStyle();
        TextColor color = style.getColor();
        if (color != null) style = style.withColor(TextColor.fromRgb(grey(color.getValue())));
        MutableComponent out = MutableComponent.create(source.getContents()).setStyle(style);
        for (Component child : source.getSiblings()) out.append(toGreyscale(child));
        return out;
    }

    static int grey(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int y = (r * 299 + g * 587 + b * 114) / 1000;
        return (y << 16) | (y << 8) | y;
    }
}
