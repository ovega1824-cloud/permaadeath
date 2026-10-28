package com.permadeath;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class PermadeathCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("permadeath")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
                .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("next").executes(ctx -> next(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static int start(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        PermadeathData data = PermadeathData.get(server);
        if (data.started) {
            source.sendFailure(Component.literal("Permadeath ya está iniciado."));
            return 0;
        }
        data.started = true;
        data.lastDay = Manager.currentDay(server);
        data.setDirty();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("Permadeath ha comenzado. Cada día de Minecraft se activará un cambio de dificultad.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        Manager.activateRandom(server, data);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        PermadeathData data = PermadeathData.get(source.getServer());
        data.started = false;
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Permadeath en pausa: no se activarán más cambios. Los activos siguen."), true);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        PermadeathData data = PermadeathData.get(server);
        data.started = false;
        data.lastHordeDay = -1L;
        data.clearChanges();
        Manager.syncAll(server, data);
        source.sendSuccess(() -> Component.literal("Permadeath reiniciado: se quitaron todos los cambios."), true);
        return 1;
    }

    private static int next(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        PermadeathData data = PermadeathData.get(server);
        if (!Manager.activateRandom(server, data)) {
            source.sendFailure(Component.literal("Ya están activos los 50 cambios."));
            return 0;
        }
        return 1;
    }

    private static int status(CommandSourceStack source) {
        PermadeathData data = PermadeathData.get(source.getServer());
        String state = data.started ? "iniciado" : "sin iniciar";
        source.sendSuccess(() -> Component.literal("Permadeath: " + state + ", "
                + data.activeCount() + " de " + Change.VALUES.length + " cambios activos."), false);
        return 1;
    }

    private PermadeathCommand() {}
}
