package com.arthou.tpa.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeleportSavedData extends SavedData {
    private static final String DATA_NAME = "tpa_state";
    private static final Codec<TeleportSavedData> CODEC = CompoundTag.CODEC.xmap(TeleportSavedData::loadTag, TeleportSavedData::saveTag);
    private static final SavedDataType<TeleportSavedData> TYPE = new SavedDataType<>(DATA_NAME, TeleportSavedData::new, CODEC);

    private final Map<String, SavedLocation> warps = new HashMap<>();
    private final Map<UUID, Map<String, SavedLocation>> homes = new HashMap<>();
    private final Map<UUID, String> defaultHomes = new HashMap<>();
    private final Map<UUID, Set<UUID>> denyLists = new HashMap<>();
    private final Map<UUID, SavedLocation> lastLogoutLocations = new HashMap<>();
    private final Map<UUID, SavedLocation> backLocations = new HashMap<>();
    private SavedLocation spawn;

    public static TeleportSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static TeleportSavedData loadTag(CompoundTag tag) {
        TeleportSavedData data = new TeleportSavedData();

        if (tag.contains("spawn")) {
            data.spawn = SavedLocation.load(tag.getCompoundOrEmpty("spawn"));
        }

        CompoundTag warpsTag = tag.getCompoundOrEmpty("warps");
        for (String key : warpsTag.keySet()) {
            data.warps.put(key, SavedLocation.load(warpsTag.getCompoundOrEmpty(key)));
        }

        CompoundTag homesTag = tag.getCompoundOrEmpty("homes");
        for (String uuidString : homesTag.keySet()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid == null) {
                continue;
            }
            CompoundTag playerHomesTag = homesTag.getCompoundOrEmpty(uuidString);
            Map<String, SavedLocation> playerHomes = new HashMap<>();
            for (String homeName : playerHomesTag.keySet()) {
                playerHomes.put(homeName, SavedLocation.load(playerHomesTag.getCompoundOrEmpty(homeName)));
            }
            data.homes.put(uuid, playerHomes);
        }

        CompoundTag defaultHomesTag = tag.getCompoundOrEmpty("defaultHomes");
        for (String uuidString : defaultHomesTag.keySet()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid != null) {
                data.defaultHomes.put(uuid, defaultHomesTag.getStringOr(uuidString, ""));
            }
        }

        CompoundTag denyListsTag = tag.getCompoundOrEmpty("denyLists");
        for (String uuidString : denyListsTag.keySet()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid == null) {
                continue;
            }
            Set<UUID> denySet = new HashSet<>();
            ListTag list = denyListsTag.getListOrEmpty(uuidString);
            for (int i = 0; i < list.size(); i++) {
                UUID target = parseUuid(list.getStringOr(i, ""));
                if (target != null) {
                    denySet.add(target);
                }
            }
            data.denyLists.put(uuid, denySet);
        }

        CompoundTag logoutTag = tag.getCompoundOrEmpty("logoutLocations");
        for (String uuidString : logoutTag.keySet()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid != null) {
                data.lastLogoutLocations.put(uuid, SavedLocation.load(logoutTag.getCompoundOrEmpty(uuidString)));
            }
        }

        CompoundTag backTag = tag.getCompoundOrEmpty("backLocations");
        for (String uuidString : backTag.keySet()) {
            UUID uuid = parseUuid(uuidString);
            if (uuid != null) {
                data.backLocations.put(uuid, SavedLocation.load(backTag.getCompoundOrEmpty(uuidString)));
            }
        }

        return data;
    }

    private CompoundTag saveTag() {
        CompoundTag tag = new CompoundTag();
        if (spawn != null) {
            tag.put("spawn", spawn.save());
        }

        CompoundTag warpsTag = new CompoundTag();
        warps.forEach((name, location) -> warpsTag.put(name, location.save()));
        tag.put("warps", warpsTag);

        CompoundTag homesTag = new CompoundTag();
        homes.forEach((uuid, playerHomes) -> {
            CompoundTag playerHomesTag = new CompoundTag();
            playerHomes.forEach((name, location) -> playerHomesTag.put(name, location.save()));
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
        lastLogoutLocations.forEach((uuid, location) -> logoutTag.put(uuid.toString(), location.save()));
        tag.put("logoutLocations", logoutTag);

        CompoundTag backTag = new CompoundTag();
        backLocations.forEach((uuid, location) -> backTag.put(uuid.toString(), location.save()));
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
