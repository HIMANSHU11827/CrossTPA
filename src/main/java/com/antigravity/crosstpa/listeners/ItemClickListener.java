package com.antigravity.crosstpa.listeners;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemClickListener implements Listener {

    private final CrossTPA plugin;

    public ItemClickListener(CrossTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if player right-clicked
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Check if item click menu is enabled
        if (!plugin.getConfig().getBoolean("features.item-menu", true)) {
            return;
        }

        // Get the configured item type
        String itemName = plugin.getConfig().getString("item-menu.item", "COMPASS");
        Material menuItem;
        try {
            menuItem = Material.valueOf(itemName.toUpperCase());
        } catch (IllegalArgumentException e) {
            menuItem = Material.COMPASS;
        }

        // Check if player is holding the menu item
        if (item.getType() != menuItem) {
            return;
        }

        // Check if item has the correct name
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = plugin.getConfig().getString("item-menu.name", "§3§lCrossTPA Menu");
        String itemDisplayName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .serialize(item.getItemMeta().displayName());

        if (!itemDisplayName.equals(displayName)) {
            return;
        }

        // Cancel the event to prevent other actions
        event.setCancelled(true);

        // Open the TPA menu
        try {
            player.performCommand("tpa");
        } catch (Exception e) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize("<red>Error opening menu!</red>"));
            plugin.getLogger().warning("Error opening TPA menu via item for " + player.getName());
        }
    }
}
