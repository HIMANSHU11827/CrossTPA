package com.antigravity.crosstpa.managers;

import com.antigravity.crosstpa.CrossTPA;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final CrossTPA plugin;
    private Economy econ = null;

    public EconomyManager(CrossTPA plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null)
            return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return;
        econ = rsp.getProvider();
    }

    public boolean isEnabled() {
        return econ != null && plugin.getConfig().getBoolean("economy.enabled", false);
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled())
            return true;
        return econ.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!isEnabled() || amount <= 0)
            return true;
        if (!econ.has(player, amount))
            return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(Player player, double amount) {
        if (!isEnabled() || amount <= 0)
            return;
        econ.depositPlayer(player, amount);
    }
}
