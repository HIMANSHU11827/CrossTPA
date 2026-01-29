package com.antigravity.crosstpa.commands;

import com.antigravity.crosstpa.CrossTPA;
import com.antigravity.crosstpa.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TeamCommand implements CommandExecutor {

    private final CrossTPA plugin;

    public TeamCommand(CrossTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return true;

        String prefix = plugin.getConfig().getString("messages.prefix");

        if (args.length == 0) {
            sendHelp(player, prefix);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team create <name></red>"));
                    return true;
                }
                if (plugin.getTeamManager().createTeam(player, args[1])) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Team <aqua>" + args[1] + "</aqua> created!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix
                            + "<red>Failed to create team. Name might be taken or you are already in a team.</red>"));
                }
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team invite <player></red>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Player not found.</red>"));
                    return true;
                }
                plugin.getTeamManager().invitePlayer(player, target);
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team accept <name></red>"));
                    return true;
                }
                if (plugin.getTeamManager().joinTeam(player, args[1])) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Joined team " + args[1] + "!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>No pending invite found or already in a team.</red>"));
                }
            }
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team kick <player></red>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (plugin.getTeamManager().kickPlayer(player, target.getUniqueId())) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Player " + args[1] + " has been kicked.</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Failed to kick player. Check ranks/permissions.</red>"));
                }
            }
            case "promote" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team promote <player></red>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (plugin.getTeamManager().promotePlayer(player, target.getUniqueId())) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Player " + args[1] + " has been promoted.</green>"));
                } else {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Failed to promote player.</red>"));
                }
            }
            case "demote" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team demote <player></red>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (plugin.getTeamManager().demotePlayer(player, target.getUniqueId())) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Player " + args[1] + " has been demoted.</green>"));
                } else {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Failed to demote player.</red>"));
                }
            }
            case "ally" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Usage: /team ally add/accept <team></red>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("accept")) {
                    if (args.length < 3)
                        return true;
                    plugin.getTeamManager().acceptAllyRequest(player, args[2]);
                } else {
                    plugin.getTeamManager().sendAllyRequest(player, args[1]);
                }
            }
            case "leave" -> {
                plugin.getTeamManager().leaveTeam(player);
                player.sendMessage(
                        plugin.getMiniMessage().deserialize(prefix + "<gray>You have left your team.</gray>"));
            }
            case "color" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team color <color></red>"));
                    return true;
                }
                plugin.getTeamManager().setTeamColor(player, args[1]);
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix + "<green>Team color updated to " + args[1] + ".</green>"));
            }
            case "transfer" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team transfer <player></red>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (plugin.getTeamManager().transferOwnership(player, target.getUniqueId())) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Ownership transferred to " + args[1] + ".</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Failed to transfer ownership. Are you the leader?</red>"));
                }
            }
            case "rename" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team rename <newName></red>"));
                    return true;
                }
                String newName = args[1];
                if (plugin.getTeamManager().renameTeam(player, newName)) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Team renamed to " + newName + "!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Failed to rename team. Name taken or not owner?</red>"));
                }
            }
            case "sethome" -> {
                if (plugin.getTeamManager().setTeamHome(player)) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<green>Team home set!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Failed to set home. Permission denied?</red>"));
                }
            }
            case "home" -> {
                Location home = plugin.getTeamManager().getTeamHome(player);
                if (home != null) {
                    if (!plugin.getRequestManager().isLocationSafe(home)) {
                        player.sendMessage(plugin.getMiniMessage()
                                .deserialize(prefix + "<red>Team home is currently in a dangerous location!</red>"));
                        return true;
                    }
                    player.teleport(home);
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<green>Teleported to team home!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Team home not set!</red>"));
                }
            }
            case "setbase" -> {
                if (!plugin.getConfig().getBoolean("features.team-base", true)) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Team base feature is disabled!</red>"));
                    return true;
                }
                if (plugin.getTeamManager().setTeamHome(player)) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<green>Team base set!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Failed to set base. Permission denied?</red>"));
                }
            }
            case "base" -> {
                if (!plugin.getConfig().getBoolean("features.team-base", true)) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Team base feature is disabled!</red>"));
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("set")) {
                    // /team base set
                    if (plugin.getTeamManager().setTeamHome(player)) {
                        player.sendMessage(
                                plugin.getMiniMessage().deserialize(prefix + "<green>Team base set!</green>"));
                    } else {
                        player.sendMessage(plugin.getMiniMessage()
                                .deserialize(prefix + "<red>Failed to set base. Permission denied?</red>"));
                    }
                } else {
                    // /team base (teleport)
                    Location home = plugin.getTeamManager().getTeamHome(player);
                    if (home != null) {
                        if (!plugin.getRequestManager().isLocationSafe(home)) {
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(
                                            prefix + "<red>Team base is currently in a dangerous location!</red>"));
                            return true;
                        }
                        player.teleport(home);
                        player.sendMessage(
                                plugin.getMiniMessage()
                                        .deserialize(prefix + "<green>Teleported to team base!</green>"));
                    } else {
                        player.sendMessage(
                                plugin.getMiniMessage().deserialize(prefix + "<red>Team base not set!</red>"));
                    }
                }
            }
            case "coins" -> {
                int balance = plugin.getTeamManager().getMemberCoins(player.getUniqueId());
                player.sendMessage(
                        plugin.getMiniMessage().deserialize(prefix + "<yellow>Your Team Coins: <gold>" + balance));
            }
            case "award" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Usage: /team award <player> <amount></red>"));
                    return true;
                }

                String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (teamName == null
                        || !plugin.getTeamManager().hasPermission(player.getUniqueId(), teamName, "PROMOTE")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>You must be a Co-Leader+ to award coins!</red>"));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    int amount = Integer.parseInt(args[2]);
                    plugin.getTeamManager().addMemberCoins(player, target.getUniqueId(), amount);
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Awarded " + amount + " coins to " + args[1] + ".</green>"));
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Invalid amount.</red>"));
                }
            }
            case "pay" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Usage: /team pay <player> <amount></red>"));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        player.sendMessage("§cAmount must be positive.");
                        return true;
                    }
                    if (plugin.getTeamManager().transferMemberCoins(player, target.getUniqueId(), amount)) {
                        player.sendMessage(plugin.getMiniMessage().deserialize(
                                prefix + "<green>Sent " + amount + " Amethyst Shards to " + args[1] + ".</green>"));
                        if (target.isOnline()) {
                            target.getPlayer()
                                    .sendMessage(plugin.getMiniMessage().deserialize(prefix + "<green>Received "
                                            + amount + " Amethyst Shards from " + player.getName() + ".</green>"));
                        }
                    } else {
                        player.sendMessage(plugin.getMiniMessage().deserialize(
                                prefix + "<red>Transaction failed. Insufficient funds or player not on team?</red>"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number.");
                }
            }
            case "deposit" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();

                // Check if it's a valid secure coin
                if (plugin.getCoinManager().isCoinShard(hand)) {
                    int amount = hand.getAmount();
                    int value = plugin.getCoinManager().getTotalValue(hand);
                    plugin.getTeamManager().addMemberCoins(player, player.getUniqueId(), value);
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<green>Deposited " + amount
                            + " Shard Coin(s) (+ " + value + " coins).</green>"));
                } else if (plugin.getCoinManager().isCoinCluster(hand)) {
                    int amount = hand.getAmount();
                    int value = plugin.getCoinManager().getTotalValue(hand);
                    plugin.getTeamManager().addMemberCoins(player, player.getUniqueId(), value);
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<green>Deposited " + amount
                            + " Cluster Coin(s) (+ " + value + " coins).</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage().deserialize(
                            prefix + "<red>You must hold a valid coin (Shard Coin or Cluster Coin) to deposit!</red>"));
                    player.sendMessage(plugin.getMiniMessage().deserialize(
                            prefix + "<yellow>Regular amethyst cannot be used. Only official coins from /team withdraw!</yellow>"));
                }
            }
            case "withdraw" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Usage: /team withdraw <amount> [cluster]</red>"));
                    return true;
                }
                String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (teamName == null)
                    return true;

                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        player.sendMessage("§cAmount must be positive.");
                        return true;
                    }

                    boolean asCluster = args.length > 2 && args[2].equalsIgnoreCase("cluster");
                    int cost = asCluster ? amount * 4 : amount;
                    String name = asCluster ? "Cluster Coin(s)" : "Shard Coin(s)";

                    if (plugin.getTeamManager().withdrawMemberCoins(teamName, player.getUniqueId(), cost)) {
                        // Create secure coins with NBT data (cannot be counterfeited)
                        ItemStack items = asCluster ? plugin.getCoinManager().createCoinCluster(amount)
                                : plugin.getCoinManager().createCoinShard(amount);
                        player.getInventory().addItem(items);
                        player.sendMessage(plugin.getMiniMessage().deserialize(
                                prefix + "<green>Withdrew " + amount + " " + name + " (Cost: " + cost + ")!</green>"));
                    } else {
                        player.sendMessage(plugin.getMiniMessage()
                                .deserialize(prefix + "<red>Insufficient funds. You need " + cost + " coins.</red>"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Invalid amount.</red>"));
                }
            }
            case "convert" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Usage: /team convert <shards|clusters> <amount></red>"));
                    return true;
                }
                String from = args[1]; // shards or clusters
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        player.sendMessage("§cAmount must be positive.");
                        return true;
                    }
                    if (plugin.getTeamManager().convertCurrency(player, from, amount)) {
                        player.sendMessage(
                                plugin.getMiniMessage().deserialize(prefix + "<green>Conversion successful!</green>"));
                    } else {
                        player.sendMessage(plugin.getMiniMessage()
                                .deserialize(prefix + "<red>Conversion failed. Check your inventory.</red>"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid amount.");
                }
            }
            case "collect" -> {
                plugin.getTeamManager().collectPendingItems(player);
            }
            case "mission" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Usage: /team mission <list|start|create|claim> ...</red>"));
                    return true;
                }

                String missionAction = args[1].toLowerCase();
                String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (teamName == null) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>You must be in a team!</red>"));
                    return true;
                }

                switch (missionAction) {
                    case "list" -> {
                        player.sendMessage(
                                plugin.getMiniMessage().deserialize(prefix + "<gray>Team Missions:</gray>"));
                        for (String m : plugin.getTeamManager().getMissionList(player)) {
                            player.sendMessage(plugin.getMiniMessage().deserialize(m));
                        }
                    }
                    case "claim" -> {
                        if (args.length < 3) {
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(prefix + "<red>Usage: /team mission claim <id></red>"));
                            return true;
                        }
                        plugin.getTeamManager().claimMission(player, args[2]);
                    }
                    case "approve" -> {
                        if (args.length < 3) {
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(prefix + "<red>Usage: /team mission approve <id></red>"));
                            return true;
                        }
                        if (!plugin.getTeamManager().hasPermission(player.getUniqueId(), teamName, "MISSION_APPROVE")) {
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(prefix + "<red>Only the Leader can approve missions!</red>"));
                            return true;
                        }
                        plugin.getTeamManager().approveMission(player, args[2]);
                    }
                    case "start" -> {
                        if (args.length < 3) {
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(prefix + "<red>Usage: /team mission start <type></red>"));
                            return true;
                        }
                        if (!plugin.getTeamManager().hasPermission(player.getUniqueId(), teamName, "MISSION_START")) {
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(prefix + "<red>You must be Elder+ to start a mission!</red>"));
                            return true;
                        }
                        String type = args[2];
                        plugin.getTeamManager().startMission(player, type);
                    }
                    case "create" -> {
                        if (!plugin.getTeamManager().hasPermission(player.getUniqueId(), teamName, "MISSION_APPROVE")) {
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(
                                            prefix + "<red>You must be Co-Leader+ to create custom missions!</red>"));
                            return true;
                        }
                        if (args.length < 6) {
                            player.sendMessage(plugin.getMiniMessage().deserialize(prefix
                                    + "<red>Usage: /team mission create <ITEM:Mat|Mob> <amount> <reward> <name...></red>"));
                            return true;
                        }
                        String type = args[2];
                        int amount;
                        int reward;
                        try {
                            amount = Integer.parseInt(args[3]);
                            reward = Integer.parseInt(args[4]);
                            if (amount <= 0 || reward < 0)
                                throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid number for amount or reward.");
                            return true;
                        }

                        StringBuilder nameBuilder = new StringBuilder();
                        for (int i = 5; i < args.length; i++) {
                            nameBuilder.append(args[i]).append(" ");
                        }
                        String name = nameBuilder.toString().trim();

                        plugin.getTeamManager().createCustomMission(player, type, amount, reward, name);
                    }
                    default -> player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Unknown mission command.</red>"));
                }
            }
            case "unally" -> {
                if (args.length < 2) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<red>Usage: /team unally <teamName></red>"));
                    return true;
                }
                String myTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (myTeam == null || !plugin.getTeamManager().hasPermission(player.getUniqueId(), myTeam, "ALLY")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>You must be Co-Leader+ to manage alliances!</red>"));
                    return true;
                }
                String targetTeam = args[1];
                if (plugin.getTeamManager().breakAlliance(myTeam, targetTeam)) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>Alliance with " + targetTeam + " broken.</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Failed to break alliance. Are you even allies?</red>"));
                }
            }
            case "disband" -> {
                if (plugin.getTeamManager().deleteTeam(player)) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<green>Team disbanded!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Failed to disband team. You must the leader.</red>"));
                }
            }
            case "chat" -> {
                boolean enabled = plugin.getTeamManager().toggleTeamChat(player);
                if (enabled) {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<green>Team chat enabled!</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<red>Team chat disabled!</red>"));
                }
            }
            case "pvp" -> {
                boolean pvp = plugin.getTeamManager().toggleFriendlyFire(player);
                if (pvp) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>Friendly fire ENABLED! Be careful!</red>"));
                } else {
                    player.sendMessage(
                            plugin.getMiniMessage().deserialize(prefix + "<green>Friendly fire disabled.</green>"));
                }
            }
            case "info" -> {
                String teamName = args.length > 1 ? args[1]
                        : plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (teamName == null) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<red>You are not in a team and didn't specify one.</red>"));
                    return true;
                }
                sendTeamInfo(player, teamName, prefix);
            }
            default -> sendHelp(player, prefix);
        }

        return true;

    }

    private void sendHelp(Player player, String prefix) {
        player.sendMessage(plugin.getMiniMessage().deserialize(prefix + "<gray>Team Commands:</gray>"));
        player.sendMessage("§b/team create <name> §7- Create a team");
        player.sendMessage("§b/team info [name] §7- Show team details");
        player.sendMessage("§b/team rename <name> §7- Rename team");
        player.sendMessage("§b/team home §7- Teleport to HQ");
        player.sendMessage("§b/team sethome §7- Set HQ location");
        player.sendMessage("§b/team base §7- Teleport to team base");
        player.sendMessage("§b/team base set §7- Set base location (Leader/Co-Leader)");
        player.sendMessage("§b/team setbase §7- Set base location (Leader/Co-Leader)");
        player.sendMessage("§b/team coins §7- Check balance");
        player.sendMessage("§b/team pay <player> <amount> §7- Transfer shards");
        player.sendMessage("§b/team convert <shards|clusters> <amount> §7- Convert currency");
        player.sendMessage("§b/team deposit §7- Deposit shards/clusters from inventory");
        player.sendMessage("§b/team withdraw <amount> [cluster] §7- Get physical shards or clusters");
        player.sendMessage("§b/team mission <list|start|create|claim> §7- Manage missions");
        player.sendMessage("§b/team mission approve <id> §7- Approve task missions (Creators)");
        player.sendMessage("§b/team collect §7- Collect mission items (Creators)");
        player.sendMessage("§b/team promote/demote <player> §7- Change ranks");
        player.sendMessage("§b/team transfer <player> §7- Transfer ownership");
        player.sendMessage("§b/team color <color> §7- Change color");
        player.sendMessage("§b/team chat §7- Toggle team chat");
        player.sendMessage("§b/team pvp §7- Toggle friendly fire");
        player.sendMessage("§b/team disband §7- Delete your team");
        player.sendMessage("§b/team ally <name> §7- Send ally request");
        player.sendMessage("§b/team leave §7- Leave your team");
    }

    private void sendTeamInfo(Player player, String teamName, String prefix) {
        DataManager.TeamData data = plugin.getTeamManager().getTeamData(teamName);
        if (data == null) {
            player.sendMessage("§cTeam not found.");
            return;
        }

        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§eTeam: §f" + teamName);
        player.sendMessage("§eColor: §f" + data.color);
        player.sendMessage("§eKills: §c" + data.teamKills + " ⚔");
        player.sendMessage("§eLeader: §f" + Bukkit.getOfflinePlayer(data.owner).getName());
        player.sendMessage("§eMembers: §7(" + data.members.size() + ")");

        StringBuilder members = new StringBuilder();
        for (UUID uuid : data.members) {
            String role = data.roles.getOrDefault(uuid, "MEMBER");
            String color = role.equals("LEADER") ? "§4"
                    : (role.equals("CO_LEADER") ? "§c" : (role.equals("ELDER") ? "§e" : "§7"));
            members.append(color).append(Bukkit.getOfflinePlayer(uuid).getName()).append("§r, ");
        }
        player.sendMessage(members.length() > 2 ? members.substring(0, members.length() - 2) : "None");

        player.sendMessage("§eAllies: §d" + String.join(", ", data.allies));
        player.sendMessage("§8§m----------------------------------");
    }
}
