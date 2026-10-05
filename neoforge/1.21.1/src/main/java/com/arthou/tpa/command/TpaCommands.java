package com.arthou.tpa.command;

import com.arthou.tpa.compat.DuCurrencyCompat;
import com.arthou.tpa.data.SavedLocation;
import com.arthou.tpa.data.TeleportSavedData;
import com.arthou.tpa.message.TpaMessages;
import com.arthou.tpa.runtime.TeleportRuntime;
import com.arthou.tpa.runtime.TeleportUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

public final class TpaCommands {
    private static final long TELEPORT_COST_CENTS = 100L;

    private TpaCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tpa")
            .executes(TpaCommands::showHelp)
            .then(Commands.literal("reload").requires(source -> source.hasPermission(2)).executes(TpaCommands::reload))
            .then(Commands.literal("version").executes(context -> message(context.getSource(), "TPA NeoForge 1.0.1")))
            .then(Commands.literal("setlang")
                .then(Commands.argument("language", StringArgumentType.word())
                    .executes(context -> message(context.getSource(), "Per-player language is not implemented in this NeoForge port."))))
            .then(Commands.argument("target", EntityArgument.player()).executes(context -> sendRequest(context, TeleportRuntime.RequestType.TPA))));

        event.getDispatcher().register(Commands.literal("tphere")
            .then(Commands.argument("target", EntityArgument.player()).executes(context -> sendRequest(context, TeleportRuntime.RequestType.TPHERE))));

        event.getDispatcher().register(Commands.literal("tpahere")
            .then(Commands.argument("target", EntityArgument.player()).executes(context -> sendRequest(context, TeleportRuntime.RequestType.TPHERE))));

        event.getDispatcher().register(Commands.literal("tpaccept").executes(TpaCommands::acceptRequest));
        event.getDispatcher().register(Commands.literal("tpdeny").executes(TpaCommands::denyRequest));
        event.getDispatcher().register(Commands.literal("tpadeny").executes(TpaCommands::denyRequest));

        event.getDispatcher().register(Commands.literal("denys")
            .executes(TpaCommands::listDeniedPlayers)
            .then(Commands.literal("add")
                .then(Commands.argument("target", EntityArgument.player()).executes(TpaCommands::denyPlayer)))
            .then(Commands.literal("remove")
                .then(Commands.argument("target", EntityArgument.player()).executes(TpaCommands::allowPlayer))));

        event.getDispatcher().register(Commands.literal("warp")
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> suggestStrings(TeleportSavedData.get(context.getSource().getServer()).getWarps().keySet(), builder))
                .executes(TpaCommands::warp)));
        event.getDispatcher().register(Commands.literal("setwarp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.word()).executes(TpaCommands::setWarp)));
        event.getDispatcher().register(Commands.literal("delwarp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> suggestStrings(TeleportSavedData.get(context.getSource().getServer()).getWarps().keySet(), builder))
                .executes(TpaCommands::deleteWarp)));

        event.getDispatcher().register(Commands.literal("home")
            .executes(TpaCommands::homeDefault)
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> suggestHomeNames(context, builder))
                .executes(TpaCommands::homeNamed)));
        event.getDispatcher().register(Commands.literal("homes").executes(TpaCommands::listHomes));
        event.getDispatcher().register(Commands.literal("sethome")
            .then(Commands.argument("name", StringArgumentType.word()).executes(TpaCommands::setHome)));
        event.getDispatcher().register(Commands.literal("setdefaulthome")
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> suggestHomeNames(context, builder))
                .executes(TpaCommands::setDefaultHome)));
        event.getDispatcher().register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> suggestHomeNames(context, builder))
                .executes(TpaCommands::deleteHome)));

        event.getDispatcher().register(Commands.literal("spawn").executes(TpaCommands::spawn));
        event.getDispatcher().register(Commands.literal("setspawn").requires(source -> source.hasPermission(2)).executes(TpaCommands::setSpawn));
        event.getDispatcher().register(Commands.literal("delspawn").requires(source -> source.hasPermission(2)).executes(TpaCommands::deleteSpawn));
        event.getDispatcher().register(Commands.literal("back").executes(TpaCommands::back));
        event.getDispatcher().register(Commands.literal("rtp").executes(TpaCommands::rtp));

        event.getDispatcher().register(Commands.literal("tpall")
            .requires(source -> source.hasPermission(2))
            .executes(TpaCommands::tpAllToSelf)
            .then(Commands.literal("player")
                .then(Commands.argument("target", EntityArgument.player()).executes(TpaCommands::tpAllToPlayer)))
            .then(Commands.literal("warp")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> suggestStrings(TeleportSavedData.get(context.getSource().getServer()).getWarps().keySet(), builder))
                    .executes(TpaCommands::tpAllToWarp)))
            .then(Commands.literal("spawn").executes(TpaCommands::tpAllToSpawn)));

        event.getDispatcher().register(Commands.literal("tplogout")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.player()).executes(TpaCommands::tpLogout)));
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(TpaMessages.prefixed(net.minecraft.network.chat.Component.literal("Use /tpa <player>, /tpaccept, /warp, /home, /spawn, /back or /rtp.")));
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(TpaMessages.prefixed(net.minecraft.network.chat.Component.literal("This NeoForge port uses SavedData and does not need manual reload.")));
        return Command.SINGLE_SUCCESS;
    }

    private static int sendRequest(CommandContext<CommandSourceStack> context, TeleportRuntime.RequestType type) throws CommandSyntaxException {
        ServerPlayer requester = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        if (requester.getUUID().equals(target.getUUID())) {
            return TpaMessages.failure(context.getSource(), TpaMessages.cantTeleportToYourself());
        }

        long cooldownSeconds = TeleportRuntime.getRemainingCooldownSeconds(requester.getUUID());
        if (cooldownSeconds > 0L) {
            return TpaMessages.failure(context.getSource(), TpaMessages.cooldownMessage(cooldownSeconds));
        }

        Set<UUID> denyList = TeleportSavedData.get(requester.server).getDenyList(target.getUUID());
        if (denyList.contains(requester.getUUID())) {
            return TpaMessages.failure(context.getSource(), TpaMessages.blockedYourRequests(target.getGameProfile().getName()));
        }

        if (!canPayTeleport(requester)) {
            return TpaMessages.failure(context.getSource(), TpaMessages.notEnoughMoney(teleportCostText(), balanceText(requester)));
        }

        String costText = teleportCostText();
        TeleportRuntime.addRequest(requester, target, type);
        TpaMessages.send(requester, type == TeleportRuntime.RequestType.TPA
            ? TpaMessages.sendingTeleportRequest(target.getGameProfile().getName(), costText)
            : TpaMessages.sendingTeleportHereRequest(target.getGameProfile().getName(), costText));
        TpaMessages.send(target, TpaMessages.sentRequestOnYou(requester.getGameProfile().getName(), type, costText));
        TpaMessages.send(target, TpaMessages.acceptMessage());
        TpaMessages.send(target, TpaMessages.denyMessage());
        target.sendSystemMessage(TpaMessages.clickableDecisionRow());
        target.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 3.0F, 1.0F);
        return Command.SINGLE_SUCCESS;
    }

    private static int acceptRequest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        TeleportRuntime.PendingRequest request = TeleportRuntime.getRequest(target.getUUID());
        if (request == null) {
            return TpaMessages.failure(context.getSource(), TpaMessages.noRequests());
        }

        ServerPlayer requester = target.server.getPlayerList().getPlayer(request.requesterId());
        if (requester == null) {
            TeleportRuntime.removeRequest(target.getUUID());
            return TpaMessages.failure(context.getSource(), TpaMessages.playerOffline());
        }

        boolean shouldCharge = DuCurrencyCompat.isEnabled();
        if (!chargeTeleportCost(requester)) {
            TeleportRuntime.removeRequest(target.getUUID());
            TpaMessages.send(requester, TpaMessages.notEnoughMoney(teleportCostText(), balanceText(requester)));
            TpaMessages.send(target, TpaMessages.requesterNotEnoughMoney(requester.getGameProfile().getName(), teleportCostText()));
            return 0;
        }

        boolean result = request.type() == TeleportRuntime.RequestType.TPA
            ? TeleportUtil.teleportToPlayer(requester, target, true)
            : TeleportUtil.teleportToPlayer(target, requester, true);

        TeleportRuntime.removeRequest(target.getUUID());
        if (!result) {
            refundTeleportCost(requester);
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Teleport failed."));
        }

        if (shouldCharge) {
            TpaMessages.send(requester, TpaMessages.teleportCostCharged(teleportCostText(), balanceText(requester)));
        }
        TpaMessages.send(target, TpaMessages.requestAccepted());
        TpaMessages.send(requester, TpaMessages.acceptedBy(target.getGameProfile().getName()));
        return Command.SINGLE_SUCCESS;
    }

    private static int denyRequest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        TeleportRuntime.PendingRequest request = TeleportRuntime.getRequest(target.getUUID());
        if (request == null) {
            return TpaMessages.failure(context.getSource(), TpaMessages.noRequests());
        }
        ServerPlayer requester = target.server.getPlayerList().getPlayer(request.requesterId());
        if (requester != null) {
            TpaMessages.send(requester, TpaMessages.rejectedByPlayer());
        }
        TeleportRuntime.removeRequest(target.getUUID());
        return TpaMessages.success(context.getSource(), TpaMessages.requestDenied());
    }

    private static int denyPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        TeleportSavedData data = TeleportSavedData.get(player.server);
        data.getDenyList(player.getUUID()).add(target.getUUID());
        data.markChanged();
        return message(context.getSource(), target.getGameProfile().getName() + " added to your deny list.");
    }

    private static int allowPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        TeleportSavedData data = TeleportSavedData.get(player.server);
        data.getDenyList(player.getUUID()).remove(target.getUUID());
        data.markChanged();
        return message(context.getSource(), target.getGameProfile().getName() + " removed from your deny list.");
    }

    private static int listDeniedPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Set<UUID> denyList = TeleportSavedData.get(player.server).getDenyList(player.getUUID());
        if (denyList.isEmpty()) {
            return TpaMessages.success(context.getSource(), net.minecraft.network.chat.Component.literal("Your deny list is empty."));
        }
        return TpaMessages.success(context.getSource(), net.minecraft.network.chat.Component.literal("Blocked players: " + denyList.stream().map(UUID::toString).sorted().toList()));
    }

    private static int warp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        TeleportSavedData data = TeleportSavedData.get(context.getSource().getServer());
        SavedLocation location = data.getWarps().get(name);
        if (location == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Warp not found: " + name));
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        return teleportWithCost(context.getSource(), player, () -> TeleportUtil.teleport(player, location, true));
    }

    private static int setWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeleportSavedData data = TeleportSavedData.get(player.server);
        data.getWarps().put(name, SavedLocation.fromPlayer(player));
        data.markChanged();
        return message(context.getSource(), "Warp set: " + name);
    }

    private static int deleteWarp(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        TeleportSavedData data = TeleportSavedData.get(context.getSource().getServer());
        if (data.getWarps().remove(name) == null) {
            return failure(context.getSource(), "Warp not found: " + name);
        }
        data.markChanged();
        return message(context.getSource(), "Warp removed: " + name);
    }

    private static int homeDefault(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeleportSavedData data = TeleportSavedData.get(player.server);
        String homeName = data.getDefaultHome(player.getUUID());
        if (homeName == null) {
            if (data.getHomes(player.getUUID()).size() == 1) {
                homeName = data.getHomes(player.getUUID()).keySet().iterator().next();
            } else {
                return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("No default home set."));
            }
        }
        SavedLocation location = data.getHomes(player.getUUID()).get(homeName);
        if (location == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Home not found: " + homeName));
        }
        return teleportWithCost(context.getSource(), player, () -> TeleportUtil.teleport(player, location, true));
    }

    private static int homeNamed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        SavedLocation location = TeleportSavedData.get(player.server).getHomes(player.getUUID()).get(name);
        if (location == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Home not found: " + name));
        }
        return teleportWithCost(context.getSource(), player, () -> TeleportUtil.teleport(player, location, true));
    }

    private static int listHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Map<String, SavedLocation> homes = TeleportSavedData.get(player.server).getHomes(player.getUUID());
        if (homes.isEmpty()) {
            return TpaMessages.success(context.getSource(), net.minecraft.network.chat.Component.literal("You have no homes set."));
        }
        return message(context.getSource(), "Homes: " + homes.keySet().stream().sorted().toList());
    }

    private static int setHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        TeleportSavedData data = TeleportSavedData.get(player.server);
        data.getHomes(player.getUUID()).put(name, SavedLocation.fromPlayer(player));
        if (data.getDefaultHome(player.getUUID()) == null) {
            data.setDefaultHome(player.getUUID(), name);
        } else {
            data.markChanged();
        }
        return message(context.getSource(), "Home set: " + name);
    }

    private static int setDefaultHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        TeleportSavedData data = TeleportSavedData.get(player.server);
        if (!data.getHomes(player.getUUID()).containsKey(name)) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Home not found: " + name));
        }
        data.setDefaultHome(player.getUUID(), name);
        return message(context.getSource(), "Default home set to " + name);
    }

    private static int deleteHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        TeleportSavedData data = TeleportSavedData.get(player.server);
        if (data.getHomes(player.getUUID()).remove(name) == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Home not found: " + name));
        }
        if (name.equals(data.getDefaultHome(player.getUUID()))) {
            data.clearDefaultHome(player.getUUID());
        } else {
            data.markChanged();
        }
        return message(context.getSource(), "Home removed: " + name);
    }

    private static int spawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SavedLocation spawn = TeleportSavedData.get(player.server).getSpawn();
        if (spawn == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Spawn is not set."));
        }
        return teleportWithCost(context.getSource(), player, () -> TeleportUtil.teleport(player, spawn, true));
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeleportSavedData.get(player.server).setSpawn(SavedLocation.fromPlayer(player));
        return message(context.getSource(), "Spawn set.");
    }

    private static int deleteSpawn(CommandContext<CommandSourceStack> context) {
        TeleportSavedData.get(context.getSource().getServer()).clearSpawn();
        return message(context.getSource(), "Spawn removed.");
    }

    private static int back(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SavedLocation location = TeleportSavedData.get(player.server).getBackLocation(player.getUUID());
        if (location == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("You don't have any back locations!"));
        }
        return teleportWithCost(context.getSource(), player, () -> TeleportUtil.teleport(player, location, false));
    }

    private static int rtp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return teleportWithCost(context.getSource(), player, () -> TeleportUtil.randomTeleport(player, 500));
    }

    private static int tpAllToSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            if (!online.getUUID().equals(player.getUUID())) {
                TeleportUtil.teleportToPlayer(online, player, true);
            }
        }
        return message(context.getSource(), "Teleported everyone to you.");
    }

    private static int tpAllToPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        for (ServerPlayer online : target.server.getPlayerList().getPlayers()) {
            if (!online.getUUID().equals(target.getUUID())) {
                TeleportUtil.teleportToPlayer(online, target, true);
            }
        }
        return message(context.getSource(), "Teleported everyone to " + target.getGameProfile().getName() + ".");
    }

    private static int tpAllToWarp(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        SavedLocation warp = TeleportSavedData.get(context.getSource().getServer()).getWarps().get(name);
        if (warp == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Warp not found: " + name));
        }
        for (ServerPlayer online : context.getSource().getServer().getPlayerList().getPlayers()) {
            TeleportUtil.teleport(online, warp, true);
        }
        return message(context.getSource(), "Teleported everyone to warp " + name + ".");
    }

    private static int tpAllToSpawn(CommandContext<CommandSourceStack> context) {
        SavedLocation spawn = TeleportSavedData.get(context.getSource().getServer()).getSpawn();
        if (spawn == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("Spawn is not set."));
        }
        for (ServerPlayer online : context.getSource().getServer().getPlayerList().getPlayers()) {
            TeleportUtil.teleport(online, spawn, true);
        }
        return message(context.getSource(), "Teleported everyone to spawn.");
    }

    private static int tpLogout(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer actor = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        SavedLocation location = TeleportSavedData.get(actor.server).getLogoutLocation(target.getUUID());
        if (location == null) {
            return TpaMessages.failure(context.getSource(), net.minecraft.network.chat.Component.literal("The player doesn't have any back locations!"));
        }
        return TeleportUtil.teleport(actor, location, true) ? Command.SINGLE_SUCCESS : 0;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestHomeNames(CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return suggestStrings(TeleportSavedData.get(player.server).getHomes(player.getUUID()).keySet(), builder);
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestStrings(Iterable<String> values, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(values, builder);
    }

    private static int message(CommandSourceStack source, String message) {
        return TpaMessages.success(source, net.minecraft.network.chat.Component.literal(message));
    }

    private static int failure(CommandSourceStack source, String message) {
        return TpaMessages.failure(source, net.minecraft.network.chat.Component.literal(message));
    }

    private static int teleportWithCost(CommandSourceStack source, ServerPlayer player, BooleanSupplier teleportAction) {
        boolean shouldCharge = DuCurrencyCompat.isEnabled();
        if (!chargeTeleportCost(player)) {
            return TpaMessages.failure(source, TpaMessages.notEnoughMoney(teleportCostText(), balanceText(player)));
        }

        if (!teleportAction.getAsBoolean()) {
            refundTeleportCost(player);
            return 0;
        }

        if (shouldCharge) {
            TpaMessages.send(player, TpaMessages.teleportCostCharged(teleportCostText(), balanceText(player)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static boolean canPayTeleport(ServerPlayer player) {
        return DuCurrencyCompat.canPay(player, TELEPORT_COST_CENTS);
    }

    private static boolean chargeTeleportCost(ServerPlayer player) {
        return DuCurrencyCompat.charge(player, TELEPORT_COST_CENTS);
    }

    private static void refundTeleportCost(ServerPlayer player) {
        DuCurrencyCompat.refund(player, TELEPORT_COST_CENTS);
    }

    private static long balance(ServerPlayer player) {
        return DuCurrencyCompat.balance(player);
    }

    private static String balanceText(ServerPlayer player) {
        return DuCurrencyCompat.format(balance(player));
    }

    private static String teleportCostText() {
        return DuCurrencyCompat.isEnabled() ? DuCurrencyCompat.format(TELEPORT_COST_CENTS) : "";
    }
}
