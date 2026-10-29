package io.github.bosatsu25.celetweaks.mixin;

import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Before vanilla Pick Block runs: if its destination slot is reserved, select an empty unreserved
 * hotbar slot instead and sync that selection to the server (since 1.21.4 the server performs the
 * pick, using the slot it believes is selected); with no such slot, cancel the pick.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftPickBlockMixin {
    @Inject(method = "pickBlockOrEntity()V", at = @At("HEAD"), cancellable = true)
    private void celetweaks$respectHotbarLock(CallbackInfo ci) {
        if (!io.github.bosatsu25.celetweaks.config.CeleConfig.get().isEnabled(io.github.bosatsu25.celetweaks.catalog.FeatureDefinition.HOTBAR_LOCK)) return;
        Minecraft client = (Minecraft) (Object) this;
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;
        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;

        Inventory inventory = player.getInventory();
        BlockPos pos = blockHit.getBlockPos();
        ItemStack picked = new ItemStack(client.level.getBlockState(pos).getBlock().asItem());
        if (picked.isEmpty()) return;
        boolean creative = player.getAbilities().instabuild;
        int existing = inventory.findSlotMatchingItem(picked);
        if (Inventory.isHotbarSlot(existing)) return;          // vanilla only re-selects; nothing is overwritten
        if (!creative && existing == -1) return;               // vanilla does nothing in survival

        int destination = inventory.getSuitableHotbarSlot();
        if (!HotbarLockFeature.isLocked(destination)) return;

        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (!HotbarLockFeature.isLocked(slot) && inventory.getItem(slot).isEmpty()) {
                inventory.setSelectedSlot(slot);
                if (client.gameMode != null) {
                    ((MultiPlayerGameModeAccessor) client.gameMode).celetweaks$ensureHasSentCarriedItem();
                }
                return;
            }
        }
        player.sendOverlayMessage(Component.translatable("celetweaks.hotbar_lock.pick_blocked"));
        ci.cancel();
    }
}
