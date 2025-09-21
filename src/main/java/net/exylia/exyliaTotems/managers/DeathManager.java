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
import static net.exylia.commons.utils.DebugUtils.logDebug;

public class DeathManager {
    private final ExyliaTotems plugin;
    private final HashMap<UUID, Integer> playerSlots = new HashMap<>();
    private final HashMap<UUID, Boolean> playerOffhandSwitch = new HashMap<>();
    private final HashMap<UUID, Long> playerDeathTime = new HashMap<>();
    public DeathManager(ExyliaTotems plugin) {
        this.plugin = plugin;
    }
    public void handlePlayerDeath(Player player, int serverSlot, List<Integer> totemSlots) {
        logDebug("Starting death handler for " + player.getName() + " - delay: " + MainConfig.totemsDelayTicks() + " ticks");

        // Marcar el tiempo de muerte para filtrar cambios de slot relevantes
        this.playerDeathTime.put(player.getUniqueId(), System.currentTimeMillis());

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Integer currentSlot = this.playerSlots.get(player.getUniqueId());
            Boolean switchedToOffhand = this.playerOffhandSwitch.get(player.getUniqueId());

            logDebug("Death handler executing for " + player.getName() + " - currentSlot: " + currentSlot + ", offhandSwitch: " + switchedToOffhand + ", serverSlot: " + serverSlot + ", totemSlots: " + totemSlots);

            if (switchedToOffhand != null && switchedToOffhand) {
                logDebug("Player " + player.getName() + " switched to offhand - checking for totem");
                ItemStack offhandTotem = player.getInventory().getItemInOffHand();
                if (offhandTotem != null && offhandTotem.getType() == Material.TOTEM_OF_UNDYING) {
                    logDebug("Valid totem found in offhand after switch, applying totem effects");
                    this.applyTotemEffects(player);

                    if (offhandTotem.getAmount() > 1) {
                        offhandTotem.setAmount(offhandTotem.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                    }

                    player.setStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING,
                                      player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING) + 1);

                    this.playerSlots.remove(player.getUniqueId());
                    this.playerOffhandSwitch.remove(player.getUniqueId());
                    this.playerDeathTime.remove(player.getUniqueId());
                    MessageUtils.sendMessage(player, Messages.get("used"));
                    logInfo("Player " + player.getName() + " saved from ghosting by ExyliaTotems system (offhand).");
                    return;
                } else {
                    logDebug("No valid totem in offhand for " + player.getName() + " after switch");
                }
            }

            if (currentSlot == null) {
                logDebug("No slot change detected for " + player.getName() + ", checking if server slot has totem");
                if (totemSlots.contains(serverSlot)) {
                    logDebug("Server slot " + serverSlot + " contains totem, applying totem effects");
                    this.applyTotemEffectsToSlot(player, serverSlot);
                } else {
                    logDebug("Server slot " + serverSlot + " doesn't contain totem, executing normal death");
                    this.simulateNormalDeath(player);
                }
                return;
            }

            if (serverSlot == currentSlot) {
                logDebug("Server slot matches current slot for " + player.getName() + ", checking if it had totem originally");
                if (totemSlots.contains(serverSlot)) {
                    logDebug("Server slot " + serverSlot + " had original totem, applying totem effects");
                    this.applyTotemEffectsToSlot(player, serverSlot);
                } else {
                    logDebug("Server slot " + serverSlot + " had no original totem but player swapped to it - applying totem effects");
                    ItemStack currentItem = player.getInventory().getItem(currentSlot);
                    if (currentItem != null && currentItem.getType() == Material.TOTEM_OF_UNDYING) {
                        this.applyTotemEffectsToSlot(player, currentSlot);
                    } else {
                        logDebug("No totem found in current slot after swap, executing normal death");
                        this.simulateNormalDeath(player);
                    }
                }
                return;
            }

            // Verificar dinámicamente si hay totem en el slot actual (después del movimiento)
            ItemStack currentItem = player.getInventory().getItem(currentSlot);
            if (currentItem == null || currentItem.getType() != Material.TOTEM_OF_UNDYING) {
                logDebug("Current slot " + currentSlot + " for " + player.getName() + " doesn't contain totem after movement, executing normal death");
                this.simulateNormalDeath(player);
                return;
            }

            logDebug("Valid totem slot change detected for " + player.getName() + ", switching to slot " + currentSlot);
            player.getInventory().setHeldItemSlot(currentSlot);

