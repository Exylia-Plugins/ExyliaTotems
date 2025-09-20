package net.exylia.exyliaTotems;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaTotems.command.ReloadCommand;
import net.exylia.exyliaTotems.configs.MainConfig;
import net.exylia.exyliaTotems.configs.Messages;
import net.exylia.exyliaTotems.listener.PacketEventsListener;
import net.exylia.exyliaTotems.listener.BukkitPlayerListener;
import net.exylia.exyliaTotems.managers.DeathManager;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.config.ConfigBase;

@Getter
public final class ExyliaTotems extends ExyliaPlugin {
    private DeathManager deathManager;

    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(this), PacketListenerPriority.LOWEST);
    }

    @Override
    public int getProductID() {
        return 303;
    }

    @Override
    protected void onExyliaEnable() {
        DebugUtils.init(this);

        PacketEvents.getAPI().init();
        this.deathManager = new DeathManager(this);
        new BukkitPlayerListener(this);

        getCommand("exyliatotem-reload").setExecutor(new ReloadCommand(this));
    }

    @Override
    protected void onExyliaDisable() {
        PacketEvents.getAPI().terminate();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<? extends ConfigBase>[] getConfigurationClasses() {
        return new Class[]{
                MainConfig.class,
                Messages.class
        };
    }
}