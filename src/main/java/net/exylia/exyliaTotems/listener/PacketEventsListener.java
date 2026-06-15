package net.exylia.exyliaTotems.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.exyliaTotems.ExyliaTotems;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class PacketEventsListener implements PacketListener {
    private final ExyliaTotems plugin;

    public PacketEventsListener(ExyliaTotems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent e) {
        // Caso 1: cambio de slot en hotbar (scroll, números)
        if (e.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(e);
            Player player = (Player) e.getPlayer();
            DebugAPI.logPluginDebug("Player " + player.getName() + " changed hotbar slot to: " + packet.getSlot());
            plugin.getDeathManager().playerSlotChanged(player, packet.getSlot());
            return;
        }

        // Caso 2: tecla F en juego normal (swap rápido con offhand)
        if (e.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(e);
            if (packet.getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                Player player = (Player) e.getPlayer();
                DebugAPI.logPluginDebug("Player " + player.getName() + " swapped item with offhand (F key)");
                plugin.getDeathManager().playerOffhandSwitch(player);
            }
            return;
        }

        // Caso 3: tecla F dentro del inventario o drag directo a offhand
        if (e.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(e);
            Player player = (Player) e.getPlayer();

            // Tecla F dentro del inventario (swap con offhand)
            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.SWAP
                    && packet.getButton() == 40) {
                DebugAPI.logPluginDebug("Player " + player.getName() + " swapped item with offhand (F in inventory)");
                plugin.getDeathManager().playerOffhandSwitch(player);
                return;
            }

            // Números 1-9 dentro del inventario (swap con hotbar)
            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.SWAP
                    && packet.getButton() >= 0 && packet.getButton() <= 8) {
                int targetSlot = packet.getButton();
                DebugAPI.logPluginDebug("Player " + player.getName() + " used number key " + (targetSlot + 1) + " to swap item to hotbar slot " + targetSlot + " from inventory slot " + packet.getSlot());
                plugin.getDeathManager().playerSlotChanged(player, targetSlot);
                return;
            }

            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.PICKUP
                    && packet.getButton() >= 0 && packet.getButton() <= 8) {
                int targetSlot = packet.getButton();
                DebugAPI.logPluginDebug("Player " + player.getName() + " used number key " + (targetSlot + 1) + " to move item to hotbar slot " + targetSlot);
                plugin.getDeathManager().playerSlotChanged(player, targetSlot);
                return;
            }

            // Click directo en slot 45 (offhand en inventario de jugador)
            if (packet.getSlot() == 45) {
                DebugAPI.logPluginDebug("Player " + player.getName() + " clicked offhand slot directly");
                plugin.getDeathManager().playerOffhandSwitch(player);
                return;
            }

            // Shift+Click para mover automáticamente items
            if (packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.QUICK_MOVE) {
                // Verificar si el item que se está moviendo es un totem
                if (packet.getSlot() >= 9) { // Solo si viene del inventario principal (no hotbar)
                    org.bukkit.inventory.ItemStack clickedItem = player.getInventory().getItem(packet.getSlot());
                    if (clickedItem != null && clickedItem.getType() == Material.TOTEM_OF_UNDYING) {
                        DebugAPI.logPluginDebug("Player " + player.getName() + " shift+clicked totem from slot " + packet.getSlot());

                        // Tomar snapshot de la hotbar actual ANTES del movimiento
                        boolean[] hotbarHadTotem = new boolean[9];
                        for (int i = 0; i < 9; i++) {
                            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
                            hotbarHadTotem[i] = (item != null && item.getType() == Material.TOTEM_OF_UNDYING);
                        }

                        // Verificar después del movimiento qué slot cambió
                        TaskAPI.atLater(player, () -> {
                            for (int i = 0; i < 9; i++) {
                                org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
                                boolean hasTotemNow = (item != null && item.getType() == Material.TOTEM_OF_UNDYING);

                                // Si antes no tenía totem y ahora sí = aquí se movió el totem
                                if (!hotbarHadTotem[i] && hasTotemNow) {
                                    DebugAPI.logPluginDebug("Totem moved to hotbar slot " + i + " via shift+click");
                                    plugin.getDeathManager().playerSlotChanged(player, i);
                                    return;
                                }
                            }
                        }, 50L, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }
}
