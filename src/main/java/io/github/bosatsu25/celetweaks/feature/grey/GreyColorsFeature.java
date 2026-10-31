package io.github.bosatsu25.celetweaks.feature.grey;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.runtime.RuntimeComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Set;

/**
 * Grey Team Colors shows every team in one neutral grey. Grey Chat Colors converts each chat colour to the grey of
 * the same brightness, so emphasis survives while hue noise disappears; text, click and hover events
 * are untouched.
 */
public final class GreyColorsFeature implements RuntimeComponent {
    @Override
    public String id() { return "grey_colors"; }

    @Override
    public Set<FeatureDefinition> definitions() {
        return Set.of(FeatureDefinition.GREY_TEAMS, FeatureDefinition.GREY_CHAT);
    }

    public static boolean greyTeams() {
        return CeleConfig.get().isEnabled(FeatureDefinition.GREY_TEAMS);
    }

    public static boolean greyChat() {
        return CeleConfig.get().isEnabled(FeatureDefinition.GREY_CHAT);
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