            logDebug("Valid totem found in slot " + currentSlot + ", applying totem effects");
            this.applyTotemEffects(player);

            if (currentItem.getAmount() > 1) {
                currentItem.setAmount(currentItem.getAmount() - 1);
            } else {
                player.getInventory().setItem(currentSlot, new ItemStack(Material.AIR));
            }

            player.setStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING,
                              player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING) + 1);

            this.playerSlots.remove(player.getUniqueId());
            this.playerOffhandSwitch.remove(player.getUniqueId());
            this.playerDeathTime.remove(player.getUniqueId());
            MessageUtils.sendMessage(player, Messages.get("used"));
            logInfo("Player " + player.getName() + " saved from ghosting by ExyliaTotems system.");
        }, MainConfig.totemsDelayTicks());
    }

    public void playerSlotChanged(Player player, int slot) {
        // Solo trackear cambios de slot si el jugador acaba de morir (dentro del window de ghosting)
        Long deathTime = this.playerDeathTime.get(player.getUniqueId());
        if (deathTime != null) {
            long timeSinceDeath = System.currentTimeMillis() - deathTime;
            // Solo considerar cambios dentro de una ventana de tiempo razonable para ghosting (500ms)
            if (timeSinceDeath <= 500) {
                logDebug("Player " + player.getName() + " slot changed to: " + slot + " (" + timeSinceDeath + "ms after death)");
                this.playerSlots.put(player.getUniqueId(), slot);
            } else {
                logDebug("Player " + player.getName() + " slot changed to: " + slot + " but too late for ghosting (" + timeSinceDeath + "ms after death)");
            }
        } else {
            logDebug("Player " + player.getName() + " slot changed to: " + slot + " (not in death window)");
        }
    }

    public void playerOffhandSwitch(Player player) {
        // Solo trackear offhand switches si el jugador acaba de morir
        Long deathTime = this.playerDeathTime.get(player.getUniqueId());
        if (deathTime != null) {
            long timeSinceDeath = System.currentTimeMillis() - deathTime;
            if (timeSinceDeath <= 10000) { // Ventana más amplia para debug
                logDebug("Player " + player.getName() + " performed offhand switch (" + timeSinceDeath + "ms after death)");
                this.playerOffhandSwitch.put(player.getUniqueId(), true);
            } else {
                logDebug("Player " + player.getName() + " performed offhand switch but too late for ghosting (" + timeSinceDeath + "ms after death)");
            }
        } else {
            // Si no hay muerte reciente, también trackear por si acaso (para debug)
            logDebug("Player " + player.getName() + " performed offhand switch (no recent death - tracking anyway for debug)");
            this.playerOffhandSwitch.put(player.getUniqueId(), true);
        }
    }

    private void simulateNormalDeath(Player player) {
        logDebug("Simulating normal death for " + player.getName());

        // Limpiar tracking antes de matar para evitar loop infinito
        this.playerSlots.remove(player.getUniqueId());
        this.playerOffhandSwitch.remove(player.getUniqueId());
        this.playerDeathTime.remove(player.getUniqueId());

        // Matar al jugador
        player.setHealth(0.0);
    }

    private void applyTotemEffects(Player player) {
        logDebug("Applying totem effects to " + player.getName());
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
        logDebug("Totem effects applied successfully to " + player.getName());
    }

    private void applyTotemEffectsToSlot(Player player, int slot) {
        logDebug("Applying totem effects and consuming totem from slot " + slot + " for " + player.getName());

        this.applyTotemEffects(player);

        ItemStack totemItem = player.getInventory().getItem(slot);
        if (totemItem != null && totemItem.getType() == Material.TOTEM_OF_UNDYING) {
            if (totemItem.getAmount() > 1) {
                totemItem.setAmount(totemItem.getAmount() - 1);
            } else {
                player.getInventory().setItem(slot, new ItemStack(Material.AIR));
            }
        }

        player.setStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING,
                          player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING) + 1);

        this.playerSlots.remove(player.getUniqueId());
        this.playerOffhandSwitch.remove(player.getUniqueId());
        this.playerDeathTime.remove(player.getUniqueId());
        MessageUtils.sendMessage(player, Messages.get("used"));
        logInfo("Player " + player.getName() + " saved from ghosting by ExyliaTotems system (slot " + slot + ").");
    }
}
