package io.github.bosatsu25.celetweaks.gametest.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the guard test run what the Pick Block key ends in, without the mouse and camera in between. */
@Mixin(Minecraft.class)
public interface MinecraftInvoker {
    @Invoker("pickBlockOrEntity")
    void celetweaks$pickBlockOrEntity();
}
