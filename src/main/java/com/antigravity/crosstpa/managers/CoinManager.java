package com.antigravity.crosstpa.managers;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import com.antigravity.crosstpa.CrossTPA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages secure coin items with NBT verification to prevent counterfeiting
 */
public class CoinManager {

    private final NamespacedKey coinKey;
    private final NamespacedKey coinValueKey;

    public CoinManager(CrossTPA plugin) {
        this.coinKey = new NamespacedKey(plugin, "crosstpa_coin");
        this.coinValueKey = new NamespacedKey(plugin, "coin_value");
    }

    /**
     * Create a secure coin shard (worth 1 coin)
     */
    public ItemStack createCoinShard(int amount) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Add NBT data to mark as legitimate coin
            meta.getPersistentDataContainer().set(coinKey, PersistentDataType.STRING, "SHARD");
            meta.getPersistentDataContainer().set(coinValueKey, PersistentDataType.INTEGER, 1);

            // Add enchantment glow
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Set display name and lore using modern Component API
            meta.displayName(Component.text("Shard Coin")
                    .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Value: 1 Coin")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Official CrossTPA Currency")
                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cannot be counterfeited")
                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a secure coin cluster (worth 4 coins)
     */
    public ItemStack createCoinCluster(int amount) {
        ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Add NBT data to mark as legitimate coin
            meta.getPersistentDataContainer().set(coinKey, PersistentDataType.STRING, "CLUSTER");
            meta.getPersistentDataContainer().set(coinValueKey, PersistentDataType.INTEGER, 4);

            // Add enchantment glow
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Set display name and lore using modern Component API
            meta.displayName(Component.text("Cluster Coin")
                    .color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Value: 4 Coins")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Official CrossTPA Currency")
                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cannot be counterfeited")
                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Check if an item is a legitimate coin shard
     */
    public boolean isCoinShard(ItemStack item) {
        if (item == null || item.getType() != Material.AMETHYST_SHARD)
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        // Check for NBT marker
        if (!meta.getPersistentDataContainer().has(coinKey, PersistentDataType.STRING))
            return false;

        String coinType = meta.getPersistentDataContainer().get(coinKey, PersistentDataType.STRING);
        return "SHARD".equals(coinType);
    }

    /**
     * Check if an item is a legitimate coin cluster
     */
    public boolean isCoinCluster(ItemStack item) {
        if (item == null || item.getType() != Material.AMETHYST_CLUSTER)
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        // Check for NBT marker
        if (!meta.getPersistentDataContainer().has(coinKey, PersistentDataType.STRING))
            return false;

        String coinType = meta.getPersistentDataContainer().get(coinKey, PersistentDataType.STRING);
        return "CLUSTER".equals(coinType);
    }

    /**
     * Get the coin value of an item (0 if not a valid coin)
     */
    public int getCoinValue(ItemStack item) {
        if (item == null)
            return 0;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return 0;

        if (!meta.getPersistentDataContainer().has(coinValueKey, PersistentDataType.INTEGER))
            return 0;

        Integer value = meta.getPersistentDataContainer().get(coinValueKey, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    /**
     * Check if an item is any type of legitimate coin
     */
    public boolean isValidCoin(ItemStack item) {
        return isCoinShard(item) || isCoinCluster(item);
    }

    /**
     * Get total coin value from an itemstack (amount * value per item)
     */
    public int getTotalValue(ItemStack item) {
        if (!isValidCoin(item))
            return 0;
        return getCoinValue(item) * item.getAmount();
    }
}
