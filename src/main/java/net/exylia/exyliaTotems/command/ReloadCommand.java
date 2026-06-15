package net.exylia.exyliaTotems.command;

import net.exylia.commons.v2.reload.api.ReloadAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    public ReloadCommand() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        ReloadAPI.getInstance().reloadAll(sender);
        return true;

    }
}