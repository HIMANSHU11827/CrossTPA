package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;
import java.util.stream.Collectors;

public class BedrockMenuManager {

    private final CrossTPA plugin;

    public BedrockMenuManager(CrossTPA plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§3CrossTPA §8- §rControl Panel")
                .content("§7Welcome back, §f" + player.getName() + "§7!\n§8Select an action to perform:");

        List<Runnable> actions = new ArrayList<>();

        // 1. TPA Request (Standard)
        builder.button("§l§2» §rTeleport to Player\n§8Send a TPA request", FormImage.Type.PATH,
                "textures/items/ender_pearl");
        actions.add(() -> openPlayerSelection(player, false));

        // 2. TPA Here Request
        builder.button("§l§b» §rRequest Player to Me\n§8Send a TPAHere request", FormImage.Type.PATH,
                "textures/items/compass_item");
        actions.add(() -> openPlayerSelection(player, true));

        // 3. My Homes (Moved UP for better access)
        builder.button("§l§a» §rMy Homes\n§8Saved locations", FormImage.Type.PATH, "textures/items/bed_red");
        actions.add(() -> openHomesMenu(player));

        // 4. Accept Request
        builder.button("§l§a» §rAccept Request\n§8Confirm latest inbound", FormImage.Type.PATH,
                "textures/ui/check");
        actions.add(() -> player.performCommand("tpaccept"));

        // 5. Deny Request
        builder.button("§l§c» §rDeny Request\n§8Reject latest inbound", FormImage.Type.PATH, "textures/ui/cross");
        actions.add(() -> player.performCommand("tpreject"));

        // 6. My Team (Moved UP)
        builder.button("§l§e» §rMy Team\n§8Team management", FormImage.Type.PATH, "textures/items/iron_helmet");
        actions.add(() -> openTeamMenu(player));

        // 7. Team Bank
        builder.button("§l§3» §rTeam Bank\n§8Shared vault", FormImage.Type.PATH,
                "textures/items/chest_minecart");
        actions.add(() -> openTeamBankMenu(player));

        // 8. Personal Bank
        builder.button("§l§6» §rPersonal Bank\n§8Manage your coins", FormImage.Type.PATH,
                "textures/items/gold_nugget");
        actions.add(() -> openPersonalBankMenu(player));

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
        actions.add(() -> openFriendsMenu(player));

        // 12. Security Settings
        builder.button("§l§6» §rSecurity Settings\n§8Blocks & Privacy", FormImage.Type.PATH,
                "textures/items/barrier");
        actions.add(() -> openSecurityMenu(player));

        // 13. Cancel Request
        builder.button("§l§4» §rCancel Request\n§8Abort latest outbound", FormImage.Type.PATH,
                "textures/ui/cancel");
        actions.add(() -> player.performCommand("tpcancel"));

        // 14. View Cooldown
        builder.button("§l§7» §rView Cooldown\n§8Check wait time", FormImage.Type.PATH, "textures/items/clock_item");
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

        sendForm(player, builder);
    }

    // --- HOMES ---

    public void openHomesMenu(Player player) {
        Map<String, Location> homes = plugin.getHomeManager().getHomes(player);
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§aMy Homes")
                .content("§7You have §f" + homes.size() + " §7homes saved.");

        builder.button("§l§2» §rSet New Home\n§8Create a location", FormImage.Type.PATH, "textures/ui/plus");

        List<String> homeNames = new ArrayList<>(homes.keySet());
        for (String name : homeNames) {
            builder.button("§l§f" + name + "\n§8Teleport", FormImage.Type.PATH, "textures/items/bed_red");
        }

        builder.button("§l§c» §rDelete Home\n§8Remove a location", FormImage.Type.PATH, "textures/ui/minus");

        builder.button("§l§4« §rMain Menu\n§8Go back", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            int idx = result.clickedButtonId();
            if (idx == 0) {
                openSetHomeForm(player); // Create
            } else if (idx <= homeNames.size()) {
                // Teleport to home (index 1 to size)
                String targetHome = homeNames.get(idx - 1);
                player.performCommand("home " + targetHome);
            } else if (idx == homeNames.size() + 1) {
                openDeleteHomeForm(player); // Delete
            } else {
                openMainMenu(player); // Back
            }
        });

