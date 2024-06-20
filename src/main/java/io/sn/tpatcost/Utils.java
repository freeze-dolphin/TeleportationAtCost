package io.sn.tpatcost;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static io.sn.tpatcost.Genesis.*;
import static io.sn.tpatcost.Genesis.PLAIN;

class Utils {

    public static void teleport(Player plr, Location target) {
        plr.playSound(plr, Sound.ENTITY_PLAYER_TELEPORT, 1f, 1f);
        plr.teleport(target);
    }

    public interface MessageTranslationPreprocessor {
        String preprocess(String s);
    }

    public static Component getMessageTranslation(String key, MessageTranslationPreprocessor preprocessor) {
        var fullPath = "message.%s".formatted(key);
        if (!CONFIG.contains(fullPath)) {
            return Component.text("");
        }
        var msg = CONFIG.getString(fullPath);
        assert msg != null;
        return MINI.deserialize(preprocessor.preprocess(msg));
    }

    public static Component getMessageTranslation(String key) {
        var fullPath = "message.%s".formatted(key);
        if (!CONFIG.contains(fullPath)) {
            return Component.text("");
        }
        var msg = CONFIG.getString(fullPath);
        assert msg != null;
        return MINI.deserialize(msg);
    }

    public static void sendTranslation(Audience audience, String key) {
        var msg = getMessageTranslation(key);
        if (!PLAIN.serialize(msg).isEmpty()) audience.sendMessage(msg);
    }

    public static void sendTranslation(Audience audience, String key, MessageTranslationPreprocessor preprocessor) {
        var msg = getMessageTranslation(key);
        if (!PLAIN.serialize(msg).isEmpty()) audience.sendMessage(getMessageTranslation(key, preprocessor));
    }

    public static ItemStack COST_ITEM;

    public static void updateCostItem() {
        final var costName = CONFIG.getString("use-item-as-cost");
        assert costName != null;
        final Material cost = Material.getMaterial(costName);
        if (cost == null) {
            throw new IllegalStateException("Failed to get Material for name: " + costName);
        }
        COST_ITEM = new ItemStack(cost);
    }

    public static FileConfiguration CONFIG;

    public static void updateConfiguration() {
        PLUGIN.reloadConfig();
        CONFIG = PLUGIN.getConfig();
    }

    public static boolean checkItem(Player player, ItemStack costItem, int amount, boolean consume) {
        Inventory inventory = player.getInventory();
        int totalPearls = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.isSimilar(costItem)) {
                totalPearls += item.getAmount();
            }
        }

        if (totalPearls < amount) {
            return false;
        }

        int pearlsToRemove = amount;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);

            if (item != null && item.isSimilar(costItem)) {
                int stackSize = item.getAmount();

                if (stackSize <= pearlsToRemove) {
                    if (consume) inventory.clear(i);
                    pearlsToRemove -= stackSize;
                } else {
                    if (consume) {
                        item.setAmount(stackSize - pearlsToRemove);
                        inventory.setItem(i, item);
                    }
                    return true;
                }

                if (pearlsToRemove <= 0) {
                    return true;
                }
            }
        }

        return true;
    }

    public static int calcCosting(Player plr, Location destination) {
        final double distance = destination.distance(plr.getLocation());
        final int tmp = Math.max((int) (distance / CONFIG.getDouble("distance-factor")), CONFIG.getInt("costing-least"));
        return Math.min(tmp, CONFIG.getInt("costing-most"));
    }

    public static int calcCosting(double distance) {
        final int tmp = Math.max((int) (distance / CONFIG.getDouble("distance-factor")), CONFIG.getInt("costing-least"));
        return Math.min(tmp, CONFIG.getInt("costing-most"));
    }

    public static MessageTranslationPreprocessor translationPreprocessorDistanceAndCosting(double distance, int costing) {
        return s -> s.replaceAll("%distance%", "%.2f".formatted(distance)).replaceAll("%requirement%", String.valueOf(costing));
    }
}
