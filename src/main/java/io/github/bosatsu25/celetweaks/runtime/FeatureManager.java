package io.github.bosatsu25.celetweaks.runtime;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.feature.blockfilter.BlockFilterFeature;
import io.github.bosatsu25.celetweaks.feature.entityfilter.EntityFilterFeature;
import io.github.bosatsu25.celetweaks.feature.grey.GreyColorsFeature;
import io.github.bosatsu25.celetweaks.feature.guard.ItemUseGuardFeature;
import io.github.bosatsu25.celetweaks.feature.guard.NoDigUnderfootFeature;
import io.github.bosatsu25.celetweaks.feature.guard.PortalProtectionFeature;
import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import io.github.bosatsu25.celetweaks.feature.integration.AutoPickScopeFeature;
import io.github.bosatsu25.celetweaks.feature.integration.MaterialListRecountFeature;
import io.github.bosatsu25.celetweaks.feature.integration.MaterialSubstitutionFeature;
import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SchematicDragDropFeature;
import io.github.bosatsu25.celetweaks.feature.integration.ShiftToMassCraftFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SyncerTuneFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SyncmaticaRemoveLockFeature;
import io.github.bosatsu25.celetweaks.feature.integration.ToolSwitchScopeFeature;
import io.github.bosatsu25.celetweaks.feature.integration.TweakerooResyncFeature;
import io.github.bosatsu25.celetweaks.feature.lava.LavaSourceTintFeature;
import io.github.bosatsu25.celetweaks.feature.log.ActionLogFeature;
import io.github.bosatsu25.celetweaks.feature.look.SlowLookFeature;
import io.github.bosatsu25.celetweaks.feature.notes.TextNotesFeature;
import io.github.bosatsu25.celetweaks.feature.range.RangeOverlayFeature;
import io.github.bosatsu25.celetweaks.feature.sidebar.SidebarLayoutFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns every runtime component. A component that throws during init is dropped; one that keeps
 * throwing during tick or session reset is quarantined so the rest of the mod keeps working.
 */
public final class FeatureManager {
    /** Tick failures tolerated before a component is quarantined. */
    static final int MAX_TICK_FAILURES = 3;

    private static final FeatureManager INSTANCE = new FeatureManager();

    private final Map<String, Slot> slots = new LinkedHashMap<>();
    private volatile Slot[] schedule = new Slot[0];
    private boolean initialized;
    private ClientLevel lastLevel;

    private FeatureManager() {}

    public static FeatureManager get() { return INSTANCE; }

    public synchronized void init() {
        if (initialized) return;

        register(new SidebarLayoutFeature());
        register(new GreyColorsFeature());
        register(new PortalProtectionFeature());
        register(new NoDigUnderfootFeature());
        register(new ItemUseGuardFeature());
        register(new HotbarLockFeature());
        register(new SlowLookFeature());
        register(new TextNotesFeature());
        register(new ActionLogFeature());
        register(new RangeOverlayFeature());
        register(new EntityFilterFeature());
        register(new BlockFilterFeature());
        register(new LavaSourceTintFeature());
        register(new ShiftToMassCraftFeature());
        register(new SyncmaticaRemoveLockFeature());
        register(new ToolSwitchScopeFeature());
        register(new TweakerooResyncFeature());
        register(new AutoPickScopeFeature());
        register(new SyncerTuneFeature());
        register(new PlacementConversionsFeature());
        register(new MaterialSubstitutionFeature());
        register(new MaterialListRecountFeature());
        register(new SchematicDragDropFeature());

        List<Slot> live = new ArrayList<>();
        for (Slot slot : slots.values()) {
            if (slot.initialize()) live.add(slot);
        }
        schedule = live.toArray(Slot[]::new);
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public synchronized void register(RuntimeComponent component) {
        if (initialized) throw new IllegalStateException("Components cannot be registered after init");
        String id = Objects.requireNonNull(component.id(), "component id");
        if (slots.putIfAbsent(id, new Slot(component)) != null) {
            throw new IllegalStateException("Duplicate component id: " + id);
        }
    }

    public Optional<RuntimeComponent> component(String id) {
        Slot slot = slots.get(id);
        return slot == null || slot.quarantined ? Optional.empty() : Optional.of(slot.component);
    }

    public boolean isQuarantined(FeatureDefinition definition) {
        for (Slot slot : slots.values()) {
            if (slot.component.definitions().contains(definition)) return slot.quarantined;
        }
        return false;
    }

    /** Routes a toggle to every live component serving that definition. */
    public void notifyToggle(FeatureDefinition definition, boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        for (Slot slot : schedule) {
            if (slot.component.definitions().contains(definition)) {
                slot.guard(client, "toggle", () -> slot.component.onToggle(definition, enabled));
            }
        }
    }

    private void onClientTick(Minecraft client) {
        if (client.level != lastLevel) {
            lastLevel = client.level;
            for (Slot slot : schedule) slot.guard(client, "session-reset", () -> slot.component.resetSession(client));
        }
        for (Slot slot : schedule) slot.tick(client);
    }

    private static final class Slot {
        final RuntimeComponent component;
        int tickFailures;
        volatile boolean quarantined;

        Slot(RuntimeComponent component) { this.component = component; }

        boolean initialize() {
            try {
                component.init();
                return true;
            } catch (RuntimeException | LinkageError failure) {
                quarantine(Minecraft.getInstance(), "init", failure);
                return false;
            }
        }

        void tick(Minecraft client) {
            if (quarantined) return;
            try {
                component.tick(client);
                tickFailures = 0;
            } catch (RuntimeException | LinkageError failure) {
                if (++tickFailures >= MAX_TICK_FAILURES) quarantine(client, "tick", failure);
            }
        }

        void guard(Minecraft client, String stage, Runnable body) {
            if (quarantined) return;
            try {
                body.run();
            } catch (RuntimeException | LinkageError failure) {
                quarantine(client, stage, failure);
            }
        }

        private void quarantine(Minecraft client, String stage, Throwable failure) {
            if (quarantined) return;
            quarantined = true;
            CeleTweaksClient.LOGGER.error("Component '{}' quarantined during {}", component.id(), stage, failure);
            try {
                component.onQuarantined(client);
            } catch (RuntimeException | LinkageError cleanupFailure) {
                CeleTweaksClient.LOGGER.warn("Component '{}' cleanup failed", component.id(), cleanupFailure);
            }
        }
    }
}
