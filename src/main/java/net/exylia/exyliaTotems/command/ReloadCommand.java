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

    /**
     * {@inheritDoc}
     *
     * <p>Reloads this plugin only. Recolouring every plugin on the server is
     * what {@code /exylialib reload} is for.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        plugin.reload(sender);
        return true;
    }
}
