package net.exylia.exyliaTotems.command;

import net.exylia.exyliaTotems.ExyliaTotems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    private final ExyliaTotems plugin;

    public ReloadCommand(ExyliaTotems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.reloadConfigurationAsync();
        return true;
    }
}