package dev.hxrry.elytratrims.commands;

import dev.hxrry.elytratrims.ElytraTrims;
import dev.hxrry.elytratrims.component.ElytraData;
import dev.hxrry.elytratrims.config.Settings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ElytraTrimsCommand implements CommandExecutor, TabCompleter {

    private final ElytraTrims plugin;

    private static final List<String> SUBCOMMANDS = List.of("reload", "apply", "remove", "clear");
    private static final List<String> EFFECT_NAMES = List.of("glow", "cosmic", "animation");

    public ElytraTrimsCommand(ElytraTrims plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eElytraTrims §7v" + plugin.getDescription().getVersion());
            sender.sendMessage("§7Usage: /" + label + " <reload|apply|remove|clear>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "apply" -> handleApply(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "clear" -> handleClear(sender);
            default -> sender.sendMessage("§cUnknown subcommand. Use: reload, apply, remove, clear");
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(plugin.getSettings().getReloadMessage());
    }

    private void handleApply(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /elytratrims apply <glow|cosmic|animation>");
            return;
        }

        Settings.Effect effect = parseEffect(args[1]);
        if (effect == null) {
            sender.sendMessage("§cUnknown effect: " + args[1] + ". Options: glow, cosmic, animation");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ElytraData.isElytra(held)) {
            player.sendMessage(plugin.getSettings().getNotElytraMessage());
            return;
        }

        ElytraData.setEffect(held, effect, true);
        player.getInventory().setItemInMainHand(held);
        player.sendMessage(plugin.getSettings().getEffectAppliedMessage());
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /elytratrims remove <glow|cosmic|animation>");
            return;
        }

        Settings.Effect effect = parseEffect(args[1]);
        if (effect == null) {
            sender.sendMessage("§cUnknown effect: " + args[1] + ". Options: glow, cosmic, animation");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ElytraData.isElytra(held)) {
            player.sendMessage(plugin.getSettings().getNotElytraMessage());
            return;
        }

        ElytraData.setEffect(held, effect, false);
        player.getInventory().setItemInMainHand(held);
        player.sendMessage(plugin.getSettings().getEffectRemovedMessage());
    }

    private void handleClear(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ElytraData.isElytra(held)) {
            player.sendMessage(plugin.getSettings().getNotElytraMessage());
            return;
        }

        ElytraData.clearAllDecorations(held);
        player.getInventory().setItemInMainHand(held);
        player.sendMessage(plugin.getSettings().getEffectRemovedMessage());
    }

    private Settings.Effect parseEffect(String name) {
        return switch (name.toLowerCase()) {
            case "glow" -> Settings.Effect.GLOW;
            case "cosmic", "gateway" -> Settings.Effect.COSMIC;
            case "animation", "badapple" -> Settings.Effect.ANIMATION;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("apply") || args[0].equalsIgnoreCase("remove"))) {
            return filter(EFFECT_NAMES, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) filtered.add(option);
        }
        return filtered;
    }
}