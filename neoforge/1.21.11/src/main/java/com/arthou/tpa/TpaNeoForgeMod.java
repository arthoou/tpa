package com.arthou.tpa;

import com.arthou.tpa.command.TpaCommands;
import com.arthou.tpa.runtime.TeleportRuntime;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TpaNeoForgeMod.MOD_ID)
public final class TpaNeoForgeMod {
    public static final String MOD_ID = "tpa";

    public TpaNeoForgeMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(TpaCommands::register);
        NeoForge.EVENT_BUS.addListener(TeleportRuntime::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(TeleportRuntime::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(TeleportRuntime::onPlayerDeath);
    }
}
