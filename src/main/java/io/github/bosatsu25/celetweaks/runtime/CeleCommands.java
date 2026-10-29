package io.github.bosatsu25.celetweaks.runtime;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.bosatsu25.celetweaks.feature.log.ActionLogFeature;
import io.github.bosatsu25.celetweaks.feature.memo.SiteMemoFeature;
import io.github.bosatsu25.celetweaks.gui.CeleScreens;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;

/** {@code /celetweaks settings | memo | log [page] | log clear}. */
public final class CeleCommands {
    private CeleCommands() {}

    public static void register() {
        EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("celetweaks")
                        .then(literal("settings").executes(ctx -> {
                            CeleScreens.openSettingsNextTick();
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(literal("memo").executes(ctx -> {
                            SiteMemoFeature.openIfEnabled();
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(literal("log")
                                .executes(ctx -> {
                                    ActionLogFeature.printPage(1, line -> ctx.getSource().sendFeedback(line));
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(argument("page", IntegerArgumentType.integer(1)).executes(ctx -> {
                                    ActionLogFeature.printPage(IntegerArgumentType.getInteger(ctx, "page"),
                                            line -> ctx.getSource().sendFeedback(line));
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(literal("clear").executes(ctx -> {
                                    ActionLogFeature.clear();
                                    ctx.getSource().sendFeedback(Component.translatable("celetweaks.action_log.cleared"));
                                    return Command.SINGLE_SUCCESS;
                                })))));
    }
}
