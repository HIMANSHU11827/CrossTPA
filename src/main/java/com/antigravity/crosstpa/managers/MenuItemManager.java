package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

public class MenuItemManager implements Listener {

    private final CrossTPA plugin;
    private final NamespacedKey menuKey;

    public MenuItemManager(CrossTPA plugin) {
        this.plugin = plugin;
        this.menuKey = new NamespacedKey(plugin, "crosstpa_menu");
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Give item to online players on startup/reload if enabled
        if (plugin.getConfig().getBoolean("menu-item.enabled", false)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                giveItem(player);
            }
        }
    }

    public void giveItem(Player player) {
        if (!plugin.getConfig().getBoolean("menu-item.enabled", false))
            return;

        ItemStack item = createMenuItem();
        int slot = plugin.getConfig().getInt("menu-item.slot", 8);

        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || current.getType() == Material.AIR) {
            player.getInventory().setItem(slot, item);
        } else if (!isMenuItem(current)) {
            // Find another empty slot for the menu item if preferred slot is taken
            player.getInventory().addItem(item);
        }
    }

    private ItemStack createMenuItem() {
        Material mat = Material.valueOf(plugin.getConfig().getString("menu-item.material", "COMPASS").toUpperCase());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(plugin.getConfig().getString("menu-item.display-name", "§b§lCrossTPA Control Panel")));
            List<String> lore = plugin.getConfig().getStringList("menu-item.lore");
            List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(LegacyComponentSerializer.legacySection().deserialize(line));
            }
            meta.lore(loreComponents);

            // Add NBT marker
            meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean isMenuItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        return meta.getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("menu-item.give-on-join", true)) {
            giveItem(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isMenuItem(event.getItem())) {
                event.setCancelled(true);
                event.getPlayer().performCommand("tpamenu");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.getConfig().getBoolean("menu-item.prevent-movement", true)) {
            if (isMenuItem(event.getCurrentItem()) || isMenuItem(event.getCursor())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getConfig().getBoolean("menu-item.prevent-movement", true)) {
            if (isMenuItem(event.getItemDrop().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }
}
