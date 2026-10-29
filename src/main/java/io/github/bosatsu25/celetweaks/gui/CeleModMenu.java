package io.github.bosatsu25.celetweaks.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Mod Menu entry; Mod Menu is a compile-time-only dependency. */
public final class CeleModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FeatureListScreen::new;
    }
}
