package com.antigravity.crosstpa.commands;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamBaseCommand implements CommandExecutor {

    private final CrossTPA plugin;

    public TeamBaseCommand(CrossTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return true;

        String prefix = plugin.getConfig().getString("messages.prefix",
                "<gradient:#4facfe:#00f2fe>[CrossTPA]</gradient> ");

        // Check if team-base feature is enabled
        if (!plugin.getConfig().getBoolean("features.team-base", true)) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + "<red>Team base feature is currently disabled!</red>"));
            return true;
        }

        // Check if player is in a team
        String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + "<red>You are not in a team!</red>"));
            return true;
        }

        // /tpateambase set - set the base location
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            if (plugin.getTeamManager().setTeamHome(player)) {
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix + "<green>Team base set at your current location!</green>"));
            } else {
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix
                                + "<red>Failed to set base. Only leaders and co-leaders can set the team base!</red>"));
            }
            return true;
        }

        // /tpateambase - teleport to base
        Location base = plugin.getTeamManager().getTeamHome(player);
        if (base != null) {
            player.teleport(base);
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + "<green>Teleported to team base!</green>"));
        } else {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix
                            + "<red>Team base not set! Leaders can use <yellow>/tpateambase set</yellow> to set it.</red>"));
        }

        return true;
    }
}
