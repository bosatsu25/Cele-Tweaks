package io.github.bosatsu25.celetweaks.feature.sidebar;

import io.github.bosatsu25.celetweaks.gui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lays out one page of the sidebar objective and draws it at the configured anchor. */
public final class SidebarLayoutRenderer {
    private static final int PAD = 3;

    private SidebarLayoutRenderer() {}

    private record Row(Component name, Component value) {}

    public static void render(GuiGraphicsExtractor graphics, Objective objective, SidebarLayoutFeature feature) {
        SidebarLayoutFeature.Config cfg = feature.config();
        Font font = Minecraft.getInstance().font;
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);

        List<PlayerScoreEntry> visible = new ArrayList<>();
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (!entry.isHidden()) visible.add(entry);
        }
        visible.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner));

        int perPage = Math.max(1, cfg.rowsPerPage);
        int pages = Math.max(1, (visible.size() + perPage - 1) / perPage);
        int page = feature.wrapPage(pages);
        List<Row> rows = new ArrayList<>();
        for (int i = page * perPage; i < Math.min(visible.size(), (page + 1) * perPage); i++) {
            PlayerScoreEntry entry = visible.get(i);
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            rows.add(new Row(PlayerTeam.formatNameForTeam(team, entry.ownerName()), entry.formatValue(format)));
        }

        Component title = objective.getDisplayName();
        Component footer = pages > 1 && cfg.showPageIndicator ? Component.literal((page + 1) + "/" + pages) : null;
        int width = cfg.showTitle ? font.width(title) : 0;
        for (Row row : rows) {
            width = Math.max(width, font.width(row.name()) + (cfg.showValues ? 8 + font.width(row.value()) : 0));
        }
        if (footer != null) width = Math.max(width, font.width(footer));
        int line = font.lineHeight + 1;
        int height = rows.size() * line + (cfg.showTitle ? line + 2 : 0) + (footer != null ? line : 0);
        int panelW = width + PAD * 2;
        int panelH = height + PAD * 2;

        float scale = (float) Math.max(0.25, Math.min(3.0, cfg.scale));
        int scaledW = Math.round(panelW * scale);
        int scaledH = Math.round(panelH * scale);
        SidebarLayoutFeature.Anchor anchor = cfg.anchor == null ? SidebarLayoutFeature.Anchor.RIGHT : cfg.anchor;
        int x = graphics.guiWidth() * anchor.column / 2 - scaledW * anchor.column / 2 + cfg.offsetX;
        int y = graphics.guiHeight() * anchor.row / 2 - scaledH * anchor.row / 2 + cfg.offsetY;
        x = Math.max(0, Math.min(x, graphics.guiWidth() - scaledW));
        y = Math.max(0, Math.min(y, graphics.guiHeight() - scaledH));

        int background = (Math.round(Math.max(0, Math.min(100, cfg.backgroundOpacity)) * 2.55f) << 24);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.fill(0, 0, panelW, panelH, background);
        int cursor = PAD;
        if (cfg.showTitle) {
            Gfx.text(graphics, font, title, PAD + (width - font.width(title)) / 2, cursor, 0xFFFFFFFF, false);
            cursor += line;
            graphics.fill(PAD, cursor - 1, PAD + width, cursor, 0x40FFFFFF);
            cursor += 2;
        }
        for (Row row : rows) {
            Gfx.text(graphics, font, row.name(), PAD, cursor, 0xFFFFFFFF, false);
            if (cfg.showValues) {
                Gfx.text(graphics, font, row.value(), PAD + width - font.width(row.value()), cursor, 0xFFFFFFFF, false);
            }
            cursor += line;
        }
        if (footer != null) Gfx.text(graphics, font, footer, PAD + width - font.width(footer), cursor, 0xFFA0A0A0, false);
        graphics.pose().popMatrix();
    }
}
