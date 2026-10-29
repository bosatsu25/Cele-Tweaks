package io.github.bosatsu25.celetweaks.feature.scoreboard;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import io.github.bosatsu25.celetweaks.runtime.FeatureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Draws the sidebar objective as an anchored, scalable, paged panel. Server values are only displayed. */
public final class ScoreboardPanelFeature extends Feature {
    public static final String NEXT_PAGE = "scoreboard_panel.next_page";
    public static final String PREVIOUS_PAGE = "scoreboard_panel.previous_page";

    /** Nine screen anchors; the panel's matching corner / edge midpoint is placed on the anchor. */
    public enum Anchor {
        TOP_LEFT(0, 0), TOP(1, 0), TOP_RIGHT(2, 0),
        LEFT(0, 1), CENTER(1, 1), RIGHT(2, 1),
        BOTTOM_LEFT(0, 2), BOTTOM(1, 2), BOTTOM_RIGHT(2, 2);

        final int column;
        final int row;

        Anchor(int column, int row) {
            this.column = column;
            this.row = row;
        }
    }

    private int page;

    public ScoreboardPanelFeature() {
        super(FeatureDefinition.SCOREBOARD_PANEL);
    }

    @Override
    public void init() {
        HotkeyManager.register(NEXT_PAGE, Component.translatable("celetweaks.hotkey.scoreboard_panel.next_page"), () -> page++);
        HotkeyManager.register(PREVIOUS_PAGE, Component.translatable("celetweaks.hotkey.scoreboard_panel.previous_page"), () -> page--);
    }

    @Override
    public void resetSession(Minecraft client) {
        page = 0;
    }

    public Config config() { return config(Config.class); }

    /** Wraps the page index into range for {@code pages} pages and returns it. */
    int wrapPage(int pages) {
        page = Math.floorMod(page, Math.max(1, pages));
        return page;
    }

    public static ScoreboardPanelFeature instance() {
        return FeatureManager.get().component(FeatureDefinition.SCOREBOARD_PANEL.id())
                .filter(ScoreboardPanelFeature.class::isInstance)
                .map(ScoreboardPanelFeature.class::cast)
                .orElse(null);
    }

    public static final class Config {
        public Anchor anchor = Anchor.RIGHT;
        /** Pixel offset from the anchor, in GUI pixels (positive = right / down). */
        @Range(min = -2000, max = 2000) public int offsetX = -2;
        @Range(min = -2000, max = 2000) public int offsetY = 0;
        @Range(min = 0.25, max = 3.0) public double scale = 1.0;
        @Range(min = 1, max = 60) public int rowsPerPage = 12;
        /** Background opacity, percent. */
        @Range(min = 0, max = 100) public int backgroundOpacity = 35;
        public boolean showTitle = true;
        public boolean showValues = true;
        public boolean showPageIndicator = true;
    }
}
