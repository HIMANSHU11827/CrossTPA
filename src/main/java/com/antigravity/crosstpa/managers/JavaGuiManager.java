package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JavaGuiManager implements Listener {

    private final CrossTPA plugin;

    // Custom Holder to safely identify plugin GUIs without relying on title strings
    private static class TpaGuiHolder implements InventoryHolder {
        private final String type;
        private final int size;

        public TpaGuiHolder(String type, int size) {
            this.type = type;
            this.size = size;
        }

        public String getType() {
            return type;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, size);
        }
    }

    public JavaGuiManager(CrossTPA plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        int size = 54;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("MAIN", size), size,
                Component.text("§8» §3§lCrossTPA Control Panel"));

        ItemStack glass = createConfigItem("java.filler-item", "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        // --- ROW 1: TELEPORT ACTIONS ---
        inv.setItem(11, createConfigItem("java.tpa-item", "COMPASS", "§l§2» §rTeleport to Player",
                "§7Search for online players", "§7to send a TPA request."));
        inv.setItem(12, createConfigItem("java.tpahere-item", "PLAYER_HEAD", "§l§b» §rRequest Player to Me",
                "§7Ask a player to teleport", "§7to your current location."));
        inv.setItem(14, createConfigItem("java.tpaback-item", "FEATHER", "§l§e» §rTeleport Back",
                "§7Return to your previous", "§7location."));
        inv.setItem(15, createItem(Material.ENDER_PEARL, "§l§d» §rTeleport All",
                "§7Request all players to", "§7teleport to you.", " ", "§c§lADMIN ONLY"));

        // --- ROW 2: REQUEST MANAGEMENT ---
        inv.setItem(20, createConfigItem("java.accept-item", "EMERALD", "§l§a» §rAccept Request",
                "§7Quickly accept the latest", "§7incoming request."));
        inv.setItem(21, createConfigItem("java.deny-item", "REDSTONE", "§l§c» §rDeny Request",
                "§7Reject the latest incoming", "§7teleport request."));
        inv.setItem(23, createItem(Material.BARRIER, "§l§4» §rCancel Request",
                "§7Cancel your outgoing", "§7teleport request."));
        inv.setItem(24, createItem(Material.BOOK, "§l§f» §rMy Inbox",
                "§7View active incoming", "§7requests and info."));

        // --- ROW 3: SOCIAL & HOMES ---
        inv.setItem(29, createItem(Material.GOLD_INGOT, "§l§6» §rPersonal Bank",
                "§7Manage your personal", "§7currency and balance."));
        inv.setItem(30, createItem(Material.TOTEM_OF_UNDYING, "§l§d» §rMy Friends",
                "§7Manage your friends list", "§7and pending requests."));
        inv.setItem(31, createConfigItem("java.settings-item", "COMPARATOR", "§l§6» §rSecurity Settings",
                "§7Manage blocks, mutes,", "§7and privacy status."));
        inv.setItem(32, createItem(Material.RED_BED, "§l§a» §rMy Homes",
                "§7View and teleport to", "§7your saved homes."));
        inv.setItem(33, createItem(Material.SHIELD, "§l§e» §rMy Team",
                "§7Create or manage your", "§7team and teammates."));
        inv.setItem(34, createItem(Material.CHEST, "§l§3» §rTeam Bank",
                "§7Access your team's", "§7shared bank vault."));

        // --- ROW 4: UTILITIES ---
        inv.setItem(40, createItem(Material.CLOCK, "§l§7» §rView Cooldown",
                "§7Check how long until", "§7your next request."));

        // Admin Action
        if (player.hasPermission("crosstpa.admin.reload")) {
            inv.setItem(49, createItem(Material.RECOVERY_COMPASS, "§l§4» §rReload Plugin", "§7Reload configurations",
                    "§7and data files."));
        }

        inv.setItem(45, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lQUICK INFO",
                "§7Status: §aOnline",
                "§7Requests: §f" + (plugin.getRequestManager().getCooldown(player) > 0 ? "§cOn Cooldown" : "§aReady")));

        player.openInventory(inv);
    }

    public void openPlayerList(Player player, boolean here) {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != null && !p.getUniqueId().equals(player.getUniqueId()))
                .collect(Collectors.toList());

        int baseSize = ((players.size() / 9) + 1) * 9 + 9;
        int size = Math.min(Math.max(baseSize, 27), 54);

        Inventory inv = Bukkit.createInventory(new TpaGuiHolder(here ? "LIST_HERE" : "LIST_TPA", size), size,
                here ? Component.text("§8» §b§lRequest to Me") : Component.text("§8» §2§lTeleport to Player"));

        for (Player p : players) {
            inv.addItem(createSkull(p, "§8Click to select player"));
        }

        inv.setItem(size - 1, createItem(Material.BARRIER, "§c« Back to Menu"));
        player.openInventory(inv);
    }

    public void openSecurityMenu(Player player) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("SECURITY", size), size,
                Component.text("§8» §6§lSecurity & Privacy"));

        ItemStack glass = createConfigItem("java.filler-item", "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        inv.setItem(11, createItem(Material.ENDER_EYE, "§l§e» §rToggle TPA Status", "§7Turn incoming requests",
                "§7ON or OFF."));
        inv.setItem(13, createItem(Material.TNT, "§l§c» §rBlock System", "§7Prevent specific players",
                "§7from sending requests."));
        inv.setItem(15, createItem(Material.PAPER, "§l§8» §rMute System", "§7Silently hide requests",
                "§7from specific players."));
        inv.setItem(22, createItem(Material.ARROW, "§7« Back"));

        player.openInventory(inv);
    }

    private final Map<UUID, String> chatInputActions = new HashMap<>();

    public void startChatInput(Player player, String action) {
        player.closeInventory();
        chatInputActions.put(player.getUniqueId(), action);
        player.sendMessage("§e[GUI] §fPlease type your value in chat now (or type 'cancel').");
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent event) {
        if (chatInputActions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            String input = event.getMessage();
            String action = chatInputActions.remove(player.getUniqueId());

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cAction cancelled.");
                openMainMenu(player); // Re-open
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> { // Sync execution
                switch (action) {
                    case "CREATE_TEAM" -> {
                        if (plugin.getTeamManager().createTeam(player, input)) {
                            player.sendMessage("§aTeam " + input + " created successfully!");
                            openTeamMenu(player);
                        } else {
                            player.sendMessage("§cTeam creation failed. Name taken or you are already in a team.");
                        }
                    }
                    case "WITHDRAW_AMOUNT" -> {
                        try {
                            int amt = Integer.parseInt(input); // Check valid int first
                            player.performCommand("team withdraw " + amt);
                            openBankMenu(player); // Re-open bank
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid number.");
                        }
                    }
                    case "MISSION_REWARD" -> {
                    } // Placeholder for advanced mission creation later
                }
            });
        }
    }

    public void openFriendsMenu(Player player) {
        int size = 54;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("FRIENDS", size), size,
                Component.text("§8» §d§lMy Friends"));

        ItemStack glass = createConfigItem("java.filler-item", "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        Set<UUID> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
        for (UUID uuid : friends) {
            inv.addItem(createSkull(Bukkit.getOfflinePlayer(uuid), "§dFriend"));
        }

        // Navigation
        inv.setItem(48, createItem(Material.PAPER, "§lPending Requests", "§7Click to view incoming requests."));
        inv.setItem(49, createItem(Material.ARROW, "§7« Back to Main"));
        inv.setItem(50, createItem(Material.EMERALD, "§lAdd Friend", "§7Click to add a friend.")); // Opens list?

        player.openInventory(inv);
    }

    public void openFriendRequests(Player player) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("FRIEND_REQUESTS", size), size,
                Component.text("§8» §b§lFriend Requests"));

        Set<UUID> requests = plugin.getFriendManager().getPendingRequests(player.getUniqueId());
        for (UUID uuid : requests) {
            inv.addItem(createSkull(Bukkit.getOfflinePlayer(uuid), "§aClick to Accept"));
        }

        inv.setItem(22, createItem(Material.ARROW, "§7« Back to Friends"));
        player.openInventory(inv);
    }

    public void openHomesMenu(Player player) {
        Map<String, Location> homes = plugin.getHomeManager().getHomes(player);
        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("HOMES", size), size,
                Component.text("§8» §a§lMy Homes"));

        ItemStack glass = createConfigItem("java.filler-item", "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        int slot = 0;
        for (String name : homes.keySet()) {
            if (slot >= 18)
                break;
            inv.setItem(slot++, createItem(Material.RED_BED, "§a§l" + name, "§7Click to teleport."));
        }

        inv.setItem(22, createItem(Material.ARROW, "§7« Back to Menu"));
        player.openInventory(inv);
    }

    public void openTeamMenu(Player player) {
        String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("TEAM", size), size, Component.text("§8» §e§lMy Team"));

        ItemStack glass = createConfigItem("java.filler-item", "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        if (teamName == null) {
            inv.setItem(13, createItem(Material.LIME_CONCRETE, "§l§aCreate Team", "§7Click to create a new team."));
        } else {
            DataManager.TeamData data = plugin.getTeamManager().getTeamData(teamName);
            inv.setItem(10,
                    createItem(Material.PLAYER_HEAD, "§l§eTeam Info", "§7Click to view stats", "§7and members list."));
            inv.setItem(11, createItem(Material.PAPER, "§l§bInvite Player", "§7Invite new teammates."));
            inv.setItem(12, createItem(Material.IRON_SWORD, "§l§cManage Members", "§7Kick, Promote, or Demote."));
            inv.setItem(13, createItem(Material.TOTEM_OF_UNDYING, "§l§dManage Allies", "§7View and add allies."));
            inv.setItem(14, createItem(Material.CYAN_DYE, "§l§6Change Color", "§7Current: §f" + data.color,
                    "§7Click to select color."));
            inv.setItem(15, createItem(Material.OAK_SIGN, "§l§aTeam Chat", "§7Toggle team chat.", "§7Current: "
                    + (plugin.getTeamManager().isTeamChatEnabled(player.getUniqueId()) ? "§aON" : "§cOFF")));
            inv.setItem(16, createItem(Material.DIAMOND_SWORD, "§l§4PvP Settings", "§7Toggle friendly fire.",
                    "§7Current: " + (data.friendlyFire ? "§aENABLED" : "§cDISABLED")));
            inv.setItem(17, createItem(Material.BARRIER, "§l§cLeave Team", "§7Quit the current team."));
            inv.setItem(18,
                    createConfigItem("java.bank.icon", "GOLD_BLOCK", "§l§eTeam Bank", "§7Manage coins, deposit,",
                            "§7withdraw, or convert."));
            inv.setItem(20,
                    createConfigItem("java.mission.item-type", "FILLED_MAP", "§l§6Mission Board",
                            "§7View, Claim, or Manage",
                            "§7team missions."));
        }

        inv.setItem(22, createItem(Material.ARROW, "§7« Back to Menu"));
        player.openInventory(inv);
    }

    public void openPersonalBankMenu(Player player) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("PERSONAL_BANK", size), size,
                Component.text("§8» §6§lPersonal Bank"));

        int bal = plugin.getTeamManager().getMemberCoins(player.getUniqueId());

        ItemStack glass = createConfigItem("java.filler-item", "BLACK_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        inv.setItem(4, createItem(Material.GOLD_BLOCK, "§l§6Personal Balance", "§f" + bal + " Coins", " ",
                "§7This is YOUR personal currency.", "§7Separate from team funds."));

        inv.setItem(11, createItem(Material.EMERALD, "§l§aView Transactions",
                "§7Check your recent", "§7coin transactions."));

        inv.setItem(13, createItem(Material.DIAMOND, "§l§bTransfer to Player",
                "§7Send coins to another", "§7player directly."));

        inv.setItem(15, createItem(Material.GOLD_INGOT, "§l§eDeposit to Team",
                "§7Transfer your personal", "§7coins to team bank."));

        inv.setItem(22, createItem(Material.ARROW, "§7« Back to Menu"));
        player.openInventory(inv);
    }

    public void openBankMenu(Player player) {
        String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return;

        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("BANK", size), size,
                Component.text("§8» §e§lTeam Bank"));

        int bal = plugin.getTeamManager().getMemberCoins(player.getUniqueId());

        ItemStack glass = createConfigItem("java.filler-item", "BLACK_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        inv.setItem(4, createItem(Material.SUNFLOWER, "§l§eBalance", "§f" + bal + " Coins"));

        inv.setItem(11,
                createConfigItem("java.bank.deposit", "HOPPER", "§l§aDeposit Held Items", "§7Click to deposit Shards",
                        "§7or Clusters from hand."));
        inv.setItem(13,
                createConfigItem("java.bank.withdraw", "GOLD_INGOT", "§l§cWithdraw Coins", "§7Click to withdraw coins",
                        "§7as physical items."));
        inv.setItem(15, createConfigItem("java.bank.convert", "EXPERIENCE_BOTTLE", "§l§bConvert Currency",
                "§7Left-Click: §f64 Shards -> 16 Clusters",
                "§7Right-Click: §f16 Clusters -> 64 Shards",
                "§8(Must hold items in hand)"));

        // Collection for creators
        inv.setItem(26, createConfigItem("java.bank.collect", "CHEST", "§l§6Collect Items", "§7Collect items earned",
                "§7from completed missions."));

        inv.setItem(22, createItem(Material.ARROW, "§7« Back to Team"));
        player.openInventory(inv);
    }

    public void openMissionMenu(Player player) {
        String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null)
            return;
        DataManager.TeamData data = plugin.getTeamManager().getTeamData(teamName);

        int size = 54;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("MISSION_BOARD", size), size,
                Component.text("§8» §6§lMission Board"));

        ItemStack glass = createConfigItem("java.filler-item", "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        // Loop Missions
        if (data.missions != null) {
            int slot = 0;
            for (DataManager.MissionData m : data.missions) {
                if (slot >= 45)
                    break;

                Material icon = Material.PAPER;
                if ("ITEM".equals(m.type)) {
                    try {
                        icon = Material.valueOf(m.itemMaterial);
                    } catch (Exception e) {
                        icon = Material.CHEST;
                    }
                } else if ("TASK".equals(m.type)) {
                    icon = Material.WRITABLE_BOOK;
                } else {
                    icon = Material.IRON_SWORD;
                }

                String status = "§7Status: §fIn Progress";
                String action = "§eLeft-Click to Check/Claim";

                if (m.completed) {
                    status = "§aStatus: §lCOMPLETED";
                    action = "§a§lLeft-Click to Claim Reward";
                } else if ("TASK".equals(m.type) && m.requestReview) {
                    status = "§6Status: §lUNDER REVIEW";
                    action = "§eRight-Click to Approve (Leaders)";
                }

                String amountStr = "§7Goal: §f" + m.progress + "/" + m.amount;
                if ("ITEM".equals(m.type))
                    amountStr = "§7Goal: §fCollect " + m.amount;

                inv.setItem(slot++, createItem(icon, "§6§l" + m.name,
                        "§7Type: " + m.type,
                        amountStr,
                        "§7Reward: §d" + m.reward + " Shards",
                        " ",
                        status,
                        action,
                        "§8ID: " + m.id));
            }
        }

        inv.setItem(49, createItem(Material.ARROW, "§7« Back to Team"));
        inv.setItem(50, createItem(Material.EMERALD, "§a§lNew Mission", "§7Click to create a new",
                "§7custom mission (Command)."));

        player.openInventory(inv);
    }

    public void openColorPicker(Player player) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("TEAM_COLOR", size), size,
                Component.text("§8» §6§lPick Team Color"));

        String[] colors = { "White", "Orange", "Magenta", "Light_Blue", "Yellow", "Lime", "Pink", "Gray", "Light_Gray",
                "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black" };
        Material[] mats = {
                Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
                Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
                Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL
        };

        for (int i = 0; i < colors.length && i < size; i++) {
            inv.setItem(i, createItem(mats[i], "§l§f" + colors[i], "§7Click to select."));
        }

        inv.setItem(22, createItem(Material.ARROW, "§7« Back to Team Menu"));
        player.openInventory(inv);
    }

    public void openActionManager(Player player, String type) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("ACTION_" + type.toUpperCase(), size), size,
                Component.text("§8» §6§l" + type + " Management"));

        ItemStack glass = createConfigItem("java.filler-item", "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < size; i++)
            inv.setItem(i, glass);

        inv.setItem(11, createItem(Material.LIME_CONCRETE, "§l§2» §r" + type + " New Player",
                "§7Select an online player", "§7to add to the " + type.toLowerCase() + " list."));
        inv.setItem(15, createItem(Material.RED_CONCRETE, "§l§c» §rRemove from " + type + " List",
                "§7View and remove players", "§7from your " + type.toLowerCase() + " list."));
        inv.setItem(22, createItem(Material.ARROW, "§7« Back"));

        player.openInventory(inv);
    }

    public void openListRemovalMenu(Player player, String type) {
        Set<UUID> list = type.equalsIgnoreCase("Block") ? plugin.getRequestManager().getBlockList(player.getUniqueId())
                : plugin.getRequestManager().getMuteList(player.getUniqueId());
        List<UUID> uuids = new ArrayList<>(list);

        int baseSize = ((uuids.size() / 9) + 1) * 9 + 9;
        int size = Math.min(Math.max(baseSize, 27), 54);

        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("REMOVE_" + type.toUpperCase(), size), size,
                Component.text("§l§cRemove from " + type));

        for (UUID uuid : uuids) {
            inv.addItem(createSkull(Bukkit.getOfflinePlayer(uuid), "§cClick to remove from list"));
        }

        inv.setItem(size - 1, createItem(Material.BARRIER, "§c« Back"));
        player.openInventory(inv);
    }

    public void openPlayerListForAction(Player player, String type) {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != null && !p.getUniqueId().equals(player.getUniqueId()))
                .collect(Collectors.toList());

        int baseSize = ((players.size() / 9) + 1) * 9 + 9;
        int size = Math.min(Math.max(baseSize, 27), 54);

        Inventory inv = Bukkit.createInventory(new TpaGuiHolder("SELECT_" + type.toUpperCase(), size), size,
                Component.text("§l§cSelect to " + type));

        for (Player p : players) {
            inv.addItem(createSkull(p, "§8Click to " + type.toLowerCase() + " player"));
        }

        inv.setItem(size - 1, createItem(Material.BARRIER, "§7« Back"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TpaGuiHolder holder))
            return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        String type = holder.getType();

        switch (type) {
            case "MAIN" -> {
                switch (event.getRawSlot()) {
                    case 11 -> openPlayerList(player, false);
                    case 12 -> openPlayerList(player, true);
                    case 14 -> player.performCommand("tpaback");
                    case 15 -> player.performCommand("tpaall");
                    case 20 -> player.performCommand("tpaccept");
                    case 21 -> player.performCommand("tpreject");
                    case 23 -> player.performCommand("tpcancel");
                    case 24 -> player.performCommand("tpainfo");
                    case 29 -> openPersonalBankMenu(player);
                    case 30 -> openFriendsMenu(player);
                    case 31 -> openSecurityMenu(player);
                    case 32 -> openHomesMenu(player);
                    case 33 -> openTeamMenu(player);
                    case 34 -> openBankMenu(player);
                    case 40 -> player.performCommand("tpacooldown");
                    case 49 -> {
                        if (player.hasPermission("crosstpa.admin.reload")) {
                            player.performCommand("crosstpa reload");
                            player.closeInventory();
                        }
                    }
                }
            }
            case "LIST_TPA", "LIST_HERE" -> {
                if (item.getType() == Material.BARRIER) {
                    openMainMenu(player);
                } else if (item.getType() == Material.PLAYER_HEAD) {
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    if (meta != null && meta.getOwningPlayer() != null) {
                        String name = meta.getOwningPlayer().getName();
                        if (name != null) {
                            player.performCommand((type.equals("LIST_HERE") ? "tpahere " : "tpa ") + name);
                            if (plugin.getConfig().getBoolean("gui.close-on-action", true))
                                player.closeInventory();
                        }
                    }
                }
            }
            case "SECURITY" -> {
                switch (event.getRawSlot()) {
                    case 11 -> player.performCommand("tpatoggle");
                    case 13 -> openActionManager(player, "Block");
                    case 15 -> openActionManager(player, "Mute");
                    case 22 -> openMainMenu(player);
                }
            }
            case "FRIENDS" -> {
                // If this is the MAIN Friends Menu
                if (event.getRawSlot() == 48) {
                    openFriendRequests(player);
                } else if (event.getRawSlot() == 50) {
                    openPlayerListForAction(player, "Friend"); // Add Friend
                } else if (event.getRawSlot() == 49) {
                    openMainMenu(player);
                } else if (item.getType() == Material.PLAYER_HEAD) {
                    // Maybe remove friend?
                    openActionManager(player, "Friend"); // Redirect to remove/manage
                }
            }
            case "FRIEND_REQUESTS" -> {
                if (event.getRawSlot() == 22) {
                    openFriendsMenu(player);
                } else if (item.getType() == Material.PLAYER_HEAD) {
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    if (meta != null && meta.getOwningPlayer() != null) {
                        String name = meta.getOwningPlayer().getName();
                        if (name != null) {
                            plugin.getFriendManager().acceptFriendRequest(player, name);
                            player.sendMessage("§aAccepted friend request from " + name);
                            openFriendRequests(player); // Refresh
                        }
                    }
                }
            }
            case "HOMES" -> {
                if (item.getType() == Material.RED_BED) {
                    String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName())
                            .substring(4);
                    player.performCommand("home " + name);
                    player.closeInventory();
                } else if (event.getRawSlot() == 22) {
                    openMainMenu(player);
                }
            }
            case "TEAM" -> {
                switch (event.getRawSlot()) {
                    case 13 -> { // Create or Manage
                        if (item.getType() == Material.LIME_CONCRETE) {
                            startChatInput(player, "CREATE_TEAM");
                        } else {
                            // Manage Allies (TOTEM)
                            player.closeInventory();
                            player.sendMessage("§eUse §f/team ally <team> §eto send ally requests!");
                        }
                    }
                    case 10 -> player.performCommand("team info");
                    case 11 -> openPlayerListForAction(player, "TeamInvite");
                    case 12 -> {
                        player.closeInventory();
                        player.sendMessage("§eUse §f/team kick/promote/demote <player> §eto manage members!");
                    }
                    case 14 -> openColorPicker(player);
                    case 15 -> {
                        player.performCommand("team chat");
                        openTeamMenu(player);
                    }
                    case 16 -> {
                        player.performCommand("team pvp");
                        openTeamMenu(player);
                    }
                    case 17 -> {
                        player.performCommand("team leave");
                        player.closeInventory();
                    }
                    case 18 -> openBankMenu(player);
                    case 20 -> openMissionMenu(player);
                    case 22 -> openMainMenu(player);
                }
            }
            case "BANK" -> {
                switch (event.getRawSlot()) {
                    case 11 -> { // Deposit
                        player.closeInventory();
                        player.performCommand("team deposit");
                    }
                    case 13 -> startChatInput(player, "WITHDRAW_AMOUNT");
                    case 15 -> { // Convert Click Logic
                        if (event.isLeftClick()) {
                            // Shards -> Clusters (64 Shards -> 16 Clusters)
                            plugin.getTeamManager().convertCurrency(player, "shards", 16);
                        } else if (event.isRightClick()) {
                            // Clusters -> Shards (16 Clusters -> 64 Shards)
                            plugin.getTeamManager().convertCurrency(player, "clusters", 16);
                        }
                        openBankMenu(player); // Refresh
                    }
                    case 26 -> {
                        player.closeInventory();
                        player.performCommand("team collect");
                    }
                    case 22 -> openTeamMenu(player);
                }
            }
            case "MISSION_BOARD" -> {
                if (event.getRawSlot() == 49) { // Back
                    openTeamMenu(player);
                } else if (event.getRawSlot() == 50) { // Create
                    player.closeInventory();
                    player.sendMessage("§eTo create a mission, use command: §f/team mission create <type> ...");
                    player.sendMessage("§7Types: ITEM:<Material>, PLAYER:<Name>, TASK, or <Mob>");
                    player.sendMessage("§7Example: /team mission create ITEM:DIAMOND 64 100 Gathering");
                } else {
                    // Dynamic Filler Check
                    String fillerName = plugin.getConfig().getString("gui.java.filler-item", "GRAY_STAINED_GLASS_PANE");
                    Material fillerMat = Material.getMaterial(fillerName);
                    if (fillerMat == null)
                        fillerMat = Material.GRAY_STAINED_GLASS_PANE;

                    if (item.getType() != fillerMat && item.hasItemMeta()) {
                        // Extract ID from Lore
                        List<Component> lore = item.getItemMeta().lore();
                        if (lore != null && !lore.isEmpty()) {
                            String idLine = LegacyComponentSerializer.legacySection()
                                    .serialize(lore.get(lore.size() - 1));
                            if (idLine.contains("ID:")) {
                                String id = idLine.substring(idLine.indexOf("ID:") + 4);

                                // Left Click = Claim, Right Click = Approve
                                if (event.isRightClick()) {
                                    plugin.getTeamManager().approveMission(player, id);
                                } else {
                                    plugin.getTeamManager().claimMission(player, id);
                                }
                                // Refresh
                                openMissionMenu(player);
                            }
                        }
                    }
                }
            }
            case "TEAM_COLOR" -> {
                if (event.getRawSlot() == 22) {
                    openTeamMenu(player);
                    return;
                }
                if (item.getType().name().contains("WOOL")) {
                    String colorName = LegacyComponentSerializer.legacySection()
                            .serialize(item.getItemMeta().displayName()).substring(4).toLowerCase();
                    player.performCommand("team color " + colorName);
                    openTeamMenu(player);
                }
            }
            case "ACTION_BLOCK", "ACTION_MUTE", "ACTION_FRIEND", "ACTION_TEAMINVITE", "ACTION_TEAMKICK",
                    "ACTION_TEAMPROMOTE" -> {
                String subType = type.split("_").length > 1 ? type.split("_")[1].toLowerCase() : "unknown";
                String capType = subType.substring(0, 1).toUpperCase() + subType.substring(1);
                switch (event.getRawSlot()) {
                    case 11 -> openPlayerListForAction(player, capType);
                    case 15 -> openListRemovalMenu(player, capType);
                    case 22 -> {
                        if (type.contains("TEAM"))
                            openTeamMenu(player);
                        else
                            openSecurityMenu(player);
                    }
                }
            }
            case "REMOVE_BLOCK", "REMOVE_MUTE", "SELECT_BLOCK", "SELECT_MUTE", "SELECT_FRIEND", "SELECT_TEAMINVITE",
                    "SELECT_TEAMKICK", "SELECT_TEAMPROMOTE" -> {
                if (item.getType() == Material.BARRIER) {
                    if (type.contains("FRIEND"))
                        openFriendsMenu(player);
                    else if (type.contains("TEAM"))
                        openTeamMenu(player);
                    else
                        openSecurityMenu(player);
                    return;
                }
                if (item.getType() == Material.PLAYER_HEAD) {
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    if (meta != null && meta.getOwningPlayer() != null) {
                        String name = meta.getOwningPlayer().getName();
                        if (name != null) {
                            if (type.contains("FRIEND")) {
                                player.performCommand("friend add " + name);
                            } else if (type.contains("TEAMINVITE")) {
                                player.performCommand("team invite " + name);
                            } else if (type.contains("TEAMKICK")) {
                                player.performCommand("team kick " + name);
                            } else if (type.contains("TEAMPROMOTE")) {
                                player.performCommand("team promote " + name);
                            } else {
                                boolean isRemove = type.startsWith("REMOVE");
                                boolean isBlock = type.endsWith("BLOCK");
                                String cmdPrefix = isBlock ? (isRemove ? "tpaunblock " : "tpablock ")
                                        : (isRemove ? "tpaunmute " : "tpamute ");
                                player.performCommand(cmdPrefix + name);

                                if (isRemove) {
                                    openListRemovalMenu(player, isBlock ? "Block" : "Mute");
                                    return;
                                }
                            }

                            if (plugin.getConfig().getBoolean("gui.close-on-action", true))
                                player.closeInventory();
                        }
                    }
                }
            }
            case "PERSONAL_BANK" -> {
                switch (event.getRawSlot()) {
                    case 11 -> player.sendMessage(plugin.getMiniMessage()
                            .deserialize("<gray>Transaction history coming soon!</gray>"));
                    case 13 -> {
                        player.closeInventory();
                        player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<yellow>Type the player name in chat to transfer coins:</yellow>"));
                        startChatInput(player, "TRANSFER_COINS");
                    }
                    case 15 -> {
                        // Transfer to team bank
                        player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<gray>Team deposit feature coming soon!</gray>"));
                    }
                    case 22 -> openMainMenu(player);
                }
            }
        }

    }

    private ItemStack createConfigItem(String configPath, String defaultMat, String name, String... lore) {
        Material mat;
        try {
            mat = Material.valueOf(plugin.getConfig().getString("gui." + configPath, defaultMat));
        } catch (Exception e) {
            mat = Material.valueOf(defaultMat);
        }
        return createItem(mat, name, lore);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            List<Component> loreList = new ArrayList<>();
            for (String s : lore)
                loreList.add(LegacyComponentSerializer.legacySection().deserialize(s));
            meta.lore(loreList);

            // Add enchantment glow + hide enchants to prevent usage
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSkull(OfflinePlayer target, String loreText) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            String name = target.getName();
            meta.displayName(Component.text("§f" + (name != null ? name : "Unknown Player")));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(loreText));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
