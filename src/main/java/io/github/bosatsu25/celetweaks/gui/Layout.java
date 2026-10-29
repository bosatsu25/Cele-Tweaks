package io.github.bosatsu25.celetweaks.gui;

/** Shared geometry for the three settings screens. */
final class Layout {
    static final int ROW_HEIGHT = 24;
    static final int WIDGET_HEIGHT = 20;
    static final int MARGIN = 10;
    static final int HEADER = 56;
    static final int FOOTER = 34;

    private Layout() {}

    static int rowsPerPage(int screenHeight) {
        return Math.max(1, (screenHeight - HEADER - FOOTER) / ROW_HEIGHT);
    }

    static int contentWidth(int screenWidth) {
        return Math.min(400, screenWidth - MARGIN * 2);
    }

    static int contentLeft(int screenWidth) {
        return (screenWidth - contentWidth(screenWidth)) / 2;
    }
}
