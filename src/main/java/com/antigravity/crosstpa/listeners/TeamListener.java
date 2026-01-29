package com.antigravity.crosstpa.listeners;

import com.antigravity.crosstpa.CrossTPA;
import com.antigravity.crosstpa.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;
import java.util.HashMap;
import java.util.UUID;

public class TeamListener implements Listener {

    private final CrossTPA plugin;

    public TeamListener(CrossTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTeamManager().isTeamChatEnabled(player.getUniqueId())) {
            String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (teamName != null) {
                event.setCancelled(true);
                DataManager.TeamData data = plugin.getTeamManager().getTeamData(teamName);
                if (data != null) {
                    // String prefix = plugin.getConfig().getString("messages.prefix"); // Unused
                    String msg = "§8[§bTeam Chat§8] §f" + player.getName() + ": §7" + event.getMessage();
                    for (UUID memberUuid : data.members) {
                        Player member = Bukkit.getPlayer(memberUuid);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(msg);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            String victimTeam = plugin.getTeamManager().getPlayerTeam(victim.getUniqueId());
            String attackerTeam = plugin.getTeamManager().getPlayerTeam(attacker.getUniqueId());

            if (victimTeam != null && attackerTeam != null) {
                // Same team friendly fire
                if (victimTeam.equals(attackerTeam)) {
                    if (!plugin.getTeamManager().isFriendlyFireEnabled(victimTeam)) {
                        event.setCancelled(true);
                        attacker.sendMessage("§cFriendly fire is disabled for your team!");
                        return;
                    }
                }

                // Ally friendly fire (always disabled for now)
                DataManager.TeamData data = plugin.getTeamManager().getTeamData(attackerTeam);
                if (data.allies.contains(victimTeam)) {
                    event.setCancelled(true);
                    attacker.sendMessage("§cYou cannot hurt an ally!");
                }
            }
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            org.bukkit.entity.EntityType type = event.getEntityType();

            // Bounty/Mission Check
            if (event.getEntity() instanceof Player victim) {
                // Prevent self-kills or teammate kills from counting
                String killerTeam = plugin.getTeamManager().getPlayerTeam(killer.getUniqueId());
                String victimTeam = plugin.getTeamManager().getPlayerTeam(victim.getUniqueId());

                if (killer.equals(victim))
                    return; // No suicide farming
                if (killerTeam != null && victimTeam != null && killerTeam.equals(victimTeam))
                    return; // No teammate farming

                String victimName = victim.getName();
                plugin.getTeamManager().progressMission(killer, victimName, 1);
            }

            // Standard Mob Kill (Doesn't apply to players usually, but addTeamKill handles
            // it)
            plugin.getTeamManager().addTeamKill(killer.getUniqueId(), type);
        }
    }

    @EventHandler
    public void onCurrencyInteract(PlayerInteractEvent event) {
        if (!event.hasItem())
            return;
        ItemStack item = event.getItem();
        if (item == null)
            return;

        Action action = event.getAction();
        Player player = event.getPlayer();

        // 1. Right Click = Deposit (Eat) - ONLY ALLOW SECURE COINS
        if (action.toString().contains("RIGHT_CLICK")) {
            int value = plugin.getCoinManager().getCoinValue(item);

            if (value > 0 && plugin.getCoinManager().isValidCoin(item)) {
                event.setCancelled(true); // Stop placement

                if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) != null) {
                    plugin.getTeamManager().addMemberCoins(player, player.getUniqueId(), value);
                    item.setAmount(item.getAmount() - 1);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                    player.sendActionBar(
                            net.kyori.adventure.text.Component.text("§a+ " + value + " Coin(s) Deposited"));
                } else {
                    player.sendMessage("§cYou must be in a team to use the bank!");
                }
            }
        }
        // 2. Left Click = Convert (Compress/Decompress) - ONLY ALLOW SECURE COINS
        else if (action.toString().contains("LEFT_CLICK")) {
            if (plugin.getCoinManager().isCoinCluster(item)) {
                // Decompress: 1 Cluster Coin -> 4 Shard Coins
                item.setAmount(item.getAmount() - 1);
                HashMap<Integer, ItemStack> left = player.getInventory()
                        .addItem(plugin.getCoinManager().createCoinShard(4));
                if (!left.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));
                }
                event.setCancelled(true); // Prevent punching
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1f);
            } else if (plugin.getCoinManager().isCoinShard(item)) {
                // Compress: 4 Shard Coins -> 1 Cluster Coin
                if (item.getAmount() >= 4) {
                    item.setAmount(item.getAmount() - 4);
                    HashMap<Integer, ItemStack> left = player.getInventory()
                            .addItem(plugin.getCoinManager().createCoinCluster(1));
                    if (!left.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));
                    }
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1f, 1f);
                }
            }
        }
    }
}
