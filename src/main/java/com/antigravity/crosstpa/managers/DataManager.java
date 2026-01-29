package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DataManager {

    private final CrossTPA plugin;
    private File dataFile;
    private FileConfiguration config;

    public DataManager(CrossTPA plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
    }

    public void saveDefaultConfig() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "data.yml");
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data.yml", e);
            }
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getConfig() {
        if (config == null)
            reloadConfig();
        return config;
    }

    public void saveConfig() {
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    public void saveUserLists(Map<UUID, Set<UUID>> blockLists, Map<UUID, Set<UUID>> muteLists, Set<UUID> disabledTpa) {
        reloadConfig(); // Ensure we don't overwrite concurrent changes
        config.set("blocks", null);
        config.set("mutes", null);
        config.set("disabled", null);

        for (Map.Entry<UUID, Set<UUID>> entry : blockLists.entrySet()) {
            List<String> list = entry.getValue().stream().map(UUID::toString).toList();
            config.set("blocks." + entry.getKey(), list);
        }

        for (Map.Entry<UUID, Set<UUID>> entry : muteLists.entrySet()) {
            List<String> list = entry.getValue().stream().map(UUID::toString).toList();
            config.set("mutes." + entry.getKey(), list);
        }

        List<String> disabled = disabledTpa.stream().map(UUID::toString).toList();
        config.set("disabled", disabled);

        saveConfig();
    }

    public void saveFriends(Map<UUID, Set<UUID>> friendLists) {
        reloadConfig();
        config.set("friends", null);
        for (Map.Entry<UUID, Set<UUID>> entry : friendLists.entrySet()) {
            List<String> list = entry.getValue().stream().map(UUID::toString).toList();
            config.set("friends." + entry.getKey(), list);
        }
        saveConfig();
    }

    public void saveHomes(Map<UUID, Map<String, Location>> playerHomes) {
        reloadConfig();
        config.set("homes", null);
        for (Map.Entry<UUID, Map<String, Location>> entry : playerHomes.entrySet()) {
            ConfigurationSection userSection = config.createSection("homes." + entry.getKey());
            for (Map.Entry<String, Location> homeEntry : entry.getValue().entrySet()) {
                Location loc = homeEntry.getValue();
                String path = homeEntry.getKey();
                userSection.set(path + ".world", loc.getWorld().getName());
                userSection.set(path + ".x", loc.getX());
                userSection.set(path + ".y", loc.getY());
                userSection.set(path + ".z", loc.getZ());
                userSection.set(path + ".yaw", (double) loc.getYaw());
                userSection.set(path + ".pitch", (double) loc.getPitch());
            }
        }
        saveConfig();
    }

    public void loadFriends(Map<UUID, Set<UUID>> friendLists) {
        reloadConfig();
        ConfigurationSection section = config.getConfigurationSection("friends");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Set<UUID> list = new HashSet<>();
                config.getStringList("friends." + key).forEach(s -> list.add(UUID.fromString(s)));
                friendLists.put(UUID.fromString(key), list);
            }
        }
    }

    public void loadHomes(Map<UUID, Map<String, Location>> playerHomes) {
        reloadConfig();
        ConfigurationSection homesSection = config.getConfigurationSection("homes");
        if (homesSection != null) {
            for (String uuidStr : homesSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection userSection = homesSection.getConfigurationSection(uuidStr);
                if (userSection != null) {
                    Map<String, Location> homes = new HashMap<>();
                    for (String homeName : userSection.getKeys(false)) {
                        ConfigurationSection locSection = userSection.getConfigurationSection(homeName);
                        if (locSection != null) {
                            String worldName = locSection.getString("world");
                            if (Bukkit.getWorld(worldName) != null) {
                                Location loc = new Location(
                                        Bukkit.getWorld(worldName),
                                        locSection.getDouble("x"),
                                        locSection.getDouble("y"),
                                        locSection.getDouble("z"),
                                        (float) locSection.getDouble("yaw"),
                                        (float) locSection.getDouble("pitch"));
                                homes.put(homeName, loc);
                            }
                        }
                    }
                    playerHomes.put(uuid, homes);
                }
            }
        }
    }

    public void loadUserLists(Map<UUID, Set<UUID>> blockLists, Map<UUID, Set<UUID>> muteLists, Set<UUID> disabledTpa) {
        reloadConfig();

        if (config.getConfigurationSection("blocks") != null) {
            for (String key : config.getConfigurationSection("blocks").getKeys(false)) {
                Set<UUID> list = new HashSet<>();
                config.getStringList("blocks." + key).forEach(s -> list.add(UUID.fromString(s)));
                blockLists.put(UUID.fromString(key), list);
            }
        }

        if (config.getConfigurationSection("mutes") != null) {
            for (String key : config.getConfigurationSection("mutes").getKeys(false)) {
                Set<UUID> list = new HashSet<>();
                config.getStringList("mutes." + key).forEach(s -> list.add(UUID.fromString(s)));
                muteLists.put(UUID.fromString(key), list);
            }
        }

        config.getStringList("disabled").forEach(s -> disabledTpa.add(UUID.fromString(s)));
    }

    public void saveTeams(Map<String, TeamData> teams) {
        reloadConfig();
        config.set("teams", null);
        for (Map.Entry<String, TeamData> entry : teams.entrySet()) {
            String name = entry.getKey();
            TeamData data = entry.getValue();
            config.set("teams." + name + ".owner", data.owner.toString());
            config.set("teams." + name + ".color", data.color);
            config.set("teams." + name + ".members", data.members.stream().map(UUID::toString).toList());

            ConfigurationSection rolesSection = config.createSection("teams." + name + ".roles");
            for (Map.Entry<UUID, String> roleEntry : data.roles.entrySet()) {
                rolesSection.set(roleEntry.getKey().toString(), roleEntry.getValue());
            }

            config.set("teams." + name + ".allies", new ArrayList<>(data.allies));
            config.set("teams." + name + ".friendly_fire", data.friendlyFire);

            if (data.home != null) {
                config.set("teams." + name + ".home.world", data.home.getWorld().getName());
                config.set("teams." + name + ".home.x", data.home.getX());
                config.set("teams." + name + ".home.y", data.home.getY());
                config.set("teams." + name + ".home.z", data.home.getZ());
                config.set("teams." + name + ".home.yaw", data.home.getYaw());
                config.set("teams." + name + ".home.pitch", data.home.getPitch());
            } else {
                config.set("teams." + name + ".home", null);
            }

            config.set("teams." + name + ".kills", data.teamKills);

            if (data.missions != null && !data.missions.isEmpty()) {
                ConfigurationSection missionSec = config.createSection("teams." + name + ".missions");
                for (MissionData mission : data.missions) {
                    ConfigurationSection m = missionSec.createSection(mission.id);
                    m.set("name", mission.name);
                    m.set("type", mission.type);
                    m.set("amount", mission.amount);
                    m.set("progress", mission.progress);
                    m.set("reward", mission.reward);
                    m.set("creator", mission.creator != null ? mission.creator.toString() : null);
                    m.set("completed", mission.completed);
                    m.set("item_material", mission.itemMaterial); // New field for ITEM missions
                    m.set("target", mission.target); // Bounty target
                    m.set("request_review", mission.requestReview); // Task verification status
                }
            } else {
                config.set("teams." + name + ".missions", null);
            }

            // Clear old legacy fields
            config.set("teams." + name + ".mission", null);

            // Pending Items
            if (!data.pendingItems.isEmpty()) {
                ConfigurationSection pendingSec = config.createSection("teams." + name + ".pendingItems");
                for (Map.Entry<UUID, List<ItemStack>> pendingEntry : data.pendingItems.entrySet()) {
                    pendingSec.set(pendingEntry.getKey().toString(), pendingEntry.getValue());
                }
            } else {
                config.set("teams." + name + ".pendingItems", null);
            }

            ConfigurationSection coinsSec = config.createSection("teams." + name + ".coins");
            data.memberCoins.forEach((uuid, amount) -> coinsSec.set(uuid.toString(), amount));
        }
        saveConfig();
    }

    public void loadTeams(Map<String, TeamData> teams) {
        reloadConfig();
        ConfigurationSection section = config.getConfigurationSection("teams");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                String ownerStr = config.getString("teams." + name + ".owner");
                if (ownerStr == null)
                    continue;
                UUID owner = UUID.fromString(ownerStr);
                String color = config.getString("teams." + name + ".color", "white");
                boolean friendlyFire = config.getBoolean("teams." + name + ".friendly_fire", false);

                Set<UUID> members = new HashSet<>();
                config.getStringList("teams." + name + ".members").forEach(s -> members.add(UUID.fromString(s)));

                Map<UUID, String> roles = new HashMap<>();
                ConfigurationSection rolesSec = config.getConfigurationSection("teams." + name + ".roles");
                if (rolesSec != null) {
                    for (String uuidStr : rolesSec.getKeys(false)) {
                        roles.put(UUID.fromString(uuidStr), rolesSec.getString(uuidStr));
                    }
                }

                Set<String> allies = new HashSet<>(config.getStringList("teams." + name + ".allies"));

                Location home = null;
                ConfigurationSection homeSec = config.getConfigurationSection("teams." + name + ".home");
                if (homeSec != null) {
                    String worldName = homeSec.getString("world");
                    if (Bukkit.getWorld(worldName) != null) {
                        home = new Location(
                                Bukkit.getWorld(worldName),
                                homeSec.getDouble("x"),
                                homeSec.getDouble("y"),
                                homeSec.getDouble("z"),
                                (float) homeSec.getDouble("yaw"),
                                (float) homeSec.getDouble("pitch"));
                    }
                }

                int teamKills = config.getInt("teams." + name + ".kills", 0);

                List<MissionData> missions = new ArrayList<>();
                ConfigurationSection missionSec = config.getConfigurationSection("teams." + name + ".missions");
                if (missionSec != null) {
                    for (String id : missionSec.getKeys(false)) {
                        ConfigurationSection m = missionSec.getConfigurationSection(id);
                        if (m == null)
                            continue;
                        String mName = m.getString("name");
                        String type = m.getString("type");
                        int amount = m.getInt("amount");
                        int progress = m.getInt("progress");
                        int reward = m.getInt("reward");
                        String cStr = m.getString("creator");
                        UUID creator = cStr != null ? UUID.fromString(cStr) : null;
                        boolean completed = m.getBoolean("completed");
                        String itemMat = m.getString("item_material");
                        String target = m.getString("target");
                        boolean reqReview = m.getBoolean("request_review");
                        missions.add(new MissionData(id, mName, type, amount, progress, reward, creator, completed,
                                itemMat, target, reqReview));
                    }
                }

                Map<UUID, List<ItemStack>> pendingItems = new HashMap<>();
                ConfigurationSection pendingSec = config.getConfigurationSection("teams." + name + ".pendingItems");
                if (pendingSec != null) {
                    for (String uuidStr : pendingSec.getKeys(false)) {
                        List<?> list = pendingSec.getList(uuidStr);
                        if (list != null) {
                            List<ItemStack> items = new ArrayList<>();
                            for (Object obj : list) {
                                if (obj instanceof ItemStack)
                                    items.add((ItemStack) obj);
                            }
                            pendingItems.put(UUID.fromString(uuidStr), items);
                        }
                    }
                }

                Map<UUID, Integer> memberCoins = new HashMap<>();
                ConfigurationSection coinsSec = config.getConfigurationSection("teams." + name + ".coins");
                if (coinsSec != null) {
                    for (String key : coinsSec.getKeys(false)) {
                        memberCoins.put(UUID.fromString(key), coinsSec.getInt(key));
                    }
                }

                teams.put(name,
                        new TeamData(owner, members, roles, allies, color, friendlyFire, home, teamKills, memberCoins,
                                missions, pendingItems));
            }
        }
    }

    public static class TeamData {
        public UUID owner;
        public Set<UUID> members;
        public Map<UUID, String> roles;
        public Set<String> allies;
        public String color;
        public boolean friendlyFire;
        public Location home;
        public int teamKills;
        public Map<UUID, Integer> memberCoins;

        public List<MissionData> missions;
        public Map<UUID, List<ItemStack>> pendingItems; // For creators to collect

        public TeamData(UUID owner, Set<UUID> members, String color) {
            this(owner, members, new HashMap<>(), new HashSet<>(), color, false, null, 0, new HashMap<>(),
                    new ArrayList<>(), new HashMap<>());
            this.roles.put(owner, "LEADER");
        }

        public TeamData(UUID owner, Set<UUID> members, Map<UUID, String> roles, Set<String> allies, String color,
                boolean friendlyFire, Location home, int teamKills, Map<UUID, Integer> memberCoins,
                List<MissionData> missions, Map<UUID, List<ItemStack>> pendingItems) {
            this.owner = owner;
            this.members = members;
            this.roles = roles;
            this.allies = allies;
            this.color = color;
            this.friendlyFire = friendlyFire;
            this.home = home;
            this.teamKills = teamKills;
            this.memberCoins = memberCoins;
            this.missions = missions;
            this.pendingItems = pendingItems;
        }
    }

    public static class MissionData {
        public String id;
        public String name;
        public String type; // ITEM, KILL_ZOMBIES, etc
        public int amount;
        public int progress;
        public int reward;
        public UUID creator;
        public boolean completed;
        public String itemMaterial; // For ITEM type
        public String target; // For BOUNTY
        public boolean requestReview; // For TASK

        public MissionData(String id, String name, String type, int amount, int progress, int reward, UUID creator,
                boolean completed) {
            this(id, name, type, amount, progress, reward, creator, completed, null, null, false);
        }

        public MissionData(String id, String name, String type, int amount, int progress, int reward, UUID creator,
                boolean completed, String itemMaterial) {
            this(id, name, type, amount, progress, reward, creator, completed, itemMaterial, null, false);
        }

        public MissionData(String id, String name, String type, int amount, int progress, int reward, UUID creator,
                boolean completed, String itemMaterial, String target, boolean requestReview) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.amount = amount;
            this.progress = progress;
            this.reward = reward;
            this.creator = creator;
            this.completed = completed;
            this.itemMaterial = itemMaterial;
            this.target = target;
            this.requestReview = requestReview;
        }
    }
}
