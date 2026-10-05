package com.arthou.tpa.runtime;

import com.arthou.tpa.data.SavedLocation;
import com.arthou.tpa.data.TeleportSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeleportRuntime {
    private static final long REQUEST_TIMEOUT_MS = 30_000L;
    private static final long REQUEST_COOLDOWN_MS = 10_000L;
    private static final Map<UUID, PendingRequest> PENDING_REQUESTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST_AT = new HashMap<>();

    private TeleportRuntime() {
    }

    public static void addRequest(ServerPlayer requester, ServerPlayer target, RequestType type) {
        PENDING_REQUESTS.put(target.getUUID(), new PendingRequest(requester.getUUID(), requester.getGameProfile().name(), type, System.currentTimeMillis()));
        LAST_REQUEST_AT.put(requester.getUUID(), System.currentTimeMillis());
    }

    public static long getRemainingCooldownSeconds(UUID requesterId) {
        Long lastRequestAt = LAST_REQUEST_AT.get(requesterId);
        if (lastRequestAt == null) {
            return 0L;
        }
        long remainingMs = REQUEST_COOLDOWN_MS - (System.currentTimeMillis() - lastRequestAt);
        if (remainingMs <= 0L) {
            return 0L;
        }
        return Math.max(1L, (long) Math.ceil(remainingMs / 1000.0D));
    }

    public static PendingRequest getRequest(UUID targetId) {
        PendingRequest request = PENDING_REQUESTS.get(targetId);
        if (request == null) {
            return null;
        }
        if (System.currentTimeMillis() - request.createdAtMs() > REQUEST_TIMEOUT_MS) {
            PENDING_REQUESTS.remove(targetId);
            return null;
        }
        return request;
    }

    public static void removeRequest(UUID targetId) {
        PENDING_REQUESTS.remove(targetId);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportSavedData.get(player.level().getServer()).setLogoutLocation(player.getUUID(), SavedLocation.fromPlayer(player));
            removeRequest(player.getUUID());
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SavedLocation spawn = TeleportSavedData.get(player.level().getServer()).getSpawn();
            if (spawn != null) {
                TeleportUtil.teleport(player, spawn, true);
            }
        }
    }

    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportSavedData.get(player.level().getServer()).setBackLocation(player.getUUID(), SavedLocation.fromPlayer(player));
        }
    }

    public enum RequestType {
        TPA,
        TPHERE
    }

    public record PendingRequest(UUID requesterId, String requesterName, RequestType type, long createdAtMs) {
        public Component message() {
            return Component.literal(requesterName + " sent a " + type.name().toLowerCase() + " request.");
        }
    }
}
