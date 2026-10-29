package io.github.bosatsu25.celetweaks.feature.hotbar;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.BitSet;

/**
 * Locks hotbar slots against being overwritten by Pick Block (and, through Auto Pick Scope, by
 * TweakerMore's schematic pick). Slots are locked from the settings or with the "lock current slot"
 * hotkey, which toggles the slot under the cursor.
 */
public final class HotbarLockFeature extends Feature {
    public static final String TOGGLE_CURRENT = "hotbar_lock.toggle_current";

    public HotbarLockFeature() {
        super(FeatureDefinition.HOTBAR_LOCK);
    }

    @Override
    public void init() {
        HotkeyManager.register(TOGGLE_CURRENT, Component.translatable("celetweaks.hotkey.hotbar_lock.toggle_current"), this::toggleCurrent);
    }

    private void toggleCurrent() {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || client.player == null) return;
        int slot = client.player.getInventory().getSelectedSlot();
        BitSet locked = lockedSlots();
        locked.flip(slot);
        Config cfg = config(Config.class);
        StringBuilder text = new StringBuilder();
        for (int i = locked.nextSetBit(0); i >= 0; i = locked.nextSetBit(i + 1)) {
            if (!text.isEmpty()) text.append(' ');
            text.append(i + 1);
        }
        cfg.lockedSlots = text.toString();
        CeleConfig.get().save();
        client.player.sendOverlayMessage(Component.translatable(
                locked.get(slot) ? "celetweaks.hotbar_lock.locked" : "celetweaks.hotbar_lock.unlocked", slot + 1));
    }

    /** Locked hotbar slots, 0-based. Parsed from "1 5 9" style text (1-based for people). */
    public static BitSet lockedSlots() {
        BitSet result = new BitSet(9);
        String text = CeleConfig.get().section(FeatureDefinition.HOTBAR_LOCK, Config.class).lockedSlots;
        if (text == null) return result;
        for (String token : text.split("[^0-9]+")) {
            if (token.isEmpty()) continue;
            try {
                int slot = Integer.parseInt(token) - 1;
                if (slot >= 0 && slot < 9) result.set(slot);
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return result;
    }

    public static boolean isLocked(int slot) {
        return CeleConfig.get().isEnabled(FeatureDefinition.HOTBAR_LOCK) && lockedSlots().get(slot);
    }

    public static final class Config {
        /** Hotbar slots 1-9 separated by spaces, e.g. "1 9". */
        public String lockedSlots = "";
    }
}
