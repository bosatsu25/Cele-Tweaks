package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Files dropped onto a Litematica schematic browser are copied into the folder the browser shows
 * (or the schematics root). Only regular, non-symlink files with a schematic extension and a bounded
 * size are accepted; existing files are never overwritten. Litematica is reached by reflection only.
 */
public final class SchematicDragDropFeature extends Feature {
    private static final Set<String> EXTENSIONS = Set.of("litematic", "schem", "schematic", "nbt");
    private static final String BROWSER_BASE = "fi.dy.masa.litematica.gui.GuiSchematicBrowserBase";

    public SchematicDragDropFeature() {
        super(FeatureDefinition.SCHEMATIC_DRAG_DROP);
    }

    /** @return true when the drop was handled (the screen is a Litematica schematic browser). */
    public static boolean onFilesDrop(Screen screen, List<Path> paths) {
        if (screen == null || paths == null || !CeleConfig.get().isEnabled(FeatureDefinition.SCHEMATIC_DRAG_DROP)) return false;
        if (!isSchematicBrowser(screen)) return false;
        Config cfg = CeleConfig.get().section(FeatureDefinition.SCHEMATIC_DRAG_DROP, Config.class);
        long maxBytes = (long) Math.max(1, cfg.maxFileSizeMiB) * 1024 * 1024;
        Path base = schematicsBase();
        Path target = browserDirectory(screen, base);
        int imported = 0;
        int rejected = 0;
        for (Path source : paths) {
            try {
                if (!acceptable(source, maxBytes)) { rejected++; continue; }
                Files.createDirectories(target);
                Files.copy(source, uniqueTarget(target, source.getFileName().toString()));
                imported++;
            } catch (IOException | RuntimeException failure) {
                rejected++;
                CeleTweaksClient.LOGGER.warn("Schematic drag & drop failed for {}", source.getFileName(), failure);
            }
        }
        refreshBrowser(screen);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.translatable("celetweaks.schematic_drag_drop.result", imported, rejected));
        }
        return true;
    }

    private static boolean isSchematicBrowser(Screen screen) {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            if (BROWSER_BASE.equals(type.getName())) return true;
        }
        return false;
    }

    private static boolean acceptable(Path source, long maxBytes) throws IOException {
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) return false;
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || !EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT))) return false;
        return Files.size(source) <= maxBytes;
    }

    private static Path uniqueTarget(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName).normalize();
        if (!candidate.startsWith(directory)) throw new IllegalArgumentException("unsafe file name");
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        for (int i = 1; Files.exists(candidate, LinkOption.NOFOLLOW_LINKS); i++) {
            candidate = directory.resolve(stem + " (" + i + ")" + ext);
        }
        return candidate;
    }

    private static Path schematicsBase() {
        try {
            Object dir = Class.forName("fi.dy.masa.litematica.data.DataManager").getMethod("getSchematicsBaseDirectory").invoke(null);
            Path path = dir instanceof Path p ? p : dir instanceof File f ? f.toPath() : null;
            if (path != null) return path.toAbsolutePath().normalize();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            CeleTweaksClient.LOGGER.debug("Litematica schematics directory unavailable: {}", failure.toString());
        }
        return FabricLoader.getInstance().getGameDir().resolve("schematics").toAbsolutePath().normalize();
    }

    /** The folder the browser currently shows, if it lies inside the schematics root; otherwise the root. */
    private static Path browserDirectory(Screen screen, Path base) {
        try {
            Object list = findMethod(screen.getClass(), "getListWidget").invoke(screen);
            Object dir = list == null ? null : findMethod(list.getClass(), "getCurrentDirectory").invoke(list);
            Path path = dir instanceof Path p ? p : dir instanceof File f ? f.toPath() : null;
            if (path != null) {
                Path normalized = path.toAbsolutePath().normalize();
                if (normalized.startsWith(base) && Files.isDirectory(normalized)) return normalized;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            CeleTweaksClient.LOGGER.debug("Litematica browser directory unavailable: {}", failure.toString());
        }
        return base;
    }

    private static void refreshBrowser(Screen screen) {
        try {
            Object list = findMethod(screen.getClass(), "getListWidget").invoke(screen);
            if (list != null) findMethod(list.getClass(), "refreshEntries").invoke(list);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            CeleTweaksClient.LOGGER.debug("Litematica browser refresh unavailable: {}", failure.toString());
        }
    }

    private static Method findMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(name);
    }

    public static final class Config {
        @Range(min = 1, max = 256) public int maxFileSizeMiB = 32;
    }
}
