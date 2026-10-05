package com.arthou.tpa.message;

import com.arthou.tpa.runtime.TeleportRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class TpaMessages {
    private static final String PREFIX_TEXT = "[Arthou's TPA]";

    private TpaMessages() {
    }

    public static Component prefixed(Component message) {
        return Component.empty()
            .append(Component.literal(PREFIX_TEXT).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal(" "))
            .append(message.copy().withStyle(ChatFormatting.GOLD));
    }

    public static int success(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> prefixed(message), false);
        return 1;
    }

    public static int failure(CommandSourceStack source, Component message) {
        source.sendFailure(prefixed(message));
        return 0;
    }

    public static void send(ServerPlayer player, Component message) {
        player.sendSystemMessage(prefixed(message));
    }

    public static Component sendingTeleportRequest(String playerName, String cost) {
        MutableComponent message = Component.literal("Sending a teleport request to ")
            .append(Component.literal(playerName).withStyle(ChatFormatting.RED));
        appendCostNotice(message, cost);
        return message;
    }

    public static Component sendingTeleportHereRequest(String playerName, String cost) {
        MutableComponent message = Component.literal("Sending a teleport here request to ")
            .append(Component.literal(playerName).withStyle(ChatFormatting.RED));
        appendCostNotice(message, cost);
        return message;
    }

    public static Component sentRequestOnYou(String playerName, TeleportRuntime.RequestType type, String cost) {
        String middle = type == TeleportRuntime.RequestType.TPA ? "has sent you a teleport request!" : "has sent you a teleport here request!";
        MutableComponent message = Component.literal("")
            .append(Component.literal(playerName).withStyle(ChatFormatting.RED))
            .append(Component.literal(" " + middle));
        if (hasCost(cost)) {
            message.append(Component.literal(" Cost: "))
                .append(Component.literal(cost).withStyle(ChatFormatting.YELLOW));
        }
        return message.append(Component.literal("."));
    }

    public static Component acceptedBy(String playerName) {
        return Component.literal("")
            .append(Component.literal(playerName).withStyle(ChatFormatting.RED))
            .append(Component.literal(" has accepted your teleportation request."));
    }

    public static Component rejectedByPlayer() {
        return Component.literal("The player has rejected your request!");
    }

    public static Component requestAccepted() {
        return Component.literal("You accepted the teleport request!");
    }

    public static Component requestDenied() {
        return Component.literal("You rejected the request!");
    }

    public static Component noRequests() {
        return Component.literal("You don't have any teleport requests!");
    }

    public static Component playerOffline() {
        return Component.literal("The player is not online!");
    }

    public static Component cantTeleportToYourself() {
        return Component.literal("You can't teleport to yourself!");
    }

    public static Component blockedYourRequests(String playerName) {
        return Component.literal("The player ").append(Component.literal(playerName).withStyle(ChatFormatting.RED)).append(Component.literal(" has ignored your teleportation requests!"));
    }

    public static Component cooldownMessage(long seconds) {
        return Component.literal("You can't use that command for ").append(Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.RED)).append(Component.literal(" seconds yet!"));
    }

    public static Component notEnoughMoney(String cost, String balance) {
        return Component.literal("You need ")
            .append(Component.literal(cost).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" to teleport. Your balance is "))
            .append(Component.literal(balance).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("."));
    }

    public static Component requesterNotEnoughMoney(String playerName, String cost) {
        return Component.literal("")
            .append(Component.literal(playerName).withStyle(ChatFormatting.RED))
            .append(Component.literal(" does not have "))
            .append(Component.literal(cost).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(", so the teleport was canceled."));
    }

    public static Component teleportCostCharged(String cost, String balance) {
        return Component.literal("Teleport cost charged: ")
            .append(Component.literal(cost).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(". New balance: "))
            .append(Component.literal(balance).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("."));
    }

    public static Component clickableDecisionRow() {
        MutableComponent accept = Component.literal("ACCEPT")
            .withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, prefixed(Component.literal("Click here to ACCEPT the tp request!")))));

        MutableComponent middle = Component.literal(" | ")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD);

        MutableComponent deny = Component.literal("DENY")
            .withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, prefixed(Component.literal("Click here to DENY the tp request!")))));

        return prefixed(Component.empty().append(accept).append(middle).append(deny));
    }

    public static Component acceptMessage() {
        return Component.literal("To accept the teleport request, type ").append(Component.literal("/tpaccept").withStyle(ChatFormatting.RED));
    }

    public static Component denyMessage() {
        return Component.literal("To deny the teleport request, type ").append(Component.literal("/tpdeny").withStyle(ChatFormatting.RED));
    }

    private static void appendCostNotice(MutableComponent message, String cost) {
        if (hasCost(cost)) {
            message.append(Component.literal(". This teleport will cost "))
                .append(Component.literal(cost).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" if accepted."));
            return;
        }
        message.append(Component.literal("."));
    }

    private static boolean hasCost(String cost) {
        return cost != null && !cost.isBlank();
    }
}
