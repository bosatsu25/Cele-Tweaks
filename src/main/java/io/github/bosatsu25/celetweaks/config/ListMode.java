package io.github.bosatsu25.celetweaks.config;

/** How an id list is interpreted. */
public enum ListMode {
    /** Entries in the list are hidden / blocked; everything else passes. */
    HIDE,
    /** Only entries in the list pass; everything else is hidden / blocked. */
    ALLOW;

    /** @return true when {@code inList} should be affected by the feature. */
    public boolean affects(boolean inList) {
        return this == HIDE ? inList : !inList;
    }
}