        sendForm(player, builder);
    }

    private void openSetHomeForm(Player player) {
        CustomForm.Builder builder = CustomForm.builder()
                .title("§l§2Set New Home")
                .input("§7Enter a name for this home:", "e.g. base, mine");

        builder.validResultHandler((form, result) -> {
            String name = result.next();
            if (name != null && !name.trim().isEmpty()) {
                player.performCommand("sethome " + name);
            }
            openHomesMenu(player);
        });

        sendForm(player, builder);
    }

    private void openDeleteHomeForm(Player player) {
        Map<String, Location> homes = plugin.getHomeManager().getHomes(player);
        List<String> homeNames = new ArrayList<>(homes.keySet());

        if (homeNames.isEmpty()) {
            player.sendMessage("§cYou have no homes to delete.");
            openHomesMenu(player);
            return;
        }

        CustomForm.Builder builder = CustomForm.builder()
                .title("§l§cDelete Home")
                .dropdown("§7Select a home to remove:", homeNames);

        builder.validResultHandler((form, result) -> {
            int idx = result.next();
            String toDelete = homeNames.get(idx);
            player.performCommand("delhome " + toDelete);
            openHomesMenu(player);
        });

        sendForm(player, builder);
    }

    // --- TEAMS ---

    public void openTeamMenu(Player player) {
        String teamName = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (teamName == null) {
            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("§l§eTeam Management")
                    .content("§7You are not currently in a team.");

            builder.button("§l§a» §rCreate Team\n§8Start a new squad", FormImage.Type.PATH, "textures/ui/plus");
            builder.button("§l§4« §rMain Menu\n§8Go back", FormImage.Type.PATH, "textures/ui/arrow_left");

            builder.validResultHandler((form, result) -> {
                if (result.clickedButtonId() == 0) {
                    openCreateTeamForm(player);
                } else {
                    openMainMenu(player);
                }
            });
            sendForm(player, builder);
        } else {
            DataManager.TeamData data = plugin.getTeamManager().getTeamData(teamName);
            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("§l§eTeam: " + teamName)
                    .content("§7Role: §f" + data.roles.getOrDefault(player.getUniqueId(), "MEMBER") + "\n§7Color: §f"
                            + data.color);

            builder.button("§l§e» §rTeam Info\n§8Stats & Members", FormImage.Type.PATH, "textures/items/paper");
            builder.button("§l§b» §rInvite Player\n§8Add to team", FormImage.Type.PATH, "textures/ui/plus");
            builder.button("§l§c» §rManage Members\n§8Kick/Promote/Demote", FormImage.Type.PATH,
                    "textures/items/iron_sword");
            builder.button("§l§a» §rTeam Base\n§8Teleport to HQ", FormImage.Type.PATH, "textures/items/fences");
            builder.button("§l§d» §rManage Allies\n§8Add/View Allies", FormImage.Type.PATH,
                    "textures/items/flower_pot_item");
            builder.button("§l§6» §rChange Color\n§8Set team style", FormImage.Type.PATH,
                    "textures/items/dye_powder_cyan");
            builder.button("§l§c» §rLeave Team\n§8Quit squad", FormImage.Type.PATH, "textures/ui/cancel");
            builder.button("§l§4« §rMain Menu\n§8Go back", FormImage.Type.PATH, "textures/ui/arrow_left");

            builder.validResultHandler((form, result) -> {
                switch (result.clickedButtonId()) {
                    case 0 -> player.performCommand("team info");
                    case 1 -> openPlayerSelection(player, "team invite", "Invite to Team");
                    case 2 -> player.sendMessage("§eUse §f/team kick/promote/demote <player> §eto manage members!");
                    case 3 -> openTeamBaseMenu(player); // Updated
                    case 4 -> player.performCommand("team ally");
                    case 5 -> openTeamColorForm(player);
                    case 6 -> {
                        player.performCommand("team leave");
                        openMainMenu(player);
                    }
                    case 7 -> openMainMenu(player);
                }
            });
            sendForm(player, builder);
        }
    }

    private void openTeamBaseMenu(Player player) { // New intermediate menu
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§eTeam Base")
                .content("§7Manage your team's headquarters.");

        builder.button("§l§a» §rTeleport to Base\n§8Warp to HQ", FormImage.Type.PATH, "textures/items/ender_pearl");
        builder.button("§l§c» §rSet Base Here\n§8Update location", FormImage.Type.PATH, "textures/ui/op");
        builder.button("§l§4« §rBack", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            switch (result.clickedButtonId()) {
                case 0 -> player.performCommand("team base");
                case 1 -> {
                    if (player.hasPermission("crosstpa.team.base.set")) {
                        player.performCommand("team setbase");
                    } else {
                        player.sendMessage("§cYou don't have permission to set the team base!");
                    }
                }
                case 2 -> openTeamMenu(player);
            }
        });
        sendForm(player, builder);
    }

    private void openCreateTeamForm(Player player) {
        CustomForm.Builder builder = CustomForm.builder()
                .title("§l§2Create Team")
                .input("§7Enter Team Name:", "e.g. Titans");

        builder.validResultHandler((form, result) -> {
            String name = result.next();
            if (name != null && !name.trim().isEmpty()) {
                player.performCommand("team create " + name);
            }
            openTeamMenu(player);
        });
        sendForm(player, builder);
    }

    private void openTeamColorForm(Player player) {
        CustomForm.Builder builder = CustomForm.builder()
                .title("§l§6Team Color")
                .dropdown("Select Color:",
                        Arrays.asList("red", "blue", "green", "yellow", "aqua", "white", "gray", "gold"));

        builder.validResultHandler((form, result) -> {
            int idx = result.next();
            String[] colors = { "red", "blue", "green", "yellow", "aqua", "white", "gray", "gold" };
            player.performCommand("team color " + colors[idx]);
            openTeamMenu(player);
        });
        sendForm(player, builder);
    }

    // --- FRIENDS ---

    public void openFriendsMenu(Player player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§dMy Friends")
                .content("§7Manage your social circle.");

        builder.button("§l§a» §rView Friends\n§8List all friends", FormImage.Type.PATH, "textures/ui/icon_steve");
        builder.button("§l§b» §rAdd Friend\n§8Invite a player", FormImage.Type.PATH, "textures/ui/plus");
        builder.button("§l§c» §rRemove Friend\n§8Unfriend player", FormImage.Type.PATH, "textures/ui/minus");
        builder.button("§l§4« §rMain Menu\n§8Go back", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            switch (result.clickedButtonId()) {
                case 0 -> player.sendMessage("§eUse /friend list for now!");
                case 1 -> openPlayerSelection(player, "friend add", "Add Friend");
                case 2 -> openPlayerSelection(player, "friend remove", "Remove Friend"); // Simplification, ideally list
                                                                                         // friends
                case 3 -> openMainMenu(player);
            }
        });
        sendForm(player, builder);
    }

    // --- BANKS ---

    public void openPersonalBankMenu(Player player) {
        int balance = plugin.getTeamManager().getMemberCoins(player.getUniqueId());
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§6Personal Bank")
                .content("§7Balance: §f" + balance + " §7Coins");

        builder.button("§l§b» §rTransfer\n§8Send to player", FormImage.Type.PATH, "textures/items/emerald");
        builder.button("§l§e» §rDeposit to Team\n§8Store in vault", FormImage.Type.PATH,
                "textures/items/chest_minecart");
        builder.button("§l§4« §rBack", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            switch (result.clickedButtonId()) {
                case 0 -> openTransferForm(player);
                case 1 -> openDepositForm(player); // TODO
                case 2 -> openMainMenu(player);
            }
        });
        sendForm(player, builder);
    }

    private void openTransferForm(Player player) {
        List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        CustomForm.Builder builder = CustomForm.builder()
                .title("§l§bTransfer Coins")
                .dropdown("Select Player:", players)
                .input("Amount:", "100");

        builder.validResultHandler((form, result) -> {
            int pIdx = result.next();
            String amountStr = result.next();
            if (pIdx >= 0 && pIdx < players.size() && amountStr != null) {
                player.performCommand("team transfer " + players.get(pIdx) + " " + amountStr);
            }
            openPersonalBankMenu(player);
        });
        sendForm(player, builder);
    }

    private void openDepositForm(Player player) { // Placeholder logic usually
        // Actually CrossTPA usually relies on commands, so:
        player.sendMessage("§eUse /team bank deposit while holding items!");
    }

    public void openTeamBankMenu(Player player) {
        openGenericBankMenu(player, "Team Bank", "team bank");
    }

    private void openGenericBankMenu(Player player, String title, String baseCmd) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§3" + title)
                .content("§7Manage shared assets.");

        builder.button("§l§a» §rDeposit Items\n§8Items in hand", FormImage.Type.PATH, "textures/ui/plus");
        builder.button("§l§c» §rWithdraw Coins\n§8Take currency", FormImage.Type.PATH, "textures/ui/minus");
        builder.button("§l§4« §rBack", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            switch (result.clickedButtonId()) {
                case 0 -> player.sendMessage("§eHold items and use /" + baseCmd + " deposit");
                case 1 -> openWithdrawForm(player, baseCmd);
                case 2 -> openMainMenu(player);
            }
        });
        sendForm(player, builder);
    }

    private void openWithdrawForm(Player player, String baseCmd) {
        CustomForm.Builder builder = CustomForm.builder()
                .title("§l§cWithdraw")
                .input("Amount to withdraw:", "50");

        builder.validResultHandler((form, result) -> {
            String amount = result.next();
            if (amount != null)
                player.performCommand(baseCmd + " withdraw " + amount);
            openMainMenu(player);
        });
        sendForm(player, builder);
    }

    // --- UTILS ---

    private void openPlayerSelection(Player player, boolean isTpaHere) {
        openPlayerSelection(player, isTpaHere ? "tpahere" : "tpa", isTpaHere ? "Request to Me" : "Teleport to Player");
    }

    private void openPlayerSelection(Player player, String commandPrefix, String title) {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != null && !p.getUniqueId().equals(player.getUniqueId()))
                .collect(Collectors.toList());

        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§8" + title)
                .content("§7Select a target:");

        if (players.isEmpty()) {
            builder.content("§cNo valid players found.");
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
                player.performCommand(commandPrefix + " " + target.getName());
            } else {
                openMainMenu(player);
            }
        });
        sendForm(player, builder);
    }

    public void openSecurityMenu(Player player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("§l§6Security")
                .content("§7Privacy Settings");

        builder.button("§l§e» §rToggle TPA\n§8On/Off", FormImage.Type.PATH, "textures/items/ender_eye");
        builder.button("§l§c» §rBlock List\n§8Manage blocked", FormImage.Type.PATH, "textures/ui/lock");
        builder.button("§l§8» §rMute List\n§8Manage muted", FormImage.Type.PATH, "textures/items/paper");
        builder.button("§l§4« §rBack", FormImage.Type.PATH, "textures/ui/arrow_left");

        builder.validResultHandler((form, result) -> {
            switch (result.clickedButtonId()) {
                case 0 -> player.performCommand("tpatoggle");
                case 1 -> openPlayerSelection(player, "tpablock", "Block Player"); // Simple alias
                case 2 -> openPlayerSelection(player, "tpamute", "Mute Player");
                case 3 -> openMainMenu(player);
            }
        });
        sendForm(player, builder);
    }

    private void sendForm(Player player, SimpleForm.Builder builder) {
        try {
            FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
        } catch (Exception e) {
            // Ignore
        }
    }

    private void sendForm(Player player, CustomForm.Builder builder) {
        try {
            FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
        } catch (Exception e) {
            // Ignore
        }
    }
}
