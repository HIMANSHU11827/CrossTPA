package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class FriendManager {

    private final CrossTPA plugin;
    private final Map<UUID, Set<UUID>> friends = new HashMap<>();
    private final Map<UUID, Set<UUID>> pendingRequests = new HashMap<>(); // Recipient -> Senders

    public FriendManager(CrossTPA plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        plugin.getDataManager().loadFriends(friends);
    }

    public void saveData() {
        plugin.getDataManager().saveFriends(friends);
    }

    public void sendFriendRequest(Player sender, Player target) {
        if (isFriends(sender.getUniqueId(), target.getUniqueId()))
            return;

        pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(sender.getUniqueId());

        String prefix = plugin.getConfig().getString("messages.prefix");
        target.sendMessage(plugin.getMiniMessage().deserialize(
                prefix + "<gray>Player <yellow>" + sender.getName() + "</yellow> sent you a friend request!</gray>"));
        sender.sendMessage(plugin.getMiniMessage().deserialize(
                prefix + "<gray>Friend request sent to <yellow>" + target.getName() + "</yellow>.</gray>"));
    }

    public void acceptFriendRequest(Player player, String senderName) {
        Player sender = Bukkit.getPlayer(senderName);
        if (sender == null)
            return;

        Set<UUID> requests = pendingRequests.get(player.getUniqueId());
        if (requests != null && requests.contains(sender.getUniqueId())) {
            requests.remove(sender.getUniqueId());

            friends.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(sender.getUniqueId());
            friends.computeIfAbsent(sender.getUniqueId(), k -> new HashSet<>()).add(player.getUniqueId());

            String prefix = plugin.getConfig().getString("messages.prefix");
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + "<green>You are now friends with " + sender.getName() + "!</green>"));
            sender.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + "<green>" + player.getName() + " accepted your friend request!</green>"));
        }
    }

    public void removeFriend(Player player, UUID friendUuid) {
        Set<UUID> playerFriends = friends.get(player.getUniqueId());
        if (playerFriends != null)
            playerFriends.remove(friendUuid);

        Set<UUID> targetFriends = friends.get(friendUuid);
        if (targetFriends != null)
            targetFriends.remove(player.getUniqueId());
    }

    public boolean isFriends(UUID u1, UUID u2) {
        return friends.getOrDefault(u1, Collections.emptySet()).contains(u2);
    }

    public Set<UUID> getFriends(UUID playerUuid) {
        return friends.getOrDefault(playerUuid, Collections.emptySet());
    }

    public Set<UUID> getPendingRequests(UUID playerUuid) {
        return pendingRequests.getOrDefault(playerUuid, Collections.emptySet());
    }
}
