package com.arthou.tpa.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record SavedLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
    public static SavedLocation fromPlayer(ServerPlayer player) {
        return new SavedLocation(
            player.level().dimension().identifier().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        return tag;
    }

    public static SavedLocation load(CompoundTag tag) {
        return new SavedLocation(
            tag.getStringOr("dimension", Level.OVERWORLD.identifier().toString()),
            tag.getDoubleOr("x", 0.0D),
            tag.getDoubleOr("y", 64.0D),
            tag.getDoubleOr("z", 0.0D),
            tag.getFloatOr("yaw", 0.0F),
            tag.getFloatOr("pitch", 0.0F)
        );
    }

    public ServerLevel resolveLevel(MinecraftServer server) {
        Identifier id = Identifier.tryParse(dimension);
        if (id == null) {
            return server.overworld();
        }
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
}
