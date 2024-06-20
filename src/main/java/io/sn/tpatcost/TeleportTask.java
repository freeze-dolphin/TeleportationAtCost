package io.sn.tpatcost;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import static io.sn.tpatcost.Utils.*;

public class TeleportTask extends BukkitRunnable {

    private final Listener listener;
    private final Player player;
    private final Location destination;

    public TeleportTask(Listener listener, Player player, Location destination) {
        this.listener = listener;
        this.player = player;
        this.destination = destination;
    }

    @Override
    public void run() {
        try {
            final ItemStack costItem = Utils.COST_ITEM;
            final double distance = destination.distance(player.getLocation());
            final int costing = calcCosting(distance);

            boolean success = false;
            if (Utils.checkItem(player, costItem, costing, true)) {
                Utils.teleport(player, destination);

                player.getWorld().spawnParticle(Particle.PORTAL, destination, 30, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(destination, Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 1.0f);

                success = true;
            }

            var preprocessor = translationPreprocessorDistanceAndCosting(distance, costing);

            if (!success) {
                sendTranslation(player, "not-enough-items", preprocessor);
            } else {
                sendTranslation(player, "success", preprocessor);
            }

            if (listener.teleportTasks.containsKey(player.getUniqueId())) {
                listener.removeTeleportTask(player);
            }

            //noinspection UnstableApiUsage
            player.damage(5.0, DamageSource.builder(DamageType.FALL).build());
            // damage the player after removing the task
        } catch (IllegalArgumentException ex) {
            sendTranslation(player, "world-changed");
        }
    }
}