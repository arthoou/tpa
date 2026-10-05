package com.arthou.tpa.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record SavedLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
    public static SavedLocation fromPlayer(ServerPlayer player) {
        return new SavedLocation(
            player.serverLevel().dimension().location().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
    }

    public CompoundTag save(HolderLookup.Provider registries) {
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
            tag.getString("dimension"),
            tag.getDouble("x"),
            tag.getDouble("y"),
            tag.getDouble("z"),
            tag.getFloat("yaw"),
            tag.getFloat("pitch")
        );
    }

    public ServerLevel resolveLevel(MinecraftServer server) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) {
            return server.overworld();
        }
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
}
