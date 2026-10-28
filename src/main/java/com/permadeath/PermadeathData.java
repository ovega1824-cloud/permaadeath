package com.permadeath;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Estado guardado del mundo: si esta iniciado, el ultimo dia procesado y los cambios activos. */
public class PermadeathData extends SavedData {
    public boolean started = false;
    public long lastDay = 0L;
    public long lastHordeDay = -1L;
    private final Set<Change> active = new LinkedHashSet<>();

    public static PermadeathData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return new PermadeathData();
        return overworld.getDataStorage()
                .computeIfAbsent(PermadeathData::load, PermadeathData::new, "permadeath");
    }

    public static PermadeathData load(CompoundTag tag) {
        PermadeathData d = new PermadeathData();
        d.started = tag.getBoolean("started");
        d.lastDay = tag.getLong("lastDay");
        d.lastHordeDay = tag.contains("lastHordeDay") ? tag.getLong("lastHordeDay") : -1L;
        ListTag list = tag.getList("active", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                d.active.add(Change.valueOf(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("started", started);
        tag.putLong("lastDay", lastDay);
        tag.putLong("lastHordeDay", lastHordeDay);
        ListTag list = new ListTag();
        for (Change c : active) {
            list.add(StringTag.valueOf(c.name()));
        }
        tag.put("active", list);
        return tag;
    }

    public boolean has(Change c) {
        return active.contains(c);
    }

    public boolean add(Change c) {
        boolean added = active.add(c);
        if (added) setDirty();
        return added;
    }

    public void clearChanges() {
        active.clear();
        setDirty();
    }

    public List<Change> activeList() {
        return new ArrayList<>(active);
    }

    public int activeCount() {
        return active.size();
    }
}
