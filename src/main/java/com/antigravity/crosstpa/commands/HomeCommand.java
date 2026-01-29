package com.antigravity.crosstpa.commands;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomeCommand implements CommandExecutor {

    private final CrossTPA plugin;

    public HomeCommand(CrossTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return true;

        String prefix = plugin.getConfig().getString("messages.prefix");

        if (label.equalsIgnoreCase("sethome")) {
            String homeName = args.length > 0 ? args[0] : "home";
            if (plugin.getHomeManager().setHome(player, homeName)) {
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix + "<green>Home <yellow>" + homeName + "</yellow> has been set!</green>"));
            } else {
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix + "<red>You have reached the maximum limit of homes!</red>"));
            }
            return true;
        }

        if (label.equalsIgnoreCase("delhome")) {
            String homeName = args.length > 0 ? args[0] : "home";
            if (plugin.getHomeManager().deleteHome(player, homeName)) {
                player.sendMessage(plugin.getMiniMessage().deserialize(
                        prefix + "<green>Home <yellow>" + homeName + "</yellow> has been deleted.</green>"));
            } else {
                player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Home not found!</red>"));
            }
            return true;
        }

        if (label.equalsIgnoreCase("home")) {
            String homeName = args.length > 0 ? args[0] : "home";
            Location loc = plugin.getHomeManager().getHome(player, homeName);
            if (loc != null) {
                // We use the teleport logic from request manager to a fake target or just
                // teleport
                player.teleport(loc);
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix + "<green>Teleported to home <yellow>" + homeName + "</yellow>.</green>"));
            } else {
                player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Home not found!</red>"));
            }
            return true;
        }

        return true;
    }
}
