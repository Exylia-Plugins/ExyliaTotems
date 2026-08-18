package net.exylia.exyliaTotems.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.lib.debug.Debug;
import net.exylia.lib.task.TaskScheduler;
import net.exylia.lib.task.Tasks;
import net.exylia.lib.text.Text;
import net.exylia.exyliaTotems.ExyliaTotems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Decides which totem a dying player actually meant to use.
 *
 * <p>A player on a bad connection swaps to their totem a few milliseconds after
 * the server already killed them. The death is held briefly, and whatever they
 * did in that window decides: a hotbar slot change, a swap to the off hand, or
 * nothing at all.
 *
 * <p>Everything tracked here expires on its own. The previous version used
 * plain maps and removed entries on the paths that succeeded, so a player who
 * died and disconnected mid-window stayed in memory until the server stopped.
 */
public class DeathManager {

    /**
     * How long a tracked reaction stays interesting.
     *
     * <p>Ghosting is a matter of milliseconds; a swap a minute later is just
     * the player playing. Kept well above the configured delay so a slow
     * server still catches the reaction it is waiting for.
     */
    private static final long TRACKING_SECONDS = 30;

    /** How late a hotbar change still counts as reacting to the death. */
    private static final long SLOT_WINDOW_MILLIS = 500;

    /** The off hand gets longer: pressing F is a deliberate, slower reaction. */
    private static final long OFFHAND_WINDOW_MILLIS = 10_000;

    private static final int OFFHAND_SLOT = 40;

    private final ExyliaTotems plugin;
    private final Debug debug;
    private final TaskScheduler tasks;

    private final Cache<UUID, Integer> heldSlots;
    private final Cache<UUID, Boolean> offhandSwaps;
    private final Cache<UUID, Long> deathTimes;

    public DeathManager(ExyliaTotems plugin) {
        this.plugin = plugin;
        this.debug = plugin.debug();
        this.tasks = Tasks.of(plugin);

        this.heldSlots = expiring();
        this.offhandSwaps = expiring();
        this.deathTimes = expiring();
    }

    private static <T> Cache<UUID, T> expiring() {
        return Caffeine.newBuilder()
                .expireAfterWrite(TRACKING_SECONDS, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    /**
     * Holds a death open, then honours whichever totem the player reached for.
     *
     * @param player     who died
     * @param serverSlot the slot the server believed they were holding
     * @param totemSlots every slot that held a totem at the moment of death
     */
    public void handlePlayerDeath(Player player, int serverSlot, List<Integer> totemSlots) {
        UUID id = player.getUniqueId();
        deathTimes.put(id, System.currentTimeMillis());

        long delayTicks = Math.max(1, plugin.settings().totem().delayTicks());
        tasks.runAtEntityLater(player, delayTicks, () -> resolve(player, serverSlot, totemSlots));
    }

    private void resolve(Player player, int serverSlot, List<Integer> totemSlots) {
        UUID id = player.getUniqueId();
        Integer heldSlot = heldSlots.getIfPresent(id);
        Boolean swappedToOffhand = offhandSwaps.getIfPresent(id);

        if (Boolean.TRUE.equals(swappedToOffhand) && consumeOffhandTotem(player)) {
            save(player, "off hand");
            return;
        }

        // They did nothing: the server's own answer stands.
        if (heldSlot == null) {
            if (totemSlots.contains(serverSlot)) {
                consumeFromSlot(player, serverSlot);
                save(player, "slot " + serverSlot);
            } else {
                die(player);
            }
            return;
        }

        // They moved a totem into the slot they were already holding.
        if (serverSlot == heldSlot) {
            int slot = totemSlots.contains(serverSlot) ? serverSlot : heldSlot;
            if (isTotem(player.getInventory().getItem(slot))) {
                consumeFromSlot(player, slot);
                save(player, "slot " + slot);
            } else {
                die(player);
            }
            return;
        }

        // They swapped to another slot. It has to still hold a totem now, not
        // only at the moment of death.
        if (!isTotem(player.getInventory().getItem(heldSlot))) {
            die(player);
            return;
        }

        player.getInventory().setHeldItemSlot(heldSlot);
        consumeFromSlot(player, heldSlot);
        save(player, "slot " + heldSlot);
    }

    /** Remembers a hotbar change, if it came soon enough after a death. */
    public void playerSlotChanged(Player player, int slot) {
        if (reactedInTime(player, SLOT_WINDOW_MILLIS)) {
            heldSlots.put(player.getUniqueId(), slot);
        }
    }

    /** Remembers a swap to the off hand, if it came soon enough after a death. */
    public void playerOffhandSwitch(Player player) {
        if (reactedInTime(player, OFFHAND_WINDOW_MILLIS)) {
            offhandSwaps.put(player.getUniqueId(), Boolean.TRUE);
        }
    }

    /**
     * Whether this happened inside the window after a death.
     *
     * <p>A player who has not died is not tracked at all. The previous version
     * recorded off-hand swaps regardless "for debug", which meant a swap made
     * minutes earlier decided a later death.
     */
    private boolean reactedInTime(Player player, long windowMillis) {
        Long diedAt = deathTimes.getIfPresent(player.getUniqueId());
        if (diedAt == null) return false;

        long since = System.currentTimeMillis() - diedAt;
        if (since > windowMillis) {
            debug.debug(player.getName() + " reacted " + since + "ms after dying, too late to count");
            return false;
        }
        return true;
    }

    private boolean consumeOffhandTotem(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (!isTotem(offhand)) return false;

        applyTotemEffects(player);
        if (offhand.getAmount() > 1) {
            offhand.setAmount(offhand.getAmount() - 1);
        } else {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }
        countStatistic(player);
        return true;
    }

    private void consumeFromSlot(Player player, int slot) {
        applyTotemEffects(player);

        ItemStack totem = slot == OFFHAND_SLOT
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItem(slot);
        if (!isTotem(totem)) return;

        if (totem.getAmount() > 1) {
            totem.setAmount(totem.getAmount() - 1);
        } else if (slot == OFFHAND_SLOT) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().setItem(slot, new ItemStack(Material.AIR));
        }
        countStatistic(player);
    }

    private void save(Player player, String where) {
        forget(player.getUniqueId());
        Text.of(plugin.settings().messages().saved()).send(player);
        debug.debug(player.getName() + " was saved from ghosting, from " + where);
    }

    /**
     * Lets the death the server already decided happen.
     *
     * <p>Tracking is cleared first: the kill fires the death event again, and
     * a player still tracked would be held open a second time.
     */
    private void die(Player player) {
        forget(player.getUniqueId());
        player.setHealth(0.0);
    }

    /** Stops tracking a player, on quit or once their death is settled. */
    public void forget(UUID id) {
        heldSlots.invalidate(id);
        offhandSwaps.invalidate(id);
        deathTimes.invalidate(id);
    }

    private static boolean isTotem(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING;
    }

    private static void countStatistic(Player player) {
        player.setStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING,
                player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING) + 1);
    }

    /** Everything vanilla does when a totem fires, since the real one never did. */
    private void applyTotemEffects(Player player) {
        player.setHealth(1.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));

        Location where = player.getLocation();
        World world = player.getWorld();
        world.playSound(where, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, where.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        tasks.runAtLocationLater(where, 5L,
                () -> world.spawnParticle(Particle.TOTEM_OF_UNDYING, where.clone().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05));
        tasks.runAtLocationLater(where, 10L,
                () -> world.spawnParticle(Particle.TOTEM_OF_UNDYING, where.clone().add(0, 0.5, 0), 20, 0.2, 0.2, 0.2, 0.02));
    }
}
