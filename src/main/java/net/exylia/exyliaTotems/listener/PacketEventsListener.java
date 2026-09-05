package net.exylia.exyliaTotems.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.exylia.lib.task.Tasks;
import net.exylia.exyliaTotems.ExyliaTotems;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PacketEventsListener implements PacketListener {
    private final ExyliaTotems plugin;

    public PacketEventsListener(ExyliaTotems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent e) {
        // Case 1: hotbar slot change (scroll, number keys)
        if (e.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(e);
            Player player = (Player) e.getPlayer();
            plugin.deaths().playerSlotChanged(player, packet.getSlot());
            return;
        }

        // Case 2: F key during normal play (quick swap with the offhand)
        if (e.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(e);
            if (packet.getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                Player player = (Player) e.getPlayer();
                plugin.deaths().playerOffhandSwitch(player);
            }
            return;
        }

        // Case 3: F key inside the inventory, or a direct drag to the offhand
        if (e.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(e);
            Player player = (Player) e.getPlayer();

            // F key inside the inventory (swap with the offhand)
            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.SWAP
                    && packet.getButton() == 40) {
                plugin.deaths().playerOffhandSwitch(player);
                return;
            }

            // Number keys 1-9 inside the inventory (swap with the hotbar)
            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.SWAP
                    && packet.getButton() >= 0 && packet.getButton() <= 8) {
                int targetSlot = packet.getButton();
                plugin.deaths().playerSlotChanged(player, targetSlot);
                return;
            }

            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.PICKUP
                    && packet.getButton() >= 0 && packet.getButton() <= 8) {
                int targetSlot = packet.getButton();
                plugin.deaths().playerSlotChanged(player, targetSlot);
                return;
            }

            // Direct click on slot 45 (offhand in the player inventory)
            if (packet.getSlot() == 45) {
                plugin.deaths().playerOffhandSwitch(player);
                return;
            }

            // Shift+Click to move items automatically
            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.QUICK_MOVE) {
                // Check whether the item being moved is a totem
                if (packet.getSlot() >= 9) { // Only if it comes from the main inventory (not the hotbar)
                    org.bukkit.inventory.ItemStack clickedItem = player.getInventory().getItem(packet.getSlot());
                    if (clickedItem != null && clickedItem.getType() == Material.TOTEM_OF_UNDYING) {

                        // Snapshot the current hotbar BEFORE the move
                        boolean[] hotbarHadTotem = new boolean[9];
                        for (int i = 0; i < 9; i++) {
                            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
                            hotbarHadTotem[i] = (item != null && item.getType() == Material.TOTEM_OF_UNDYING);
                        }

                        // After the move, check which slot changed
                        Tasks.of(plugin).runAtEntityLater(player, 1L, () -> {
                            for (int i = 0; i < 9; i++) {
                                org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
                                boolean hasTotemNow = (item != null && item.getType() == Material.TOTEM_OF_UNDYING);

                                // No totem before and one now = the totem moved here
                                if (!hotbarHadTotem[i] && hasTotemNow) {
                                    plugin.deaths().playerSlotChanged(player, i);
                                    return;
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}
