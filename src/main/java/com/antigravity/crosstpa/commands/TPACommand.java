package com.antigravity.crosstpa.commands;

import com.antigravity.crosstpa.CrossTPA;
import com.antigravity.crosstpa.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TPACommand implements CommandExecutor, TabCompleter {

    private final CrossTPA plugin;

    public TPACommand(CrossTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("crosstpa")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("crosstpa.admin.reload")) {
                    sender.sendMessage(plugin.getMiniMessage()
                            .deserialize(plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getMiniMessage()
                        .deserialize("<green>CrossTPA configuration and data reloaded successfully!</green>"));
                return true;
            }
            return false;
        }

        if (!plugin.getConfig().getBoolean("commands." + cmdName, true)) {
            sender.sendMessage(plugin.getMiniMessage()
                    .deserialize(getPrefix() + plugin.getConfig().getString("messages.command-disabled")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    plugin.getMiniMessage().deserialize(plugin.getConfig().getString("messages.player-only")));
            return true;
        }

        // Basic permission check for all commands
        if (!player.hasPermission("crosstpa.use")) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(getPrefix() + plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        String prefix = getPrefix();

        switch (cmdName) {
            case "tpa" -> {
                if (args.length < 1) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(
                                    "<yellow>Usage: /tpa <player></yellow>\n<gray>Or use /tpamenu to open the menu!</gray>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix
                            + plugin.getConfig().getString("messages.player-not-found").replace("{player}", args[0])));
                    return true;
                }
                plugin.getRequestManager().sendRequest(player, target, false);
            }
            case "tpahere" -> {
                if (args.length < 1) {
                    sendMainMenuForm(player);
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix
                            + plugin.getConfig().getString("messages.player-not-found").replace("{player}", args[0])));
                    return true;
                }
                plugin.getRequestManager().sendRequest(player, target, true);
            }
            case "tpaccept" -> plugin.getRequestManager().acceptRequest(player);
            case "tpreject", "tpdeny" -> plugin.getRequestManager().rejectRequest(player);
            case "tpcancel" -> plugin.getRequestManager().cancelRequest(player);
            case "tpaall" -> {
                if (!player.hasPermission("crosstpa.admin.all")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target == null || target.equals(player))
                        continue;
                    plugin.getRequestManager().sendRequest(player, target, true);
                }
            }
            case "tpaback" -> {
                if (!player.hasPermission("crosstpa.back")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                plugin.getRequestManager().teleportBack(player);
            }
            case "tpacooldown" -> {
                long remaining = plugin.getRequestManager().getCooldown(player);
                if (remaining <= 0) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + "<green>You have no active cooldown.</green>"));
                } else {
                    player.sendMessage(plugin.getMiniMessage().deserialize(prefix + plugin.getConfig()
                            .getString("messages.tp-cooldown").replace("{time}", String.valueOf(remaining))));
                }
            }
            case "tpainfo" -> plugin.getRequestManager().sendInfo(player);
            case "tpablock" -> {
                if (!plugin.getConfig().getBoolean("settings.enable-blocking")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + plugin.getConfig().getString("messages.blocking-disabled")));
                    return true;
                }
                if (args.length < 1)
                    return false;
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null)
                    plugin.getRequestManager().blockPlayer(player, target);
            }
            case "tpaunblock" -> {
                if (!plugin.getConfig().getBoolean("settings.enable-blocking")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + plugin.getConfig().getString("messages.blocking-disabled")));
                    return true;
                }
                if (args.length < 1)
                    return false;
                plugin.getRequestManager().unblockPlayer(player, args[0]);
            }
            case "tpamute" -> {
                if (!plugin.getConfig().getBoolean("settings.enable-muting")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + plugin.getConfig().getString("messages.muting-disabled")));
                    return true;
                }
                if (args.length < 1)
                    return false;
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null)
                    plugin.getRequestManager().mutePlayer(player, target);
            }
            case "tpaunmute" -> {
                if (!plugin.getConfig().getBoolean("settings.enable-muting")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + plugin.getConfig().getString("messages.muting-disabled")));
                    return true;
                }
                if (args.length < 1)
                    return false;
                plugin.getRequestManager().unmutePlayer(player, args[0]);
            }
            case "tpatoggle" -> plugin.getRequestManager().toggleTpa(player);
            case "tpahistory" -> {
                if (!player.hasPermission("crosstpa.admin.history")) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                List<String> history = plugin.getHistoryManager().getHistory();
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix + plugin.getConfig().getString("messages.history-header")));
                for (int i = 0; i < Math.min(history.size(), 10); i++) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(
                            plugin.getConfig().getString("messages.history-entry").replace("{entry}", history.get(i))));
                }
            }
            case "tpamenu" -> {
                // Open the TPA menu
                try {
                    sendMainMenuForm(player);
                } catch (Exception e) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize("<red>Error opening menu!</red>"));
                    plugin.getLogger().warning("Error opening TPA menu for " + player.getName());
                }
                return true;
            }
            case "tpaitem" -> {
                // Give the player the TPA menu item
                if (!plugin.getConfig().getBoolean("features.item-menu", true)) {
                    player.sendMessage(plugin.getMiniMessage()
                            .deserialize("<red>Item menu is disabled!</red>"));
                    return true;
                }

                String itemName = plugin.getConfig().getString("item-menu.item", "COMPASS");
                org.bukkit.Material material;
                try {
                    material = org.bukkit.Material.valueOf(itemName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = org.bukkit.Material.COMPASS;
                }

                org.bukkit.inventory.ItemStack menuItem = new org.bukkit.inventory.ItemStack(material);
                org.bukkit.inventory.meta.ItemMeta meta = menuItem.getItemMeta();
                if (meta != null) {
                    String displayName = plugin.getConfig().getString("item-menu.name", "§3§lCrossTPA Menu");
                    meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize(displayName));

                    java.util.List<String> loreConfig = plugin.getConfig().getStringList("item-menu.lore");
                    java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                    for (String line : loreConfig) {
                        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                .legacySection().deserialize(line));
                    }
                    meta.lore(lore);
                    menuItem.setItemMeta(meta);
                }

                player.getInventory().addItem(menuItem);
                player.sendMessage(plugin.getMiniMessage()
                        .deserialize(
                                "<green>TPA Menu item added to your inventory! Right-click to open menu.</green>"));
                return true;
            }
        }

        return true;
    }

    private boolean isBedrock(Player player) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    private void sendMainMenuForm(Player player) {
        if (!isBedrock(player)) {
            plugin.getJavaGuiManager().openMainMenu(player);
            return;
        }

        try {
            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("§l§3CrossTPA §8- §rControl Panel")
                    .content("§7Welcome back, §f" + player.getName() + "§7!\n§8Select an action to perform:");

            List<Runnable> actions = new ArrayList<>();

            // 1. TPA Request (Standard)
            builder.button("§l§2» §rTeleport to Player\n§8Send a TPA request", FormImage.Type.PATH,
                    "textures/items/ender_pearl");
            actions.add(() -> sendPlayerListForm(player, false));

            // 2. TPA Here Request
            builder.button("§l§b» §rRequest Player to Me\n§8Send a TPAHere request", FormImage.Type.PATH,
                    "textures/items/compass_item");
            actions.add(() -> sendPlayerListForm(player, true));

            // 3. My Homes (Moved UP for better access)
            builder.button("§l§a» §rMy Homes\n§8Saved locations", FormImage.Type.PATH, "textures/items/bed_red");
            actions.add(() -> sendHomesMenuForm(player));

            // 4. Accept Request
            builder.button("§l§a» §rAccept Request\n§8Confirm latest inbound", FormImage.Type.PATH,
                    "textures/ui/check");
            actions.add(() -> player.performCommand("tpaccept"));

            // 5. Deny Request
            builder.button("§l§c» §rDeny Request\n§8Reject latest inbound", FormImage.Type.PATH, "textures/ui/cross");
            actions.add(() -> player.performCommand("tpreject"));

            // 6. My Team (Moved UP)
            builder.button("§l§e» §rMy Team\n§8Team management", FormImage.Type.PATH, "textures/items/iron_helmet");
            actions.add(() -> sendTeamMenuForm(player));

            // 7. Team Bank
            builder.button("§l§3» §rTeam Bank\n§8Shared vault", FormImage.Type.PATH,
                    "textures/items/chest_minecart");
            actions.add(() -> sendTeamBankForm(player));

            // 8. Personal Bank
            builder.button("§l§6» §rPersonal Bank\n§8Manage your coins", FormImage.Type.PATH,
                    "textures/items/gold_nugget");
            actions.add(() -> sendPersonalBankForm(player));

            // 9. Teleport Back
            builder.button("§l§e» §rTeleport Back\n§8Return to previous location", FormImage.Type.PATH,
                    "textures/items/chorus_fruit");
            actions.add(() -> player.performCommand("tpaback"));

            // 10. My Inbox
            builder.button("§l§f» §rMy Inbox\n§8View active requests", FormImage.Type.PATH,
                    "textures/items/book_writable");
            actions.add(() -> player.performCommand("tpainfo"));

            // 11. My Friends
            builder.button("§l§d» §rMy Friends\n§8Social connections", FormImage.Type.PATH,
                    "textures/items/cake");
            actions.add(() -> sendFriendsMenuForm(player));

            // 12. Security Settings
            builder.button("§l§6» §rSecurity Settings\n§8Blocks & Privacy", FormImage.Type.PATH,
                    "textures/items/barrier");
            actions.add(() -> sendSecurityMenuForm(player));

            // 13. Cancel Request
            builder.button("§l§4» §rCancel Request\n§8Abort latest outbound", FormImage.Type.PATH,
                    "textures/ui/cancel");
            actions.add(() -> player.performCommand("tpcancel"));

            // 14. View Cooldown
            builder.button("§l§7» §rView Cooldown\n§8Check wait time", FormImage.Type.PATH,
                    "textures/items/clock_item");
            actions.add(() -> player.performCommand("tpacooldown"));

            if (player.hasPermission("crosstpa.admin.all")) {
                builder.button("§l§d» §rTeleport All\n§8Admin Request", FormImage.Type.PATH,
                        "textures/items/nether_star");
                actions.add(() -> player.performCommand("tpaall"));
            }

            if (player.hasPermission("crosstpa.admin.reload")) {
                builder.button("§l§4» §rReload Plugin\n§8Refresh systems", FormImage.Type.PATH,
                        "textures/ui/refresh_light");
                actions.add(() -> player.performCommand("crosstpa reload"));
            }

            builder.validResultHandler((form, result) -> {
                int idx = result.clickedButtonId();
                if (idx >= 0 && idx < actions.size()) {
                    actions.get(idx).run();
                }
            });

            FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
        } catch (Exception ignored) {
        }
    }

    private void sendPlayerListForm(Player player, boolean here) {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != null && !p.getUniqueId().equals(player.getUniqueId()))
                .collect(Collectors.toList());

        SimpleForm.Builder builder = SimpleForm.builder()
                .title((here ? "§l§bRequest to Me" : "§l§2Teleport to Player"))
                .content("§7Select a player from the list below:");

        if (players.isEmpty()) {
            builder.content("§cNo other players are currently online.");
        } else {
            for (Player p : players) {
                String name = p.getName();
                if (name == null || name.isEmpty())
                    name = "Unknown Player";
                builder.button("§f" + name + "\n§8Online", FormImage.Type.PATH, "textures/items/emerald");
            }
        }

        builder.button("§c« Back to Menu", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            int idx = result.clickedButtonId();
            if (idx < players.size()) {
                Player target = players.get(idx);
                player.performCommand((here ? "tpahere " : "tpa ") + target.getName());
            } else {
                sendMainMenuForm(player);
            }
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    private void sendSecurityMenuForm(Player player) {
        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                .title("§l§6Security & Privacy")
                .content("§7Manage who can interact with you:")
                .button("§l§e» §rToggle TPA Status\n§8Enable/Disable receiving", FormImage.Type.PATH,
                        "textures/items/ender_eye")
                .button("§l§c» §rBlock System\n§8Prevent player requests", FormImage.Type.PATH,
                        "textures/ui/lock")
                .button("§l§8» §rMute System\n§8Hide player requests", FormImage.Type.PATH, "textures/items/paper")
                .button("§l§4« §rMain Menu\n§8Go back", FormImage.Type.PATH, "textures/ui/arrow_left")
                .validResultHandler((form, result) -> {
                    int idx = result.clickedButtonId();
                    switch (idx) {
                        case 0 -> player.performCommand("tpatoggle");
                        case 1 -> sendActionManagerForm(player, "Block");
                        case 2 -> sendActionManagerForm(player, "Mute");
                        case 3 -> sendMainMenuForm(player);
                    }
                })
                .build());
    }

    private void sendActionManagerForm(Player player, String type) {
        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                .title("§l§6" + type + " Management")
                .content("§7Choose an action for the " + type.toLowerCase() + " system:")
                .button("§l§2» §r" + type + " New Player", FormImage.Type.PATH, "textures/ui/plus")
                .button("§l§c» §rRemove from " + type + " List", FormImage.Type.PATH, "textures/ui/minus")
                .button("§l§4« §rBack", FormImage.Type.PATH, "textures/ui/realign_left")
                .validResultHandler((form, result) -> {
                    int idx = result.clickedButtonId();
                    if (idx == 0)
                        sendPlayerListActionForm(player, type.toLowerCase().equals("block") ? "tpablock" : "tpamute",
                                type + " Player");
                    else if (idx == 1)
                        sendListRemovalForm(player, type);
                    else
                        sendSecurityMenuForm(player);
                })
                .build());
    }

    private void sendPlayerListActionForm(Player player, String command, String title) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player);

        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§6" + title)
                .content("§7Select a player to " + title.toLowerCase() + ":");

        if (players.isEmpty()) {
            builder.content("§cNo other players available.");
        } else {
            for (Player p : players) {
                builder.button("§f" + p.getName(), FormImage.Type.PATH, "textures/ui/icon_steve");
            }
        }

        builder.button("§c« Back", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            int idx = result.clickedButtonId();
            if (idx < players.size()) {
                Player target = players.get(idx);
                player.performCommand(command + " " + target.getName());
            } else {
                sendSecurityMenuForm(player);
            }
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    private void sendListRemovalForm(Player player, String type) {
        Set<UUID> list = type.equalsIgnoreCase("Block") ? plugin.getRequestManager().getBlockList(player.getUniqueId())
                : plugin.getRequestManager().getMuteList(player.getUniqueId());
        List<UUID> uuids = new ArrayList<>(list);

        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§cRemove from " + type + " List")
                .content("§7Select a player to remove from your " + type.toLowerCase() + " list:");

        if (uuids.isEmpty()) {
            builder.content("§cYour " + type.toLowerCase() + " list is currently empty.");
        } else {
            for (UUID uuid : uuids) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String name = op.getName();
                if (name == null)
                    name = "Unknown (" + uuid.toString().substring(0, 5) + ")";
                builder.button("§f" + name, FormImage.Type.PATH, "textures/ui/icon_steve");
            }
        }

        builder.button("§c« Back", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            int idx = result.clickedButtonId();
            if (idx < uuids.size()) {
                UUID targetUuid = uuids.get(idx);
                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                player.performCommand((type.equalsIgnoreCase("Block") ? "tpaunblock " : "tpaunmute ") + op.getName());
            } else {
                sendSecurityMenuForm(player);
            }
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("crosstpa")) {
            if (args.length == 1)
                return List.of("reload");
            return List.of();
        }
        if (args.length == 1) {
            String sub = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(sub))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private String getPrefix() {
        return plugin.getConfig().getBoolean("settings.prefix-enabled", true)
                ? plugin.getConfig().getString("messages.prefix", "<gradient:#4facfe:#00f2fe>[CrossTPA]</gradient> ")
                : "";
    }

    private void sendFriendsMenuForm(Player player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§dMy Friends")
                .content("§7Manage your social circle:")
                .button("§l§a» §rView Friends\n§8List all friends", FormImage.Type.PATH, "textures/ui/icon_steve")
                .button("§l§e» §rPending Requests\n§8Check invitations", FormImage.Type.PATH,
                        "textures/items/book_writable")
                .button("§l§b» §rAdd Friend\n§8Invite a player", FormImage.Type.PATH, "textures/ui/plus")
                .button("§l§4« §rMain Menu\n§8Go back", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            int idx = result.clickedButtonId();
            switch (idx) {
                case 0 -> player.sendMessage("§cFeature coming soon!");
                case 1 -> player.sendMessage("§cFeature coming soon!");
                case 2 -> sendPlayerListActionForm(player, "friend add", "Invite Player");
                case 3 -> sendMainMenuForm(player);
            }
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    private void sendHomesMenuForm(Player player) {
        Map<String, Location> homes = plugin.getHomeManager().getHomes(player);
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§aMy Homes")
                .content("§7Select a home to teleport:");

        List<String> homeNames = new ArrayList<>(homes.keySet());
        for (String name : homeNames) {
            builder.button("§l§2" + name + "\n§8Saved Location", FormImage.Type.PATH, "textures/items/bed_red");
        }

        builder.button("§l§4« §rBack\n§8Main Menu", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            int idx = result.clickedButtonId();
            if (idx < homeNames.size()) {
                player.performCommand("home " + homeNames.get(idx));
            } else {
                sendMainMenuForm(player);
            }
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    private void sendTeamMenuForm(Player player) {
        String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        SimpleForm.Builder builder = SimpleForm.builder().title("§l§eMy Team");

        if (teamName == null) {
            builder.content("§7You are not in a team.")
                    .button("§l§a» §rCreate Team\n§8Start a new squad", FormImage.Type.PATH, "textures/ui/plus")
                    .button("§l§4« §rBack\n§8Main Menu", FormImage.Type.PATH, "textures/ui/arrow_left")
                    .validResultHandler((form, result) -> {
                        int idx = result.clickedButtonId();
                        if (idx == 0) {
                            player.sendMessage("§eType §f/team create <name> §eto start your team!");
                        } else {
                            sendMainMenuForm(player);
                        }
                    });
        } else {
            DataManager.TeamData data = plugin.getTeamManager().getTeamData(teamName);
            builder.content("§7Team: §f" + teamName + "\n§7Color: §f" + data.color)
                    .button("§l§e» §rTeam Info\n§8Stats & Members", FormImage.Type.PATH, "textures/items/paper")
                    .button("§l§b» §rInvite Player\n§8Add to team", FormImage.Type.PATH, "textures/ui/plus")
                    .button("§l§c» §rManage Members\n§8Kick/Promote/Demote", FormImage.Type.PATH,
                            "textures/items/iron_sword")
                    .button("§l§d» §rManage Allies\n§8Add/View Allies", FormImage.Type.PATH,
                            "textures/items/flower_pot_item")
                    .button("§l§6» §rChange Color\n§8Set team style", FormImage.Type.PATH,
                            "textures/items/dye_powder_cyan")
                    .button("§l§c» §rLeave Team\n§8Quit squad", FormImage.Type.PATH, "textures/ui/cancel")
                    .button("§l§4« §rBack\n§8Main Menu", FormImage.Type.PATH, "textures/ui/arrow_left")
                    .validResultHandler((form, result) -> {
                        int idx = result.clickedButtonId();
                        switch (idx) {
                            case 0 -> player.performCommand("team info");
                            case 1 -> sendPlayerListActionForm(player, "team invite", "Invite Player");
                            case 2 ->
                                player.sendMessage("§eUse §f/team kick/promote/demote <player> §eto manage members!");
                            case 3 -> player.sendMessage("§eUse §f/team ally <team> §eto send ally requests!");
                            case 4 -> {
                                player.performCommand("team color aqua");
                                sendTeamMenuForm(player);
                            }
                            case 5 -> {
                                player.performCommand("team leave");
                                sendMainMenuForm(player);
                            }
                            case 6 -> sendMainMenuForm(player);
                        }
                    });
        }

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    private void sendPersonalBankForm(Player player) {
        int balance = plugin.getTeamManager().getMemberCoins(player.getUniqueId());

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                .title("§l§6Personal Bank")
                .content("§7Your Balance: §f" + balance
                        + " Coins\n\n§7This is your personal currency,\n§7separate from team funds.")
                .button("§l§a» §rView Transactions\n§8Recent history", FormImage.Type.PATH,
                        "textures/items/book_writable")
                .button("§l§b» §rTransfer to Player\n§8Send coins", FormImage.Type.PATH, "textures/items/emerald")
                .button("§l§e» §rDeposit to Team\n§8Add to team vault", FormImage.Type.PATH,
                        "textures/items/chest_minecart")
                .button("§l§4« §rBack\n§8Main Menu", FormImage.Type.PATH, "textures/ui/arrow_left")
                .validResultHandler((form, result) -> {
                    int idx = result.clickedButtonId();
                    switch (idx) {
                        case 0 -> player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<gray>Transaction history coming soon!</gray>"));
                        case 1 -> player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<yellow>Use /team transfer <player> <amount> to send coins!</yellow>"));
                        case 2 -> player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<gray>Team deposit feature coming soon!</gray>"));
                        case 3 -> sendMainMenuForm(player);
                    }
                })
                .build());
    }

    private void sendTeamBankForm(Player player) {
        String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize("<red>You are not in a team!</red>"));
            return;
        }

        int balance = plugin.getTeamManager().getMemberCoins(player.getUniqueId());

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                .title("§l§3Team Bank")
                .content("§7Team: §f" + teamName + "\n§7Balance: §f" + balance
                        + " Coins\n\n§7Shared team vault for all members.")
                .button("§l§a» §rDeposit Items\n§8Add shards/clusters", FormImage.Type.PATH, "textures/ui/plus")
                .button("§l§c» §rWithdraw Coins\n§8Take from vault", FormImage.Type.PATH, "textures/ui/minus")
                .button("§l§b» §rConvert Currency\n§8Shards ↔ Clusters", FormImage.Type.PATH, "textures/items/emerald")
                .button("§l§6» §rCollect Rewards\n§8Mission items", FormImage.Type.PATH, "textures/items/diamond")
                .button("§l§4« §rBack\n§8Main Menu", FormImage.Type.PATH, "textures/ui/arrow_left")
                .validResultHandler((form, result) -> {
                    int idx = result.clickedButtonId();
                    switch (idx) {
                        case 0 -> player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<gray>Hold items in hand and use /team bank deposit</gray>"));
                        case 1 -> player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<yellow>Use /team bank withdraw <amount></yellow>"));
                        case 2 -> player.sendMessage(plugin.getMiniMessage()
                                .deserialize("<gray>Hold items and use /team bank convert</gray>"));
                        case 3 -> {
                            plugin.getTeamManager().collectPendingItems(player);
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize("<green>Collected pending items!</green>"));
                        }
                        case 4 -> sendMainMenuForm(player);
                    }
                })
                .build());
    }
}
