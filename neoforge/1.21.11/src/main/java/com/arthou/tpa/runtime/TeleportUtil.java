package com.arthou.tpa.runtime;

import com.arthou.tpa.data.SavedLocation;
import com.arthou.tpa.data.TeleportSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;
import java.util.Set;

public final class TeleportUtil {
    private static final Random RANDOM = new Random();

    private TeleportUtil() {
    }

    public static boolean teleport(ServerPlayer player, SavedLocation location, boolean saveBack) {
        ServerLevel level = location.resolveLevel(player.level().getServer());
        if (level == null) {
            player.sendSystemMessage(Component.literal("Target world is unavailable."));
            return false;
        }
        if (saveBack) {
            TeleportSavedData.get(player.level().getServer()).setBackLocation(player.getUUID(), SavedLocation.fromPlayer(player));
        }
        player.teleportTo(level, location.x(), location.y(), location.z(), Set.<Relative>of(), location.yaw(), location.pitch(), true);
        return true;
    }

    public static boolean teleportToPlayer(ServerPlayer player, ServerPlayer target, boolean saveBack) {
        return teleport(player, SavedLocation.fromPlayer(target), saveBack);
    }

    public static boolean randomTeleport(ServerPlayer player, int horizontalLimit) {
        ServerLevel level = player.level();
        int x = player.blockPosition().getX() + RANDOM.nextInt(horizontalLimit * 2 + 1) - horizontalLimit;
        int z = player.blockPosition().getZ() + RANDOM.nextInt(horizontalLimit * 2 + 1) - horizontalLimit;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        return teleport(player, new SavedLocation(level.dimension().identifier().toString(), x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot()), true);
    }
}
