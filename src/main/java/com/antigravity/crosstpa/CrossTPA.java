package com.antigravity.crosstpa;

import com.antigravity.crosstpa.commands.TPACommand;
import com.antigravity.crosstpa.commands.HomeCommand;
import com.antigravity.crosstpa.commands.FriendCommand;
import com.antigravity.crosstpa.commands.TeamCommand;
import com.antigravity.crosstpa.commands.TeamBaseCommand;
import com.antigravity.crosstpa.managers.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class CrossTPA extends JavaPlugin {

    private static CrossTPA instance;
    private RequestManager requestManager;
    private HistoryManager historyManager;
    private DataManager dataManager;
    private JavaGuiManager javaGuiManager;
    private EconomyManager economyManager;
    private MenuItemManager menuItemManager;
    private HomeManager homeManager;
    private FriendManager friendManager;
    private TeamManager teamManager;
    private CoinManager coinManager;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.miniMessage = MiniMessage.miniMessage();
        this.dataManager = new DataManager(this);
        this.historyManager = new HistoryManager(this);
        this.requestManager = new RequestManager(this);
        this.javaGuiManager = new JavaGuiManager(this);
        this.economyManager = new EconomyManager(this);
        this.menuItemManager = new MenuItemManager(this);
        this.homeManager = new HomeManager(this);
        this.friendManager = new FriendManager(this);
        this.teamManager = new TeamManager(this);
        this.coinManager = new CoinManager(this);

        requestManager.loadData();
        homeManager.loadData();
        friendManager.loadData();
        teamManager.loadData();

        registerCommands();
        registerEvents();

        getLogger().info("CrossTPA has been enabled successfully!");
    }

    private void registerEvents() {
        // JavaGuiManager registers itself in constructor
        getServer().getPluginManager().registerEvents(new com.antigravity.crosstpa.listeners.TeamListener(this), this);
        getServer().getPluginManager().registerEvents(new com.antigravity.crosstpa.listeners.ItemClickListener(this),
                this);
    }

    @Override
    public void onDisable() {
        if (requestManager != null) {
            requestManager.saveData();
            requestManager.cleanupTasks();
        }
        if (homeManager != null)
            homeManager.saveData();
        if (friendManager != null)
            friendManager.saveData();
        if (teamManager != null)
            teamManager.saveData();
        getLogger().info("CrossTPA has been disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        dataManager.reloadConfig();
        requestManager.cleanupTasks();
        requestManager.loadData();
        homeManager.loadData();
        friendManager.loadData();
        teamManager.loadData();
    }

    private void registerCommands() {
        TPACommand tpaCommand = new TPACommand(this);
        String[] cmds = { "tpa", "tpahere", "tpaccept", "tpreject", "tpdeny", "tpcancel", "tpablock",
                "tpaunblock", "tpatoggle", "tpahistory", "tpamenu", "tpaitem", "tpaall", "tpaback",
                "tpacooldown", "tpainfo", "tpamute", "tpaunmute", "crosstpa" };

        for (String cmd : cmds) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(tpaCommand);
                getCommand(cmd).setTabCompleter(tpaCommand);
            }
        }

        HomeCommand homeCommand = new HomeCommand(this);
        if (getCommand("home") != null)
            getCommand("home").setExecutor(homeCommand);
        if (getCommand("sethome") != null)
            getCommand("sethome").setExecutor(homeCommand);
        if (getCommand("delhome") != null)
            getCommand("delhome").setExecutor(homeCommand);

        FriendCommand friendCommand = new FriendCommand(this);
        if (getCommand("friend") != null)
            getCommand("friend").setExecutor(friendCommand);

        TeamCommand teamCommand = new TeamCommand(this);
        if (getCommand("team") != null)
            getCommand("team").setExecutor(teamCommand);

        TeamBaseCommand teamBaseCommand = new TeamBaseCommand(this);
        if (getCommand("tpateambase") != null)
            getCommand("tpateambase").setExecutor(teamBaseCommand);
    }

    public static CrossTPA getInstance() {
        return instance;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public JavaGuiManager getJavaGuiManager() {
        return javaGuiManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public MenuItemManager getMenuItemManager() {
        return menuItemManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public CoinManager getCoinManager() {
        return coinManager;
    }
}
