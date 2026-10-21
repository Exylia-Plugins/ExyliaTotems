package net.exylia.exyliaTotems;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.exylia.exyliaTotems.bootstrap.ExyliaLibMissing;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Bukkit entry point, kept free of ExyliaLib types so it still runs when ExyliaLib is missing.
 *
 * <p>Without ExyliaLib it says where to get the library and disables itself until the restart; with it, it
 * hands over to {@link ExyliaTotems}.
 */
public final class ExyliaTotemsPlugin extends JavaPlugin {

    private ExyliaTotems core;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        Plugin lib = getServer().getPluginManager().getPlugin("ExyliaLib");
        if (lib == null || !lib.isEnabled()) {
            ExyliaLibMissing.report(this);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        core = new ExyliaTotems();
        core.start(this);
    }

    @Override
    public void onDisable() {
        if (core != null) core.shutdown();
        core = null;
    }
}
