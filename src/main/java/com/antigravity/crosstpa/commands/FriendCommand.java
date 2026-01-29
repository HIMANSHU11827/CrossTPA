package com.antigravity.crosstpa.commands;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FriendCommand implements CommandExecutor {

    private final CrossTPA plugin;

    public FriendCommand(CrossTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return true;

        String prefix = plugin.getConfig().getString("messages.prefix");

        if (args.length == 0) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + "<gray>Friend Commands: /friend add/accept/remove <player></gray>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add" -> {
                if (args.length < 2)
                    return false;
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Player not found!</red>"));
                    return true;
                }
                plugin.getFriendManager().sendFriendRequest(player, target);
            }
            case "accept" -> {
                if (args.length < 2)
                    return false;
                plugin.getFriendManager().acceptFriendRequest(player, args[1]);
            }
            case "remove" -> {
                if (args.length < 2)
                    return false;
                // Simplified removal for example
                player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Feature coming soon...</red>"));
            }
        }

        return true;
    }
}
