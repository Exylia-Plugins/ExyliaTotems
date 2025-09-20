package net.exylia.exyliaTotems.command;

import net.exylia.commons.utils.visuals.MessageUtils;
import net.exylia.exyliaTotems.ExyliaTotems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static net.exylia.commons.ReloadResult.sendReloadResult;

public class ReloadCommand implements CommandExecutor {
    private final ExyliaTotems plugin;

    public ReloadCommand(ExyliaTotems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.reloadConfigurationAsync()
                .thenCompose(configResult -> configResult.isSuccess() ?
                        plugin.reloadPluginAsync() : CompletableFuture.completedFuture(configResult))
                .thenAccept(finalResult ->
                        Bukkit.getScheduler().runTask(plugin, () -> sendReloadResult(sender, finalResult)));
        return true;

    }
}