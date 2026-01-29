package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TeamManager {

    private final CrossTPA plugin;
    private final Map<String, DataManager.TeamData> teams = new HashMap<>();
    private final Map<UUID, Set<String>> pendingInvites = new HashMap<>();
    private final Map<String, Set<String>> pendingAllyRequests = new HashMap<>();

    public TeamManager(CrossTPA plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        plugin.getDataManager().loadTeams(teams);
    }

    public void saveData() {
        plugin.getDataManager().saveTeams(teams);
    }

    public boolean createTeam(Player owner, String name) {
        if (teams.containsKey(name))
            return false;
        if (getPlayerTeam(owner.getUniqueId()) != null)
            return false;

        Set<UUID> members = new HashSet<>();
        members.add(owner.getUniqueId());
        teams.put(name, new DataManager.TeamData(owner.getUniqueId(), members, "white"));
        return true;
    }

    public boolean deleteTeam(Player owner) {
        String teamName = getPlayerTeam(owner.getUniqueId());
        if (teamName == null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (data.owner.equals(owner.getUniqueId())) {
            teams.remove(teamName);
            // Clear alliances
            for (DataManager.TeamData other : teams.values()) {
                other.allies.remove(teamName);
            }
            return true;
        }
        return false;
    }

    public void invitePlayer(Player sender, Player target) {
        String teamName = getPlayerTeam(sender.getUniqueId());
        if (teamName == null)
            return;

        // DataManager.TeamData data = teams.get(teamName); // Unused
        if (!hasPermission(sender.getUniqueId(), teamName, "INVITE")) {
            sender.sendMessage("§cYou don't have permission to invite players.");
            return;
        }

        pendingInvites.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(teamName);

        String prefix = plugin.getConfig().getString("messages.prefix");
        target.sendMessage(plugin.getMiniMessage().deserialize(
                prefix + "<gray>You have been invited to join team <aqua>" + teamName + "</aqua>!</gray>"));
        sender.sendMessage(plugin.getMiniMessage()
                .deserialize(prefix + "<gray>Invite sent to <yellow>" + target.getName() + "</yellow>.</gray>"));
    }

    public boolean joinTeam(Player player, String teamName) {
        Set<String> invites = pendingInvites.get(player.getUniqueId());
        if (invites == null || !invites.contains(teamName))
            return false;
        if (getPlayerTeam(player.getUniqueId()) != null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (data == null)
            return false;

        data.members.add(player.getUniqueId());
        data.roles.put(player.getUniqueId(), "MEMBER");
        invites.remove(teamName);
        return true;
    }

    public void leaveTeam(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return;

        DataManager.TeamData data = teams.get(teamName);
        if (data.owner.equals(player.getUniqueId())) {
            deleteTeam(player);
        } else {
            data.members.remove(player.getUniqueId());
            data.roles.remove(player.getUniqueId());
        }
    }

    public boolean kickPlayer(Player requester, UUID targetUuid) {
        String teamName = getPlayerTeam(requester.getUniqueId());
        if (teamName == null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (!hasPermission(requester.getUniqueId(), teamName, "KICK"))
            return false;

        if (targetUuid.equals(data.owner))
            return false;

        // Cannot kick someone of same or higher rank
        int rRank = getRoleRank(data.roles.get(requester.getUniqueId()));
        int tRank = getRoleRank(data.roles.get(targetUuid));
        if (rRank <= tRank)
            return false;

        data.members.remove(targetUuid);
        data.roles.remove(targetUuid);
        return true;
    }

    public boolean promotePlayer(Player requester, UUID targetUuid) {
        String teamName = getPlayerTeam(requester.getUniqueId());
        if (teamName == null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (!hasPermission(requester.getUniqueId(), teamName, "PROMOTE"))
            return false;

        String currentRole = data.roles.getOrDefault(targetUuid, "MEMBER");
        String nextRole = getNextRole(currentRole);
        if (nextRole == null || nextRole.equals("LEADER"))
            return false; // Cannot promote to leader this way

        // Cannot promote beyond your own rank
        int rRank = getRoleRank(data.roles.get(requester.getUniqueId()));
        int nRank = getRoleRank(nextRole);
        if (rRank <= nRank)
            return false;

        data.roles.put(targetUuid, nextRole);
        return true;
    }

    public boolean demotePlayer(Player requester, UUID targetUuid) {
        String teamName = getPlayerTeam(requester.getUniqueId());
        if (teamName == null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (!hasPermission(requester.getUniqueId(), teamName, "DEMOTE"))
            return false;

        String currentRole = data.roles.getOrDefault(targetUuid, "MEMBER");
        String prevRole = getPreviousRole(currentRole);
        if (prevRole == null)
            return false;

        // Cannot demote someone of same or higher rank than you
        int rRank = getRoleRank(data.roles.get(requester.getUniqueId()));
        int tRank = getRoleRank(currentRole);
        if (rRank <= tRank)
            return false;

        data.roles.put(targetUuid, prevRole);
        return true;
    }

    public boolean transferOwnership(Player owner, UUID newOwnerUuid) {
        String teamName = getPlayerTeam(owner.getUniqueId());
        if (teamName == null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (!data.owner.equals(owner.getUniqueId()))
            return false; // Only actual owner can transfer

        if (!data.members.contains(newOwnerUuid))
            return false;

        // Swap roles
        data.roles.put(owner.getUniqueId(), "CO_LEADER");
        data.roles.put(newOwnerUuid, "LEADER");
        data.owner = newOwnerUuid;

        return true;
    }

    public void addTeamKill(UUID playerUuid, org.bukkit.entity.EntityType type) {
        String teamName = getPlayerTeam(playerUuid);
        if (teamName == null)
            return;

        // Anti-Exploit: No self-kills counting (if Player type)
        // Check isn't here, it's in Listener usually, but we can't see victim here
        // easily unless passed.
        // Wait, onKill passed type. If type is PLAYER, we rely on Listener to have
        // checked names.
        // But for Team Kills (Score), we usually want to prevent farming teammates.
        // I need to change addTeamKill signature or logic?
        // Actually, Listener calls this. I should fix Listener to block self/team
        // kills.
        // But for THIS method (TeamManager), I will add the Mission Limit check.

        Player p = Bukkit.getPlayer(playerUuid);
        if (p != null)
            progressMission(p, type, 1);
    }

    // --------------------------------------------------------------------------------
    // Currency Conversion
    // --------------------------------------------------------------------------------

    public boolean convertCurrency(Player player, String from, int amount) {
        // Shard Coin (1) <-> Cluster Coin (4)
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack reward = null;
        int costAmount = 0;

        if (from.equalsIgnoreCase("shards")) {
            // Check if player is holding valid Shard Coins
            if (plugin.getCoinManager().isCoinShard(hand) && hand.getAmount() >= 4 * amount) {
                costAmount = 4 * amount;
                reward = plugin.getCoinManager().createCoinCluster(amount);
            }
        } else if (from.equalsIgnoreCase("clusters")) {
            // Check if player is holding valid Cluster Coins
            if (plugin.getCoinManager().isCoinCluster(hand) && hand.getAmount() >= amount) {
                costAmount = amount;
                reward = plugin.getCoinManager().createCoinShard(4 * amount);
            }
        }

        if (reward != null) {
            hand.setAmount(hand.getAmount() - costAmount);
            Map<Integer, ItemStack> left = player.getInventory().addItem(reward);
            for (ItemStack drop : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
            return true;
        }

        return false;
    }

    // --------------------------------------------------------------------------------
    // Mission Board System (List)
    // --------------------------------------------------------------------------------

    public void startMission(Player player, String type) { // Server Event
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return;
        DataManager.TeamData data = teams.get(teamName);

        String missionId = type.toUpperCase();
        int goal = switch (missionId) {
            case "KILL_ANY" -> 20;
            case "KILL_ZOMBIES" -> 15;
            case "KILL_SKELETONS" -> 15;
            case "KILL_SPIDERS" -> 10;
            default -> -1;
        };

        if (goal == -1) {
            player.sendMessage("§cInvalid type.");
            return;
        }

        // reward = 20 (5 clusters)
        DataManager.MissionData m = new DataManager.MissionData(UUID.randomUUID().toString(),
                formatMissionName(missionId), missionId, goal, 0, 20, null, false);
        data.missions.add(m);

        String prefix = CrossTPA.getPlugin(CrossTPA.class).getConfig().getString("messages.prefix");
        Bukkit.broadcast(CrossTPA.getPlugin(CrossTPA.class).getMiniMessage().deserialize(
                prefix + "<red>Team " + teamName + " added event: " + m.name + " (Reward: 5 Clusters)!</red>"));
    }

    public void createCustomMission(Player player, String typeOrMob, int amount, int inputReward, String name) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return;
        DataManager.TeamData data = teams.get(teamName);

        // Limit active missions
        if (data.missions.size() >= 50) {
            player.sendMessage("§cTeam mission board is full (Max 50).");
            return;
        }

        String type = "KILL";
        String itemMat = null;
        String targetName = null;

        // Parsing Type
        if (typeOrMob.startsWith("ITEM:")) {
            type = "ITEM";
            String matName = typeOrMob.split(":")[1];
            try {
                itemMat = Material.valueOf(matName.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid material: " + matName);
                return;
            }
        } else if (typeOrMob.startsWith("PLAYER:")) {
            type = "BOUNTY";
            targetName = typeOrMob.split(":")[1];
            amount = 1; // Bounties are usually 1 kill
        } else if (typeOrMob.equalsIgnoreCase("TASK")) {
            type = "TASK";
            amount = 1; // 1 task
        } else {
            // Default Mobs
            try {
                org.bukkit.entity.EntityType et = org.bukkit.entity.EntityType.valueOf(typeOrMob.toUpperCase());
                type = et.name();
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid type. Use ITEM:Mat, PLAYER:Name, TASK, or MobType.");
                return;
            }
        }

        String role = data.roles.getOrDefault(player.getUniqueId(), "MEMBER");
        boolean isHighRank = role.equals("LEADER") || role.equals("CO_LEADER");
        int multiplier = isHighRank ? 4 : 1;
        int actualReward = inputReward * multiplier;

        int totalCost = actualReward;
        int playerBal = data.memberCoins.getOrDefault(player.getUniqueId(), 0);

        if (playerBal < totalCost) {
            player.sendMessage("§cInsufficient funds! Cost: " + totalCost + " Shards.");
            return;
        }

        data.memberCoins.put(player.getUniqueId(), playerBal - totalCost);

        DataManager.MissionData m = new DataManager.MissionData(UUID.randomUUID().toString(), name, type, amount, 0,
                actualReward, player.getUniqueId(), false, itemMat, targetName, false);
        data.missions.add(m);

        String currency = isHighRank ? "Clusters" : "Shards";
        String prefix = CrossTPA.getPlugin(CrossTPA.class).getConfig().getString("messages.prefix");
        Bukkit.broadcast(CrossTPA.getPlugin(CrossTPA.class).getMiniMessage().deserialize(prefix + "<gold>Team "
                + teamName + " posted job: " + name + " (Reward: " + inputReward + " " + currency + ")!</gold>"));
    }

    // For Mobs AND Players
    public void progressMission(Player player, org.bukkit.entity.EntityType entityType, int amount) {
        // Overload to handle just EntityType
        progressMission(player, entityType.name(), amount);
    }

    public void progressMission(Player player, String targetIdentifier, int amount) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return;
        DataManager.TeamData data = teams.get(teamName);
        if (data.missions == null || data.missions.isEmpty())
            return;

        for (DataManager.MissionData m : data.missions) {
            if (m.completed)
                continue;

            boolean matches = false;

            if ("BOUNTY".equals(m.type)) {
                // Check if targetIdentifier matches targetName (Case insensitive?)
                if (m.target != null && m.target.equalsIgnoreCase(targetIdentifier))
                    matches = true;
            } else if ("KILL_ANY".equals(m.type)) {
                matches = true; // Still assumes valid mob passed. Bounties usually distinct?
                // If I kill a player, does it count for Kill Any? Maybe.
            } else {
                if (m.type.equals(targetIdentifier))
                    matches = true;
                else if (m.type.equals("KILL_" + targetIdentifier + "S"))
                    matches = true; // Legacy
                else if (m.type.equals("KILL_ZOMBIES") && "ZOMBIE".equals(targetIdentifier))
                    matches = true;
                else if (m.type.equals("KILL_SKELETONS") && "SKELETON".equals(targetIdentifier))
                    matches = true;
                else if (m.type.equals("KILL_SPIDERS") && "SPIDER".equals(targetIdentifier))
                    matches = true;
            }

            if (matches) {
                m.progress += amount;
                if (m.progress >= m.amount) {
                    m.completed = true;
                    player.sendMessage("§aMission '" + m.name + "' completed! Type /team mission claim "
                            + m.id.substring(0, 4) + " to claim reward.");
                }
            }
        }
    }

    public boolean claimMission(Player player, String partialId) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return false;
        DataManager.TeamData data = teams.get(teamName);

        DataManager.MissionData target = null;
        for (DataManager.MissionData m : data.missions) {
            if (m.id.startsWith(partialId)) {
                target = m;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("§cMission not found.");
            return false;
        }

        if ("TASK".equals(target.type)) {
            if (target.completed) {
                // Already paid out? No, if completed=true in TASK, it means Approved.
                // Wait, logic check:
                // Normal missions: completed=true means "Goal Met", waiting for "Claim".
                // Task: No auto-progress. "Claim" means "I did it".
                // Once "Claimed" (Verified), it should allow Payout.

                // My Flow:
                // 1. User does task.
                // 2. User types /claim.
                // 3. Status -> Request Review.
                // 4. Creator types /approve.
                // 5. User gets paid. Mission Removed.

                if (target.requestReview) {
                    player.sendMessage("§eMission is already awaiting review by "
                            + (target.creator != null ? Bukkit.getOfflinePlayer(target.creator).getName() : "Server")
                            + ".");
                    return false;
                }

                // If already completed (approved)?
                if (target.completed) {
                    // Pay out immediately
                } else {
                    // Mark for review
                    target.requestReview = true;
                    player.sendMessage("§aTask submitted for review! The creator must approve it.");

                    if (target.creator != null) {
                        Player c = Bukkit.getPlayer(target.creator);
                        if (c != null)
                            c.sendMessage("§e" + player.getName() + " requests approval for task '" + target.name
                                    + "' (" + target.id.substring(0, 4) + "). Type /team mission approve "
                                    + target.id.substring(0, 4) + " to pay.");
                    }
                    return true;
                }
            }
        } else if ("ITEM".equals(target.type)) {
            // ... (Item logic same as before) ...
            Material mat = Material.getMaterial(target.itemMaterial);
            if (mat == null)
                return false;
            ItemStack req = new ItemStack(mat, target.amount);
            if (!player.getInventory().containsAtLeast(req, target.amount)) {
                player.sendMessage("§cYou do not have " + target.amount + " " + target.itemMaterial + "!");
                return false;
            }
            player.getInventory().removeItem(req);
            if (target.creator != null) {
                data.pendingItems.computeIfAbsent(target.creator, k -> new ArrayList<>()).add(req);
            }
            target.completed = true;
        } else {
            // KILL / BOUNTY
            if (!target.completed) {
                player.sendMessage("§cMission not completed yet (" + target.progress + "/" + target.amount + ").");
                return false;
            }
        }

        // Payout
        data.memberCoins.put(player.getUniqueId(),
                data.memberCoins.getOrDefault(player.getUniqueId(), 0) + target.reward);
        data.missions.remove(target);

        if (target.creator == null) {
            data.teamKills += 50;
            player.sendMessage("§dClaimed " + target.reward + " Shards and +50 Team Score!");
        } else {
            player.sendMessage("§aClaimed " + target.reward + " Shards!");
        }
        return true;
    }

    public boolean approveMission(Player creator, String partialId) {
        // Creator approves the task. Payout happens?
        // Wait, current claimMission logic handles payment.
        // If I approve, do I pay instantly or mark as completed so they can claim?
        // User said: "he says claim -> creator sees -> he gives money".
        // This implies Creator triggers the payment.

        // Let's make Approve trigger payment to the *Applicant*.
        // Issue: I didn't store WHO applied.
        // I need to store `applicant` in MissionData? Or allow ANYONE to claim once
        // approved?
        // Usually, the person who did it claims it.
        // If I make Approve satisfy the condition, then the person must claim AGAIN?
        // That's tedious.
        // Better: MissionData should store `applicant` UUID when status is Review.
        // But I don't have that field yet.
        // I can add it, or just Payout to the person who Claimed (Store it in memory?
        // No, persistence).

        // Alternative: Approve marks `completed = true` and `requestReview = false`.
        // Then the user types `/claim` again to get money.
        // "Mission 'Build House' approved! Type /claim to get money."

        String teamName = getPlayerTeam(creator.getUniqueId());
        if (teamName == null)
            return false;
        DataManager.TeamData data = teams.get(teamName);

        DataManager.MissionData target = null;
        for (DataManager.MissionData m : data.missions) {
            if (m.id.startsWith(partialId)) {
                target = m;
                break;
            }
        }

        if (target == null) {
            creator.sendMessage("§cMission not found.");
            return false;
        }

        if (!target.creator.equals(creator.getUniqueId()) && !data.roles.get(creator.getUniqueId()).equals("LEADER")) {
            creator.sendMessage("§cYou did not create this mission.");
            return false;
        }

        if (!target.requestReview) {
            creator.sendMessage("§cThis mission is not pending review.");
            return false;
        }

        target.requestReview = false;
        target.completed = true;

        creator.sendMessage("§aMission approved! The member can now claim the reward.");
        // Broadcast
        for (UUID mem : data.members) {
            Player p = Bukkit.getPlayer(mem);
            if (p != null)
                p.sendMessage("§aTask '" + target.name + "' approved! Completer can now /claim.");
        }

        return true;
    }

    public List<String> getMissionList(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return new ArrayList<>();
        DataManager.TeamData data = teams.get(teamName);
        List<String> lines = new ArrayList<>();
        if (data.missions == null || data.missions.isEmpty()) {
            lines.add("§7No active missions.");
            return lines;
        }

        for (DataManager.MissionData m : data.missions) {
            String status;
            if ("ITEM".equals(m.type)) {
                status = "§e" + m.itemMaterial + " x" + m.amount;
            } else if ("TASK".equals(m.type)) {
                if (m.completed)
                    status = "§a[APPROVED-CLAIM]";
                else if (m.requestReview)
                    status = "§6[REVIEWING]";
                else
                    status = "§7[OPEN]";
            } else {
                status = m.completed ? "§a[CLAIM]" : "§e" + m.progress + "/" + m.amount;
            }
            lines.add("§6" + m.id.substring(0, 4) + " §f" + m.name + " §7(" + m.type + ") - " + status + " §7Reward: "
                    + m.reward);
        }
        return lines;
    }

    private String formatMissionName(String id) {
        return switch (id) {
            case "KILL_ANY" -> "Kill 20 Mobs";
            case "KILL_ZOMBIES" -> "Kill 15 Zombies";
            case "KILL_SKELETONS" -> "Kill 15 Skeletons";
            case "KILL_SPIDERS" -> "Kill 10 Spiders";
            default -> id;
        };
    }

    public boolean transferMemberCoins(Player sender, UUID receiverUuid, int amount) {
        String teamName = getPlayerTeam(sender.getUniqueId());
        if (teamName == null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (!data.members.contains(receiverUuid))
            return false;

        int senderBal = data.memberCoins.getOrDefault(sender.getUniqueId(), 0);
        if (senderBal < amount)
            return false;

        data.memberCoins.put(sender.getUniqueId(), senderBal - amount);
        data.memberCoins.put(receiverUuid, data.memberCoins.getOrDefault(receiverUuid, 0) + amount);
        return true;
    }

    public boolean breakAlliance(String team1, String team2) {
        DataManager.TeamData data1 = teams.get(team1);
        DataManager.TeamData data2 = teams.get(team2);

        if (data1 == null || data2 == null)
            return false;

        if (data1.allies.contains(team2)) {
            data1.allies.remove(team2);
            data2.allies.remove(team1); // Mutual break
            return true;
        }
        return false;
    }

    public boolean collectPendingItems(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return false;
        DataManager.TeamData data = teams.get(teamName);
        if (data == null)
            return false;

        List<ItemStack> items = data.pendingItems.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) {
            return false;
        }

        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                player.sendMessage("§cInventory full! Some items dropped.");
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
        return true;
    }

    public String getMissionInfo(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return "None";
        DataManager.TeamData data = teams.get(teamName);
        if (data.missions == null || data.missions.isEmpty())
            return "No active missions.";

        StringBuilder sb = new StringBuilder("§eActive Missions:");
        int count = 0;
        for (DataManager.MissionData m : data.missions) {
            if (m.completed)
                continue;
            if (count >= 3) {
                sb.append("\n§7...and more");
                break;
            }
            int percent = (int) ((m.progress / (double) m.amount) * 100);
            sb.append("\n§6- ").append(m.name).append(" §7(").append(m.progress).append("/").append(m.amount)
                    .append(") §8[").append(percent).append("%]");
            count++;
        }

        if (count == 0)
            return "No active missions.";
        return sb.toString();
    }

    public List<String> getAvailableMissions() {
        List<String> list = new ArrayList<>();
        list.add("§6KILL_ANY §7- Kill 20 mobs of any kind.");
        list.add("§6KILL_ZOMBIES §7- Kill 15 Zombies.");
        list.add("§6KILL_SKELETONS §7- Kill 15 Skeletons.");
        list.add("§6KILL_SPIDERS §7- Kill 10 Spiders.");
        return list;
    }

    public void addMemberCoins(Player giver, UUID receiverUuid, int amount) {
        String teamName = getPlayerTeam(giver.getUniqueId());
        if (teamName == null)
            return;

        DataManager.TeamData data = teams.get(teamName);
        if (!data.members.contains(receiverUuid))
            return;

        data.memberCoins.put(receiverUuid, data.memberCoins.getOrDefault(receiverUuid, 0) + amount);
    }

    public boolean withdrawMemberCoins(String teamName, UUID playerUuid, int amount) {
        DataManager.TeamData data = teams.get(teamName);
        if (data == null)
            return false;

        int current = data.memberCoins.getOrDefault(playerUuid, 0);
        if (current >= amount) {
            data.memberCoins.put(playerUuid, current - amount);
            return true;
        }
        return false;
    }

    public int getMemberCoins(UUID playerUuid) {
        String teamName = getPlayerTeam(playerUuid);
        if (teamName == null)
            return 0;

        DataManager.TeamData data = teams.get(teamName);
        return data.memberCoins.getOrDefault(playerUuid, 0);
    }

    private final Set<UUID> teamChatEnabled = new HashSet<>();

    public boolean toggleTeamChat(Player player) {
        if (teamChatEnabled.contains(player.getUniqueId())) {
            teamChatEnabled.remove(player.getUniqueId());
            return false;
        } else {
            teamChatEnabled.add(player.getUniqueId());
            return true;
        }
    }

    public boolean isTeamChatEnabled(UUID uuid) {
        return teamChatEnabled.contains(uuid);
    }

    public boolean toggleFriendlyFire(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return false;

        DataManager.TeamData data = teams.get(teamName);
        if (!hasPermission(player.getUniqueId(), teamName, "COLOR"))
            return false; // reuse CO_LEADER+ permission

        data.friendlyFire = !data.friendlyFire;
        return data.friendlyFire;
    }

    public boolean isFriendlyFireEnabled(String teamName) {
        DataManager.TeamData data = teams.get(teamName);
        return data != null && data.friendlyFire;
    }

    public boolean renameTeam(Player player, String newName) {
        String oldName = getPlayerTeam(player.getUniqueId());
        if (oldName == null)
            return false;

        DataManager.TeamData data = teams.get(oldName);
        if (!data.owner.equals(player.getUniqueId()))
            return false; // Only owner

        if (teams.containsKey(newName))
            return false; // Name taken

        // Update data structure
        teams.remove(oldName);
        teams.put(newName, data);

        // Update allies references
        for (DataManager.TeamData otherTeam : teams.values()) {
            if (otherTeam.allies.contains(oldName)) {
                otherTeam.allies.remove(oldName);
                otherTeam.allies.add(newName);
            }
        }

        // Update pending invites
        // (This is a simplified approach, ideally we scan all invites but for now we'll
        // just clear pending invites for this team to avoid broken links)
        pendingInvites.values().forEach(set -> {
            if (set.contains(oldName)) {
                set.remove(oldName);
                set.add(newName);
            }
        });

        return true;
    }

    public boolean setTeamHome(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return false;

        if (!hasPermission(player.getUniqueId(), teamName, "SETHOME"))
            return false; // Default: CO_LEADER+

        DataManager.TeamData data = teams.get(teamName);
        data.home = player.getLocation();
        return true;
    }

    public Location getTeamHome(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return null;

        DataManager.TeamData data = teams.get(teamName);
        return data.home;
    }

    public void setTeamColor(Player owner, String color) {
        String teamName = getPlayerTeam(owner.getUniqueId());
        if (teamName == null)
            return;

        DataManager.TeamData data = teams.get(teamName);
        if (data.owner.equals(owner.getUniqueId())) {
            data.color = color;
        }
    }

    public void sendAllyRequest(Player player, String targetTeamName) {
        String myTeam = getPlayerTeam(player.getUniqueId());
        if (myTeam == null || !hasPermission(player.getUniqueId(), myTeam, "ALLY"))
            return;

        if (!teams.containsKey(targetTeamName))
            return;
        if (myTeam.equals(targetTeamName))
            return;

        DataManager.TeamData myData = teams.get(myTeam);
        if (myData.allies.contains(targetTeamName))
            return;

        pendingAllyRequests.computeIfAbsent(targetTeamName, k -> new HashSet<>()).add(myTeam);

        Player targetOwner = Bukkit.getPlayer(teams.get(targetTeamName).owner);
        if (targetOwner != null) {
            targetOwner.sendMessage("§dTeam §f" + myTeam + " §dwants to be your Ally! §f/team ally accept " + myTeam);
        }
        player.sendMessage("§dAlly request sent to §f" + targetTeamName);
    }

    public void acceptAllyRequest(Player player, String fromTeamName) {
        String myTeam = getPlayerTeam(player.getUniqueId());
        if (myTeam == null || !hasPermission(player.getUniqueId(), myTeam, "ALLY"))
            return;

        Set<String> requests = pendingAllyRequests.get(myTeam);
        if (requests != null && requests.contains(fromTeamName)) {
            requests.remove(fromTeamName);
            teams.get(myTeam).allies.add(fromTeamName);
            teams.get(fromTeamName).allies.add(myTeam);

            player.sendMessage("§dYou are now Allied with §f" + fromTeamName);
            Player otherOwner = Bukkit.getPlayer(teams.get(fromTeamName).owner);
            if (otherOwner != null)
                otherOwner.sendMessage("§dYour ally request to §f" + myTeam + " §dwas accepted!");
        }
    }

    public boolean hasPermission(UUID playerUuid, String teamName, String action) {
        DataManager.TeamData data = teams.get(teamName);
        if (data == null)
            return false;
        if (data.owner.equals(playerUuid))
            return true;

        String role = data.roles.getOrDefault(playerUuid, "MEMBER");
        int rank = getRoleRank(role);

        return switch (action) {
            case "INVITE", "KICK" -> rank >= 1; // ELDER+
            case "PROMOTE", "DEMOTE", "ALLY", "COLOR", "SETHOME", "MISSION_START" -> rank >= 2; // CO_LEADER+
            case "MISSION_APPROVE" -> rank >= 3 || data.owner.equals(playerUuid); // LEADER+
            default -> false;
        };
    }

    public int getRoleRank(String role) {
        return switch (role) {
            case "LEADER" -> 3;
            case "CO_LEADER" -> 2;
            case "ELDER" -> 1;
            default -> 0; // MEMBER
        };
    }

    private String getNextRole(String role) {
        return switch (role) {
            case "MEMBER" -> "ELDER";
            case "ELDER" -> "CO_LEADER";
            case "CO_LEADER" -> "LEADER";
            default -> null;
        };
    }

    private String getPreviousRole(String role) {
        return switch (role) {
            case "ELDER" -> "MEMBER";
            case "CO_LEADER" -> "ELDER";
            case "LEADER" -> "CO_LEADER";
            default -> null;
        };
    }

    public String getPlayerTeam(UUID uuid) {
        for (Map.Entry<String, DataManager.TeamData> entry : teams.entrySet()) {
            if (entry.getValue().members.contains(uuid))
                return entry.getKey();
        }
        return null;
    }

    public DataManager.TeamData getTeamData(String name) {
        return teams.get(name);
    }

    public Map<String, DataManager.TeamData> getAllTeams() {
        return teams;
    }
}
