package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.*;

public class RequestManager implements Listener {

    private final CrossTPA plugin;
    private final Map<UUID, UUID> incomingRequests = new HashMap<>(); // Receiver -> Sender
    private final Map<UUID, Boolean> isTpHere = new HashMap<>(); // Receiver -> isHere
    private final Map<UUID, Long> requestTime = new HashMap<>(); // Receiver -> Timestamp
    private final Map<UUID, Set<UUID>> blockLists = new HashMap<>();
    private final Map<UUID, Set<UUID>> muteLists = new HashMap<>();
    private final Set<UUID> disabledTpa = new HashSet<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Location> backLocations = new HashMap<>();
    private final Map<UUID, Long> protectionExpiry = new HashMap<>();

    public RequestManager(CrossTPA plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadData() {
        plugin.getDataManager().loadUserLists(blockLists, muteLists, disabledTpa);
    }

    public void saveData() {
        plugin.getDataManager().saveUserLists(blockLists, muteLists, disabledTpa);
    }

    public void cleanupTasks() {
        activeTasks.values().forEach(BukkitRunnable::cancel);
        activeTasks.clear();

        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null)
                p.hideBossBar(entry.getValue());
        }
        activeBossBars.clear();

        // Refund all pending requests on shutdown/reload
        for (Map.Entry<UUID, UUID> entry : incomingRequests.entrySet()) {
            Player sender = Bukkit.getPlayer(entry.getValue());
            if (sender != null) {
                handleRefund(sender, isTpHere.getOrDefault(entry.getKey(), false));
            }
        }
        incomingRequests.clear();
        isTpHere.clear();
    }

    public void sendRequest(Player sender, Player receiver, boolean here) {
        String prefix = getPrefix();

        if (plugin.getConfig().getStringList("worlds.blacklist").contains(sender.getWorld().getName())) {
            sender.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + plugin.getConfig().getString("messages.world-blacklisted")));
            return;
        }

        if (!plugin.getConfig().getBoolean("worlds.allow-cross-world", true)
                && !sender.getWorld().equals(receiver.getWorld())) {
            sender.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + plugin.getConfig().getString("messages.cross-world-disabled")));
            return;
        }

        int maxDist = plugin.getConfig().getInt("settings.max-distance", 0);
        if (maxDist > 0 && sender.getWorld().equals(receiver.getWorld())) {
            if (sender.getLocation().distance(receiver.getLocation()) > maxDist) {
                sender.sendMessage(plugin.getMiniMessage().deserialize(prefix + plugin.getConfig()
                        .getString("messages.too-far").replace("{distance}", String.valueOf(maxDist))));
                return;
            }
        }

        long cooldownTime = getCooldown(sender);
        if (cooldownTime > 0) {
            sender.sendMessage(plugin.getMiniMessage().deserialize(prefix + plugin.getConfig()
                    .getString("messages.tp-cooldown").replace("{time}", String.valueOf(cooldownTime))));
            return;
        }

        if (plugin.getConfig().getBoolean("settings.enable-blocking", true) && isBlocked(receiver, sender)) {
            sender.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + plugin.getConfig().getString("messages.target-blocking")));
            return;
        }

        if (disabledTpa.contains(receiver.getUniqueId())) {
            sender.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + plugin.getConfig().getString("messages.target-disabled")));
            return;
        }

        // Refund previous sender if this target had a pending request
        if (incomingRequests.containsKey(receiver.getUniqueId())) {
            UUID oldSenderUuid = incomingRequests.get(receiver.getUniqueId());
            Player oldSender = Bukkit.getPlayer(oldSenderUuid);
            if (oldSender != null) {
                handleRefund(oldSender, isTpHere.getOrDefault(receiver.getUniqueId(), false));
                oldSender.sendMessage(plugin.getMiniMessage()
                        .deserialize(prefix + plugin.getConfig().getString("messages.tp-cancelled")));
            }
        }

        if (plugin.getConfig().getBoolean("settings.enable-admin-spy", true)) {
            notifyAdmins(sender.getName(), receiver.getName(), here ? "TPAHERE" : "TPA");
        }

        // Economy Check & Charge
        double cost = plugin.getConfig().getDouble(here ? "economy.tpahere-cost" : "economy.tpa-cost", 0.0);
        if (plugin.getEconomyManager().isEnabled() && cost > 0) {
            if (!plugin.getEconomyManager().hasBalance(sender, cost)) {
                sender.sendMessage(plugin.getMiniMessage().deserialize(prefix + plugin.getConfig()
                        .getString("messages.insufficient-funds").replace("{cost}", String.valueOf(cost))));
                return;
            }
            plugin.getEconomyManager().withdraw(sender, cost);
        }

        if (plugin.getConfig().getBoolean("settings.enable-muting", true) && isMuted(receiver, sender)) {
            sender.sendMessage(plugin.getMiniMessage().deserialize(prefix
                    + plugin.getConfig().getString("messages.tpa-sent").replace("{player}", receiver.getName())));
            playSound(sender, "request-sent-sound");
            return;
        }

        long now = System.currentTimeMillis();
        incomingRequests.put(receiver.getUniqueId(), sender.getUniqueId());
        isTpHere.put(receiver.getUniqueId(), here);
        requestTime.put(receiver.getUniqueId(), now);

        sender.sendMessage(plugin.getMiniMessage().deserialize(
                prefix + plugin.getConfig().getString("messages.tpa-sent").replace("{player}", receiver.getName())));
        playSound(sender, "request-sent-sound");

        if (isBedrock(receiver) && plugin.getConfig().getBoolean("settings.use-bedrock-forms", true)) {
            sendBedrockForm(receiver, sender, here);
        } else {
            String receivedKey = here ? "messages.tpahere-received" : "messages.tpa-received";
            receiver.sendMessage(plugin.getMiniMessage().deserialize(
                    prefix + plugin.getConfig().getString(receivedKey).replace("{player}", sender.getName())));
        }
        playSound(receiver, "request-received-sound");

        int expiryTime = plugin.getConfig().getInt("settings.request-expiry", 60);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (incomingRequests.containsKey(receiver.getUniqueId())
                        && incomingRequests.get(receiver.getUniqueId()).equals(sender.getUniqueId())
                        && requestTime.getOrDefault(receiver.getUniqueId(), 0L) == now) {
                    incomingRequests.remove(receiver.getUniqueId());
                    boolean wasHere = isTpHere.remove(receiver.getUniqueId());
                    requestTime.remove(receiver.getUniqueId());
                    handleRefund(sender, wasHere);
                    if (plugin.getConfig().getBoolean("settings.notify-expiry", true) && sender.isOnline()) {
                        sender.sendMessage(plugin.getMiniMessage().deserialize(prefix + plugin.getConfig()
                                .getString("messages.request-expired").replace("{player}", receiver.getName())));
                    }
                }
            }
        }.runTaskLater(plugin, expiryTime * 20L);
    }

    public void acceptRequest(Player receiver) {
        UUID senderUuid = incomingRequests.get(receiver.getUniqueId());
        if (senderUuid == null)
            return;
        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender == null)
            return;

        boolean here = isTpHere.getOrDefault(receiver.getUniqueId(), false);
        Player toTeleport = here ? receiver : sender;
        Player target = here ? sender : receiver;

        incomingRequests.remove(receiver.getUniqueId());
        isTpHere.remove(receiver.getUniqueId());
        requestTime.remove(receiver.getUniqueId());

        startTeleportProcess(toTeleport, target);
        cooldowns.put(sender.getUniqueId(), System.currentTimeMillis());
        plugin.getHistoryManager().logHistory(sender, receiver, "ACCEPTED");
    }

    private void startTeleportProcess(Player player, Player target) {
        String prefix = getPrefix();
        int delay = player.hasPermission("crosstpa.bypass.delay") ? 0
                : plugin.getConfig().getInt("settings.teleport-delay", 3);

        cleanupTask(player.getUniqueId());

        if (delay <= 0) {
            finalTeleport(player, target);
            return;
        }

        player.sendMessage(plugin.getMiniMessage().deserialize(prefix
                + plugin.getConfig().getString("messages.tp-delay-chat").replace("{time}", String.valueOf(delay))));
        Location startLoc = player.getLocation().clone();

        BossBar bar = null;
        if (plugin.getConfig().getBoolean("settings.use-bossbar", true)) {
            try {
                bar = BossBar.bossBar(
                        plugin.getMiniMessage()
                                .deserialize(plugin.getConfig().getString("effects.bossbar.title").replace("{time}",
                                        String.valueOf(delay))),
                        1.0f,
                        BossBar.Color.valueOf(plugin.getConfig().getString("effects.bossbar.color", "BLUE")),
                        BossBar.Overlay.valueOf(plugin.getConfig().getString("effects.bossbar.style", "PROGRESS")
                                .replace("SOLID", "PROGRESS")));
                player.showBossBar(bar);
                activeBossBars.put(player.getUniqueId(), bar);
            } catch (Exception ignored) {
            }
        }

        final BossBar finalBar = bar;
        BukkitRunnable task = new BukkitRunnable() {
            int remainingTicks = delay * 20;

            @Override
            public void run() {
                if (!player.isOnline() || !target.isOnline()) {
                    cleanup();
                    return;
                }

                if (plugin.getConfig().getBoolean("settings.cancel-on-move", true)) {
                    if (!player.getWorld().equals(startLoc.getWorld())
                            || player.getLocation().distanceSquared(startLoc) > 0.1) {
                        player.sendMessage(plugin.getMiniMessage()
                                .deserialize(prefix + plugin.getConfig().getString("messages.tp-moved")));
                        playSound(player, "teleport-cancel-sound");
                        cleanup();
                        return;
                    }
                }

                if (remainingTicks % 20 == 0) {
                    int seconds = remainingTicks / 20;
                    if (finalBar != null) {
                        finalBar.name(plugin.getMiniMessage().deserialize(plugin.getConfig()
                                .getString("effects.bossbar.title").replace("{time}", String.valueOf(seconds))));
                        finalBar.progress((float) seconds / delay);
                    }
                    if (plugin.getConfig().getBoolean("settings.use-actionbar", true)) {
                        player.sendActionBar(plugin.getMiniMessage().deserialize(plugin.getConfig()
                                .getString("effects.bossbar.title").replace("{time}", String.valueOf(seconds))));
                    }
                    spawnParticle(player.getLocation(), "delay-particle");
                }

                if (remainingTicks <= 0) {
                    finalTeleport(player, target);
                    cleanup();
                    return;
                }
                remainingTicks--;
            }

            private void cleanup() {
                cleanupTask(player.getUniqueId());
            }
        };
        activeTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, 1L);
    }

    private void cleanupTask(UUID uuid) {
        BukkitRunnable task = activeTasks.remove(uuid);
        if (task != null)
            task.cancel();
        BossBar bar = activeBossBars.remove(uuid);
        if (bar != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.hideBossBar(bar);
        }
    }

    public boolean isLocationSafe(Location loc) {
        if (!plugin.getConfig().getBoolean("settings.hazard-prevention", true))
            return true;

        Material block = loc.getBlock().getType();
        Material headBlock = loc.clone().add(0, 1, 0).getBlock().getType();
        Material footBlock = loc.getBlock().getType();

        boolean isWater = block == Material.WATER;
        boolean avoidWater = plugin.getConfig().getBoolean("settings.block-water-tp", true);

        boolean dangerous = block == Material.LAVA || block == Material.VOID_AIR
                || (block == Material.CAVE_AIR && loc.getY() < -60)
                || block == Material.FIRE || block == Material.SOUL_FIRE
                || footBlock.isSolid() || headBlock.isSolid()
                || (avoidWater && isWater);

        return !dangerous;
    }

    private void finalTeleport(Player player, Player target) {
        String prefix = getPrefix();

        if (!isLocationSafe(target.getLocation())) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    prefix + "<red>Target is in a dangerous location (Lava/Water/Void)! Teleport cancelled.</red>"));
            return;
        }

        backLocations.put(player.getUniqueId(), player.getLocation().clone());
        spawnParticle(player.getLocation(), "teleport-particle");
        player.teleport(target.getLocation());
        player.sendMessage(
                plugin.getMiniMessage().deserialize(prefix + plugin.getConfig().getString("messages.tp-success")));
        playSound(player, "teleport-success-sound");
        spawnParticle(player.getLocation(), "teleport-particle");

        if (plugin.getConfig().getBoolean("safety.protection-enabled", true)) {
            int time = plugin.getConfig().getInt("safety.protection-time", 5);
            protectionExpiry.put(player.getUniqueId(), System.currentTimeMillis() + (time * 1000L));
            player.sendMessage(plugin.getMiniMessage().deserialize(prefix + plugin.getConfig()
                    .getString("messages.protection-started").replace("{time}", String.valueOf(time))));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (protectionExpiry.containsKey(player.getUniqueId())
                            && System.currentTimeMillis() >= protectionExpiry.get(player.getUniqueId())) {
                        protectionExpiry.remove(player.getUniqueId());
                        if (player.isOnline())
                            player.sendMessage(plugin.getMiniMessage()
                                    .deserialize(prefix + plugin.getConfig().getString("messages.protection-expired")));
                    }
                }
            }.runTaskLater(plugin, time * 20L);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        if (protectionExpiry.containsKey(player.getUniqueId())) {
            if (System.currentTimeMillis() < protectionExpiry.get(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            } else {
                protectionExpiry.remove(player.getUniqueId());
            }
        }

        if (plugin.getConfig().getBoolean("settings.cancel-on-damage", true)
                && activeTasks.containsKey(player.getUniqueId())) {
            cancelOnDamage(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // If receiver had a pending request, refund the sender
        if (incomingRequests.containsKey(uuid)) {
            UUID senderUuid = incomingRequests.remove(uuid);
            boolean wasHere = isTpHere.remove(uuid);
            Player sender = Bukkit.getPlayer(senderUuid);
            if (sender != null) {
                sender.sendMessage(plugin.getMiniMessage()
                        .deserialize(getPrefix() + plugin.getConfig().getString("messages.tp-cancelled")));
                handleRefund(sender, wasHere);
            }
        }

        // If sender had outgoing requests, cleanup tracking
        Iterator<Map.Entry<UUID, UUID>> it = incomingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            if (entry.getValue().equals(uuid)) {
                isTpHere.remove(entry.getKey());
                it.remove();
            }
        }

        cooldowns.remove(uuid);
        protectionExpiry.remove(uuid);
        cleanupTask(uuid);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
                || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN
                || event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (plugin.getConfig().getBoolean("settings.cancel-on-teleport", true)
                    && activeTasks.containsKey(event.getPlayer().getUniqueId())) {
                cancelOnDamage(event.getPlayer());
            }
        }
    }

    public void teleportBack(Player player) {
        String prefix = getPrefix();
        Location loc = backLocations.get(player.getUniqueId());
        if (loc == null) {
            player.sendMessage(
                    plugin.getMiniMessage().deserialize(prefix + plugin.getConfig().getString("messages.back-empty")));
            return;
        }

        if (!plugin.getConfig().getBoolean("worlds.back-cross-world", true)
                && !player.getWorld().equals(loc.getWorld())) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(prefix + plugin.getConfig().getString("messages.cross-world-disabled")));
            return;
        }

        // Apply economy charge AFTER world validation
        double cost = plugin.getConfig().getDouble("economy.back-cost", 0.0);
        if (plugin.getEconomyManager().isEnabled() && cost > 0) {
            if (!plugin.getEconomyManager().hasBalance(player, cost)) {
                player.sendMessage(plugin.getMiniMessage().deserialize(prefix + plugin.getConfig()
                        .getString("messages.insufficient-funds").replace("{cost}", String.valueOf(cost))));
                return;
            }
            plugin.getEconomyManager().withdraw(player, cost);
        }

        player.teleport(loc);
        player.sendMessage(
                plugin.getMiniMessage().deserialize(prefix + plugin.getConfig().getString("messages.tp-success")));
        playSound(player, "teleport-success-sound");
    }

    public void cancelOnDamage(Player player) {
        cleanupTask(player.getUniqueId());
        player.sendMessage(
                plugin.getMiniMessage().deserialize(getPrefix() + plugin.getConfig().getString("messages.tp-damaged")));
        playSound(player, "teleport-cancel-sound");
    }

    private void playSound(Player player, String configKey) {
        try {
            String soundName = plugin.getConfig().getString("effects." + configKey);
            float vol = (float) plugin.getConfig().getDouble("effects.sound-volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("effects.sound-pitch", 1.0);
            player.playSound(player.getLocation(), Sound.valueOf(soundName), vol, pitch);
        } catch (Exception ignored) {
        }
    }

    private void spawnParticle(Location loc, String configKey) {
        try {
            String partName = plugin.getConfig().getString("effects." + configKey);
            int count = plugin.getConfig().getInt("effects.particle-count", 30);
            loc.getWorld().spawnParticle(Particle.valueOf(partName), loc.add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.05);
        } catch (Exception ignored) {
        }
    }

    private void notifyAdmins(String sender, String receiver, String action) {
        Component spyMsg = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("messages.admin-spy",
                "<gray>[Admin Spy] {sender} -> {receiver}: {action}</gray>")
                .replace("{sender}", sender).replace("{receiver}", receiver).replace("{action}", action));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p != null && p.hasPermission("crosstpa.admin.spy")) {
                p.sendMessage(spyMsg);
            }
        }
    }

    private String getPrefix() {
        return plugin.getConfig().getBoolean("settings.prefix-enabled", true)
                ? plugin.getConfig().getString("messages.prefix", "<gradient:#4facfe:#00f2fe>[CrossTPA]</gradient> ")
                : "";
    }

    public long getCooldown(Player player) {
        if (player.hasPermission("crosstpa.admin.bypass") || player.hasPermission("crosstpa.bypass.cooldown"))
            return 0;
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long diff = (System.currentTimeMillis() - last) / 1000;
        return Math.max(0, plugin.getConfig().getLong("settings.teleport-cooldown") - diff);
    }

    public void sendInfo(Player player) {
        UUID senderUuid = incomingRequests.get(player.getUniqueId());
        if (senderUuid == null) {
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(getPrefix() + plugin.getConfig().getString("messages.info-none")));
            return;
        }
        Player sender = Bukkit.getPlayer(senderUuid);
        String name = sender != null ? sender.getName() : "Offline Player";
        String msg = getPrefix() + plugin.getConfig().getString("messages.info-entry")
                .replace("{player}", name)
                .replace("{type}", isTpHere.getOrDefault(player.getUniqueId(), false) ? "TPAHERE" : "TPA");
        player.sendMessage(plugin.getMiniMessage().deserialize(msg));
    }

    public void blockPlayer(Player player, Player target) {
        blockLists.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(target.getUniqueId());
        player.sendMessage(plugin.getMiniMessage().deserialize(getPrefix()
                + plugin.getConfig().getString("messages.player-blocked").replace("{player}", target.getName())));
    }

    public void unblockPlayer(Player player, String name) {
        Set<UUID> list = blockLists.get(player.getUniqueId());
        if (list != null) {
            list.removeIf(u -> {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(u);
                if (op.getName() != null && op.getName().equalsIgnoreCase(name)) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(getPrefix()
                            + plugin.getConfig().getString("messages.player-unblocked").replace("{player}", name)));
                    return true;
                }
                return false;
            });
        }
    }

    public void mutePlayer(Player player, Player target) {
        muteLists.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(target.getUniqueId());
        player.sendMessage(plugin.getMiniMessage().deserialize(getPrefix()
                + plugin.getConfig().getString("messages.player-muted").replace("{player}", target.getName())));
    }

    public void unmutePlayer(Player player, String name) {
        Set<UUID> list = muteLists.get(player.getUniqueId());
        if (list != null) {
            list.removeIf(u -> {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(u);
                if (op.getName() != null && op.getName().equalsIgnoreCase(name)) {
                    player.sendMessage(plugin.getMiniMessage().deserialize(getPrefix()
                            + plugin.getConfig().getString("messages.player-unmuted").replace("{player}", name)));
                    return true;
                }
                return false;
            });
        }
    }

    public void toggleTpa(Player player) {
        if (disabledTpa.contains(player.getUniqueId())) {
            disabledTpa.remove(player.getUniqueId());
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(getPrefix() + plugin.getConfig().getString("messages.tpa-enabled")));
        } else {
            disabledTpa.add(player.getUniqueId());
            player.sendMessage(plugin.getMiniMessage()
                    .deserialize(getPrefix() + plugin.getConfig().getString("messages.tpa-disabled")));
        }
    }

    public void rejectRequest(Player receiver) {
        UUID sUuid = incomingRequests.remove(receiver.getUniqueId());
        if (sUuid != null) {
            Player s = Bukkit.getPlayer(sUuid);
            boolean wasHere = isTpHere.remove(receiver.getUniqueId());
            if (s != null) {
                s.sendMessage(plugin.getMiniMessage()
                        .deserialize(getPrefix() + plugin.getConfig().getString("messages.tp-rejected")));
                handleRefund(s, wasHere);
            }
            receiver.sendMessage(plugin.getMiniMessage()
                    .deserialize(getPrefix() + plugin.getConfig().getString("messages.tp-rejected")));
            plugin.getHistoryManager().logHistory(s, receiver, "REJECTED");
        }
    }

    public void cancelRequest(Player sender) {
        Iterator<Map.Entry<UUID, UUID>> it = incomingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            if (entry.getValue().equals(sender.getUniqueId())) {
                boolean wasHere = isTpHere.remove(entry.getKey());
                handleRefund(sender, wasHere);
                sender.sendMessage(plugin.getMiniMessage()
                        .deserialize(getPrefix() + plugin.getConfig().getString("messages.tp-cancelled")));
                it.remove();
            }
        }
    }

    private void handleRefund(Player player, boolean here) {
        if (!plugin.getConfig().getBoolean("economy.refund-on-deny", true))
            return;
        double cost = plugin.getConfig().getDouble(here ? "economy.tpahere-cost" : "economy.tpa-cost", 0.0);
        if (plugin.getEconomyManager().isEnabled() && cost > 0) {
            plugin.getEconomyManager().deposit(player, cost);
        }
    }

    public Set<UUID> getBlockList(UUID playerUuid) {
        return blockLists.getOrDefault(playerUuid, Collections.emptySet());
    }

    public Set<UUID> getMuteList(UUID playerUuid) {
        return muteLists.getOrDefault(playerUuid, Collections.emptySet());
    }

    private boolean isBlocked(Player r, Player s) {
        return blockLists.getOrDefault(r.getUniqueId(), Collections.emptySet()).contains(s.getUniqueId());
    }

    private boolean isMuted(Player r, Player s) {
        return muteLists.getOrDefault(r.getUniqueId(), Collections.emptySet()).contains(s.getUniqueId());
    }

    private boolean isBedrock(Player p) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    private void sendBedrockForm(Player r, Player s, boolean h) {
        try {
            FloodgateApi.getInstance().getPlayer(r.getUniqueId()).sendForm(SimpleForm.builder()
                    .title("§l§3Incoming Teleport Request")
                    .content("§7Player §f" + s.getName() + "§7 "
                            + (h ? "wants you to teleport to them." : "wants to teleport to you.") +
                            "\n\n§eDo you want to accept?")
                    .button("§l§2ACCEPT", FormImage.Type.PATH, "textures/ui/confirm")
                    .button("§l§cDENY", FormImage.Type.PATH, "textures/ui/cancel")
                    .validResultHandler((f, res) -> {
                        if (res.clickedButtonId() == 0)
                            r.performCommand("tpaccept");
                        else
                            r.performCommand("tpreject");
                    })
                    .build());
        } catch (Exception ignored) {
        }
    }
}
