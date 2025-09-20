package net.exylia.exyliaTotems.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.exylia.exyliaTotems.ExyliaTotems;
import net.exylia.exyliaTotems.configs.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BukkitPlayerListener implements Listener {
    private final ExyliaTotems plugin;

    private final Cache<UUID, Long> cache;

    public BukkitPlayerListener(ExyliaTotems plugin) {
        this.plugin = plugin;
        this.cache = CacheBuilder.newBuilder().expireAfterWrite(MainConfig.totemsDelayTicks() * 51L, TimeUnit.MILLISECONDS).build();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!(player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) return;
        if (player.getLastDamageCause() == null) return;
        EntityDamageEvent.DamageCause cause = player.getLastDamageCause().getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.SUICIDE || cause == EntityDamageEvent.DamageCause.KILL) return;
        if (cache.getIfPresent(player.getUniqueId()) != null) return;

        cache.put(player.getUniqueId(), System.currentTimeMillis());

        List<Integer> totemSlots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING)
                totemSlots.add(i);
        }

        if (!totemSlots.isEmpty()) {
            event.setCancelled(true);
            event.deathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);

            plugin.getDeathManager().handlePlayerDeath(player, player.getInventory().getHeldItemSlot(), totemSlots);
        }
    }
}