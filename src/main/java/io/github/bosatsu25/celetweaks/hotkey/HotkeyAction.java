package io.github.bosatsu25.celetweaks.hotkey;

import net.minecraft.network.chat.Component;

/** Something a key combo can trigger. {@code id} is the key in hotkeys.json. */
public record HotkeyAction(String id, Component name, Runnable run) {}
