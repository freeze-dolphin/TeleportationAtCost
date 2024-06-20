package io.sn.tpatcost;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.sn.tpatcost.Utils.*;

public class Listener implements org.bukkit.event.Listener {

    private static Genesis PLUGIN;

    public Listener(Genesis plugin) {
        PLUGIN = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onTp(PlayerCommandPreprocessEvent evt) {
        // /tp Freeze_Dolphin -733.5 135 289.5
        if (evt.getMessage().startsWith("/tp") && !evt.getPlayer().isOp() && !evt.getPlayer().getGameMode().isInvulnerable() && evt.getPlayer().hasPermission("teleportationatcost.use")) {
            final var parameters = evt.getMessage().split(" ");

            if (parameters.length != 5) return;
            if (!parameters[1].equals(evt.getPlayer().getName())) return;

            evt.setCancelled(true);
            try {
                Location destination = new Location(evt.getPlayer().getWorld(), Double.parseDouble(parameters[2]), Double.parseDouble(parameters[3]), Double.parseDouble(parameters[4]));
                final double distance = destination.distance(evt.getPlayer().getLocation());
                final int costing = calcCosting(distance);

                var preprocessor = translationPreprocessorDistanceAndCosting(distance, costing);

                sendTranslation(evt.getPlayer(), "calculation", preprocessor);

                if (Utils.checkItem(evt.getPlayer(), Utils.COST_ITEM, costing, false)) {
                    BukkitRunnable task = new TeleportTask(this, evt.getPlayer(), destination);
                    task.runTaskLater(PLUGIN, CONFIG.getInt("teleport-delay-in-ticks"));

                    addTeleportTask(evt.getPlayer(), task);
                    sendTranslation(evt.getPlayer(), "getting-ready", preprocessor);
                } else {
                    sendTranslation(evt.getPlayer(), "not-enough-items", preprocessor);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public final Map<UUID, BukkitRunnable> teleportTasks = new HashMap<>();

    public void addTeleportTask(Player player, BukkitRunnable task) {
        teleportTasks.put(player.getUniqueId(), task);
    }

    public void removeTeleportTask(Player player) {
        teleportTasks.get(player.getUniqueId()).cancel();
        teleportTasks.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent evt) {
        Player player = evt.getPlayer();
        if (evt.hasChangedBlock()) {
            if (teleportTasks.containsKey(player.getUniqueId())) {
                removeTeleportTask(player);
                sendTranslation(player, "moved");
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent evt) {
        if (evt.getEntity() instanceof Player player) {
            if (teleportTasks.containsKey(player.getUniqueId())) {
                removeTeleportTask(player);
                sendTranslation(player, "damaged");
            }
        }
    }


}
