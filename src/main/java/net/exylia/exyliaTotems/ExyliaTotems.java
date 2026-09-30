package net.exylia.exyliaTotems;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.exylia.lib.config.ConfigFile;
import net.exylia.lib.config.Configs;
import net.exylia.lib.debug.Debug;
import net.exylia.lib.reload.Reloads;
import net.exylia.exyliaTotems.command.ReloadCommand;
import net.exylia.exyliaTotems.configs.TotemMessages;
import net.exylia.exyliaTotems.configs.TotemSettings;
import net.exylia.exyliaTotems.listener.BukkitPlayerListener;
import net.exylia.exyliaTotems.listener.PacketEventsListener;
import net.exylia.exyliaTotems.managers.DeathManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Honours a totem the player reached for a moment too late.
 *
 * <p>A player on a bad connection dies on the server before their swap packet
 * arrives. Their death is held open for a few ticks, and what they did in that
 * window decides whether the totem counts.
 */
public final class ExyliaTotems extends JavaPlugin {

    private Debug debug;
    private ConfigFile<TotemSettings> settings;
    private ConfigFile<TotemMessages> messages;
    private DeathManager deaths;
    private Reloads reloads;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager()
                .registerListener(new PacketEventsListener(this), PacketListenerPriority.LOWEST);
    }

    @Override
    public void onEnable() {
        debug = Debug.of(this);
        settings = Configs.define(this, "config", TotemSettings.class)
                .version(2)
                .migration(1, TotemSettings.MIGRATION_FROM_1)
                .load();
        messages = Configs.define(this, "messages", TotemMessages.class).translated().load();

        PacketEvents.getAPI().init();
        deaths = new DeathManager(this);
        new BukkitPlayerListener(this);

        PluginCommand command = getCommand("exyliatotem-reload");
        if (command != null) {
            command.setExecutor(new ReloadCommand(this));
        } else {
            debug.warn("Command exyliatotem-reload is missing from plugin.yml and was not registered");
        }

        reloads = Reloads.of(this).step("configs", () -> Configs.reloadAll(this));

        debug.motd();
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public Debug debug() { return debug; }
    public TotemSettings settings() { return settings.get(); }
    public TotemMessages messages() { return messages.get(); }
    public DeathManager deaths() { return deaths; }
    public void reload(CommandSender sender) { reloads.run(sender); }
}
