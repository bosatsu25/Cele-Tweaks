package io.github.bosatsu25.celetweaks.gametest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts how often an external mod's method ran to completion, fed by the RETURN spies in
 * {@code gametest.mixin}. A Cele-Tweaks guard cancels at the method head, so a guarded call never
 * reaches a RETURN and leaves its counter unchanged.
 */
public final class Spies {
    /** Item Scroller {@code KeybindCallbacks.onClientTickMassCraftImpl}. */
    public static final AtomicInteger MASS_CRAFT = new AtomicInteger();
    /** Tweakeroo {@code InventoryUtils.trySwitchToEffectiveTool}. */
    public static final AtomicInteger TOOL_SWITCH = new AtomicInteger();
    /** TweakerMore {@code SchematicBlockPicker.doSchematicWorldPickBlock}. */
    public static final AtomicInteger SCHEMATIC_PICK = new AtomicInteger();

    private Spies() {}
}
