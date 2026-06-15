package net.exylia.exyliaTotems;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.exyliaTotems.command.ReloadCommand;
import net.exylia.exyliaTotems.listener.PacketEventsListener;
import net.exylia.exyliaTotems.listener.BukkitPlayerListener;
import net.exylia.exyliaTotems.managers.DeathManager;
import net.exylia.commons.ExyliaPlugin;

@Getter
public final class ExyliaTotems extends ExyliaPlugin {
    private DeathManager deathManager;

    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(this), PacketListenerPriority.LOWEST);
    }

    @Override
    protected void onExyliaEnable() {
        DebugAPI.init(this);

        PacketEvents.getAPI().init();
        this.deathManager = new DeathManager();
        new BukkitPlayerListener(this);

        getCommand("exyliatotem-reload").setExecutor(new ReloadCommand());
    }

    @Override
    protected void onExyliaDisable() {
        PacketEvents.getAPI().terminate();
    }
}