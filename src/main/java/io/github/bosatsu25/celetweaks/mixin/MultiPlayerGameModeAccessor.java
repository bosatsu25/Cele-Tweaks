package io.github.bosatsu25.celetweaks.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Private on 26.1.2, 26.2 and 26.3. */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    /** Sends the selected hotbar slot to the server if it changed. */
    @Invoker("ensureHasSentCarriedItem")
    void celetweaks$ensureHasSentCarriedItem();
}
