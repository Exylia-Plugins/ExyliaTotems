package net.exylia.exyliaTotems.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import net.exylia.exyliaTotems.ExyliaTotems;

public class PacketEventsListener implements PacketListener {
    ExyliaTotems plugin;

    public PacketEventsListener(ExyliaTotems plugin) {
        this.plugin = plugin;
    }

    public void onPacketReceive(PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.HELD_ITEM_CHANGE)
            return;
        WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(e);
        this.plugin.getDeathManager().playerSlotChanged(e.getPlayer(), packet.getSlot());
    }
}
