package com.arthou.tpa.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeleportSavedData extends SavedData {
    private static final String DATA_NAME = "tpa_state";
    private static final Factory<TeleportSavedData> FACTORY = new Factory<>(TeleportSavedData::new, TeleportSavedData::load);

    private final Map<String, SavedLocation> warps = new HashMap<>();
    private final Map<UUID, Map<String, SavedLocation>> homes = new HashMap<>();
    private final Map<UUID, String> defaultHomes = new HashMap<>();
    private final Map<UUID, Set<UUID>> denyLists = new HashMap<>();
    private final Map<UUID, SavedLocation> lastLogoutLocations = new HashMap<>();
    private final Map<UUID, SavedLocation> backLocations = new HashMap<>();
    private SavedLocation spawn;

    public static TeleportSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static TeleportSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TeleportSavedData data = new TeleportSavedData();

        if (tag.contains("spawn", Tag.TAG_COMPOUND)) {
            data.spawn = SavedLocation.load(tag.getCompound("spawn"));
        }

        CompoundTag warpsTag = tag.getCompound("warps");
        for (String key : warpsTag.getAllKeys()) {
            data.warps.put(key, SavedLocation.load(warpsTag.getCompound(key)));
        }

        CompoundTag homesTag = tag.getCompound("homes");
        for (String uuidString : homesTag.getAllKeys()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid == null) {
                continue;
            }
            CompoundTag playerHomesTag = homesTag.getCompound(uuidString);
            Map<String, SavedLocation> playerHomes = new HashMap<>();
            for (String homeName : playerHomesTag.getAllKeys()) {
                playerHomes.put(homeName, SavedLocation.load(playerHomesTag.getCompound(homeName)));
            }
            data.homes.put(uuid, playerHomes);
        }

        CompoundTag defaultHomesTag = tag.getCompound("defaultHomes");
        for (String uuidString : defaultHomesTag.getAllKeys()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid != null) {
                data.defaultHomes.put(uuid, defaultHomesTag.getString(uuidString));
            }
        }

        CompoundTag denyListsTag = tag.getCompound("denyLists");
        for (String uuidString : denyListsTag.getAllKeys()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid == null) {
                continue;
            }
            Set<UUID> denySet = new HashSet<>();
            ListTag list = denyListsTag.getList(uuidString, Tag.TAG_STRING);
            for (Tag entry : list) {
                UUID target = parseUuid(entry.getAsString());
                if (target != null) {
                    denySet.add(target);
                }
            }
            data.denyLists.put(uuid, denySet);
        }

        CompoundTag logoutTag = tag.getCompound("logoutLocations");
        for (String uuidString : logoutTag.getAllKeys()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid != null) {
                data.lastLogoutLocations.put(uuid, SavedLocation.load(logoutTag.getCompound(uuidString)));
            }
        }

        CompoundTag backTag = tag.getCompound("backLocations");
        for (String uuidString : backTag.getAllKeys()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid != null) {
                data.backLocations.put(uuid, SavedLocation.load(backTag.getCompound(uuidString)));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (spawn != null) {
            tag.put("spawn", spawn.save(registries));
        }

        CompoundTag warpsTag = new CompoundTag();
        warps.forEach((name, location) -> warpsTag.put(name, location.save(registries)));
        tag.put("warps", warpsTag);

        CompoundTag homesTag = new CompoundTag();
        homes.forEach((uuid, playerHomes) -> {
            CompoundTag playerHomesTag = new CompoundTag();
            playerHomes.forEach((name, location) -> playerHomesTag.put(name, location.save(registries)));
            homesTag.put(uuid.toString(), playerHomesTag);
        });
        tag.put("homes", homesTag);

        CompoundTag defaultHomesTag = new CompoundTag();
        defaultHomes.forEach((uuid, homeName) -> defaultHomesTag.putString(uuid.toString(), homeName));
        tag.put("defaultHomes", defaultHomesTag);

        CompoundTag denyListsTag = new CompoundTag();
        denyLists.forEach((uuid, targets) -> {
            ListTag list = new ListTag();
            for (UUID target : targets) {
                list.add(StringTag.valueOf(target.toString()));
            }
            denyListsTag.put(uuid.toString(), list);
        });
        tag.put("denyLists", denyListsTag);

        CompoundTag logoutTag = new CompoundTag();
        lastLogoutLocations.forEach((uuid, location) -> logoutTag.put(uuid.toString(), location.save(registries)));
        tag.put("logoutLocations", logoutTag);

        CompoundTag backTag = new CompoundTag();
        backLocations.forEach((uuid, location) -> backTag.put(uuid.toString(), location.save(registries)));
        tag.put("backLocations", backTag);
        return tag;
    }

    public Map<String, SavedLocation> getWarps() {
        return warps;
    }

    public SavedLocation getSpawn() {
        return spawn;
    }

    public void setSpawn(SavedLocation spawn) {
        this.spawn = spawn;
        setDirty();
    }

    public void clearSpawn() {
        this.spawn = null;
        setDirty();
    }

    public Map<String, SavedLocation> getHomes(UUID playerId) {
        return homes.computeIfAbsent(playerId, ignored -> new HashMap<>());
    }

    public String getDefaultHome(UUID playerId) {
        return defaultHomes.get(playerId);
    }

    public void setDefaultHome(UUID playerId, String homeName) {
        defaultHomes.put(playerId, homeName);
        setDirty();
    }

    public void clearDefaultHome(UUID playerId) {
        defaultHomes.remove(playerId);
        setDirty();
    }

    public Set<UUID> getDenyList(UUID playerId) {
        return denyLists.computeIfAbsent(playerId, ignored -> new HashSet<>());
    }

    public SavedLocation getLogoutLocation(UUID playerId) {
        return lastLogoutLocations.get(playerId);
    }

    public void setLogoutLocation(UUID playerId, SavedLocation location) {
        lastLogoutLocations.put(playerId, location);
        setDirty();
    }

    public SavedLocation getBackLocation(UUID playerId) {
        return backLocations.get(playerId);
    }

    public void setBackLocation(UUID playerId, SavedLocation location) {
        backLocations.put(playerId, location);
        setDirty();
    }

    public void markChanged() {
        setDirty();
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
