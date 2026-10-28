package com.permadeath;

import com.permadeath.net.AnimationPacket;
import com.permadeath.net.Net;
import com.permadeath.net.SyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

public final class Manager {

    public static long currentDay(MinecraftServer server) {
        return server.overworld().getDayTime() / 24000L;
    }

    /** Activa un cambio al azar que todavia no este activo. Devuelve false si ya estan los 50. */
    public static boolean activateRandom(MinecraftServer server, PermadeathData data) {
        List<Change> pool = new ArrayList<>();
        for (Change c : Change.VALUES) {
            if (!data.has(c)) pool.add(c);
        }
        if (pool.isEmpty()) return false;
        Change picked = pool.get(server.overworld().random.nextInt(pool.size()));
        activate(server, data, picked);
        return true;
    }

    public static void activate(MinecraftServer server, PermadeathData data, Change change) {
        if (!data.add(change)) return;

        // Mensaje en el chat, todo en rojo.
        Component line = Component.literal("Cambio de dificultad: ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(change.title + " - " + change.description).withStyle(ChatFormatting.RED));
        server.getPlayerList().broadcastSystemMessage(line, false);

        // Animacion y sonido para todos.
        Net.sendToAll(new AnimationPacket(AnimationPacket.CHANGE, change.title));
        syncAll(server, data);

        // Aplicar a los mobs que ya estan cargados.
        long day = currentDay(server);
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob) {
                    MobBuffs.apply(mob, data, false, day);
                }
            }
        }
    }

    public static void syncAll(MinecraftServer server, PermadeathData data) {
        Net.sendToAll(SyncPacket.of(data.activeList()));
    }

    public static void syncTo(ServerPlayer player, PermadeathData data) {
        Net.sendTo(player, SyncPacket.of(data.activeList()));
    }

    private Manager() {}
}
