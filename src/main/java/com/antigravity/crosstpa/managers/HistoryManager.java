package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryManager {

    private final CrossTPA plugin;
    private final File historyFile;
    private FileConfiguration historyConfig;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public HistoryManager(CrossTPA plugin) {
        this.plugin = plugin;
        this.historyFile = new File(plugin.getDataFolder(), "history.yml");
        loadHistory();
    }

    private void loadHistory() {
        if (!historyFile.exists()) {
            try {
                historyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);
    }

    public void logHistory(Player sender, Player receiver, String action) {
        if (!plugin.getConfig().getBoolean("history.save", true))
            return;

        String time = dateFormat.format(new Date());
        Location loc = sender != null ? sender.getLocation() : null;

        String entry = plugin.getConfig().getString("history.log-format")
                .replace("{time}", time)
                .replace("{sender}", sender != null ? sender.getName() : "Unknown")
                .replace("{receiver}", receiver != null ? receiver.getName() : "Unknown")
                .replace("{action}", action);

        if (loc != null) {
            String locString = loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", "
                    + loc.getBlockZ() + ")";
            entry = entry.replace("{loc}", locString);
        } else {
            entry = entry.replace("{loc}", "Unknown");
        }

        List<String> history = historyConfig.getStringList("history");
        history.add(0, entry); // Add to top
        if (history.size() > 1000)
            history.remove(history.size() - 1); // Limit size

        historyConfig.set("history", history);
        try {
            historyConfig.save(historyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getHistory() {
        return historyConfig.getStringList("history");
    }
}
