package net.exylia.exyliaTotems.managers;

import net.exylia.commons.utils.visuals.MessageUtils;
import net.exylia.exyliaTotems.ExyliaTotems;
import net.exylia.exyliaTotems.configs.MainConfig;
import net.exylia.exyliaTotems.configs.Messages;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static net.exylia.commons.utils.DebugUtils.logInfo;

public class DeathManager {
    private final ExyliaTotems plugin;
    private final HashMap<UUID, Integer> playerSlots = new HashMap<>();
    public DeathManager(ExyliaTotems plugin) {
        this.plugin = plugin;
    }
    public void handlePlayerDeath(Player player, int serverSlot, List<Integer> totemSlots) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Integer currentSlot = this.playerSlots.get(player.getUniqueId());

            if (currentSlot == null || !totemSlots.contains(currentSlot) || serverSlot == currentSlot) {
                this.simulateNormalDeath(player);
                return;
            }

            player.getInventory().setHeldItemSlot(currentSlot);

            ItemStack totem = player.getInventory().getItem(currentSlot);
            if (totem != null && totem.getType() == Material.TOTEM_OF_UNDYING) {
                this.applyTotemEffects(player);

                if (totem.getAmount() > 1) {
                    totem.setAmount(totem.getAmount() - 1);
                } else {
                    player.getInventory().setItem(currentSlot, new ItemStack(Material.AIR));
                }
            } else {
                player.setHealth(1.0);
            }

            player.setStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING,
                              player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING) + 1);

            this.playerSlots.remove(player.getUniqueId());
            MessageUtils.sendMessage(player, Messages.get("used"));
            logInfo("Player " + player.getName() + " saved from ghosting by ExyliaTotems system.");
        }, MainConfig.totemsDelayTicks());
    }

    public void playerSlotChanged(Player player, int slot) {
        this.playerSlots.put(player.getUniqueId(), slot);
    }

    private void simulateNormalDeath(Player player) {
        player.setHealth(0.0);
        this.playerSlots.remove(player.getUniqueId());
    }

    private void applyTotemEffects(Player player) {
        player.setHealth(1.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        Location loc = player.getLocation();
        World world = player.getWorld();
        world.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        world.spawnParticle(Particle.TOTEM, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> world.spawnParticle(Particle.TOTEM, loc.clone().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05), 5L);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> world.spawnParticle(Particle.TOTEM, loc.clone().add(0, 0.5, 0), 20, 0.2, 0.2, 0.2, 0.02), 10L);
    }
}
