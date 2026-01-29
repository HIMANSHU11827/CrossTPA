package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeManager {

    private final CrossTPA plugin;
    private final Map<UUID, Map<String, Location>> playerHomes = new HashMap<>();

    public HomeManager(CrossTPA plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        plugin.getDataManager().loadHomes(playerHomes);
    }

    public void saveData() {
        plugin.getDataManager().saveHomes(playerHomes);
    }

    public boolean setHome(Player player, String name) {
        name = name.toLowerCase();
        int maxHomes = plugin.getConfig().getInt("settings.max-homes", 3);

        Map<String, Location> homes = playerHomes.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

        if (homes.size() >= maxHomes && !homes.containsKey(name)) {
            return false;
        }

        homes.put(name, player.getLocation());
        return true;
    }

    public boolean deleteHome(Player player, String name) {
        name = name.toLowerCase();
        Map<String, Location> homes = playerHomes.get(player.getUniqueId());
        if (homes != null && homes.containsKey(name)) {
            homes.remove(name);
            return true;
        }
        return false;
    }

    public Location getHome(Player player, String name) {
        name = name.toLowerCase();
        Map<String, Location> homes = playerHomes.get(player.getUniqueId());
        if (homes != null) {
            return homes.get(name);
        }
        return null;
    }

    public Map<String, Location> getHomes(Player player) {
        return playerHomes.getOrDefault(player.getUniqueId(), new HashMap<>());
    }
}
