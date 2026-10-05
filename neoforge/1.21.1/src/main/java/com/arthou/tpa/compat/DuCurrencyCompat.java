package com.arthou.tpa.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

public final class DuCurrencyCompat {
    private static final String MOD_ID = "ducurrency";
    private static final String DATA_CLASS = "com.arthou.ducurrency.data.CurrencySavedData";
    private static final String NETWORK_CLASS = "com.arthou.ducurrency.network.CurrencyNetwork";
    private static final String FORMAT_CLASS = "com.arthou.ducurrency.util.MoneyFormat";

    private static ReflectionCache cache;
    private static boolean failed;

    private DuCurrencyCompat() {
    }

    public static boolean isEnabled() {
        return ModList.get().isLoaded(MOD_ID) && resolve() != null;
    }

    public static boolean canPay(ServerPlayer player, long cents) {
        if (!isEnabled()) {
            return true;
        }
        return balance(player) >= cents;
    }

    public static boolean charge(ServerPlayer player, long cents) {
        if (!isEnabled()) {
            return true;
        }

        long balance = balance(player);
        if (balance < cents) {
            return false;
        }

        setBalance(player, balance - cents);
        sync(player);
        return true;
    }

    public static void refund(ServerPlayer player, long cents) {
        if (!isEnabled()) {
            return;
        }
        setBalance(player, Math.addExact(balance(player), cents));
        sync(player);
    }

    public static long balance(ServerPlayer player) {
        ReflectionCache resolved = resolve();
        if (resolved == null) {
            return 0L;
        }

        try {
            Object data = resolved.getData.invoke(null, player.server);
            return (long) resolved.getBalance.invoke(data, player.getUUID());
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            failed = true;
            return 0L;
        }
    }

    public static String format(long cents) {
        ReflectionCache resolved = resolve();
        if (resolved == null) {
            return "Du$ " + (cents / 100L);
        }

        try {
            return (String) resolved.format.invoke(null, cents);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            failed = true;
            return "Du$ " + (cents / 100L);
        }
    }

    private static void setBalance(ServerPlayer player, long cents) {
        ReflectionCache resolved = resolve();
        if (resolved == null) {
            return;
        }

        try {
            Object data = resolved.getData.invoke(null, player.server);
            resolved.setBalance.invoke(data, player.getUUID(), cents);
        } catch (ReflectiveOperationException ignored) {
            failed = true;
        }
    }

    private static void sync(ServerPlayer player) {
        ReflectionCache resolved = resolve();
        if (resolved == null) {
            return;
        }

        try {
            resolved.sync.invoke(null, player);
        } catch (ReflectiveOperationException ignored) {
            failed = true;
        }
    }

    private static ReflectionCache resolve() {
        if (cache != null) {
            return cache;
        }
        if (failed || !ModList.get().isLoaded(MOD_ID)) {
            return null;
        }

        try {
            Class<?> dataClass = Class.forName(DATA_CLASS);
            Class<?> networkClass = Class.forName(NETWORK_CLASS);
            Class<?> formatClass = Class.forName(FORMAT_CLASS);
            cache = new ReflectionCache(
                dataClass.getMethod("get", MinecraftServer.class),
                dataClass.getMethod("getBalance", UUID.class),
                dataClass.getMethod("setBalance", UUID.class, long.class),
                networkClass.getMethod("sync", ServerPlayer.class),
                formatClass.getMethod("currency", long.class)
            );
            return cache;
        } catch (ReflectiveOperationException ignored) {
            failed = true;
            return null;
        }
    }

    private record ReflectionCache(
        Method getData,
        Method getBalance,
        Method setBalance,
        Method sync,
        Method format
    ) {
    }
}
