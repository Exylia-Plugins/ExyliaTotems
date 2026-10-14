package net.exylia.exyliaTotems.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.exyliaTotems.ExyliaTotems;
import net.exylia.lib.api.totems.event.PlayerTotemHoldEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Catches a death that a totem might still undo.
 *
 * <p>Runs first so the death can be held before anything else reacts to it.
 */
public class BukkitPlayerListener implements Listener {

    private static final int OFFHAND_SLOT = 40;

    private final ExyliaTotems plugin;

    /**
     * Players whose death is already being decided.
     *
     * <p>Without it, the kill used to settle a death would be caught here and
     * held open again, forever.
     */
    private final Cache<UUID, Long> deciding;

    public BukkitPlayerListener(ExyliaTotems plugin) {
        this.plugin = plugin;
        this.deciding = Caffeine.newBuilder()
                .expireAfterWrite(Math.max(1, plugin.settings().totem().delayTicks()) * 51L, TimeUnit.MILLISECONDS)
                .maximumSize(1000)
                .build();
        plugin.plugin().getServer().getPluginManager().registerEvents(this, plugin.plugin());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (player.getLastDamageCause() == null) return;

        // A totem does not save you from these, so neither do we.
        EntityDamageEvent.DamageCause cause = player.getLastDamageCause().getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID
                || cause == EntityDamageEvent.DamageCause.SUICIDE
                || cause == EntityDamageEvent.DamageCause.KILL) {
            return;
        }

        if (deciding.getIfPresent(player.getUniqueId()) != null) return;

        List<Integer> totemSlots = totemSlots(player);
        if (totemSlots.isEmpty()) return;

        // A plugin that runs its own deaths can keep this one out of our hands.
        if (!new PlayerTotemHoldEvent(player).callEvent()) return;

        deciding.put(player.getUniqueId(), System.currentTimeMillis());

        event.setCancelled(true);
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        plugin.deaths().handlePlayerDeath(player, player.getInventory().getHeldItemSlot(), totemSlots);
    }

    /**
     * Stops tracking a player who left.
     *
     * <p>They cannot react any more, and their death is never going to settle.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.deaths().forget(event.getPlayer().getUniqueId());
    }

    private List<Integer> totemSlots(Player player) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                slots.add(slot);
            }
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.TOTEM_OF_UNDYING && !slots.contains(OFFHAND_SLOT)) {
            slots.add(OFFHAND_SLOT);
        }
        return slots;
    }
}
