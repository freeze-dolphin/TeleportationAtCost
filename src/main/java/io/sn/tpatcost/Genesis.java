package io.sn.tpatcost;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Genesis extends JavaPlugin {

    public static final MiniMessage MINI = MiniMessage.miniMessage();
    public static PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    public static Genesis PLUGIN;

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();

        Utils.updateConfiguration();
        Utils.updateCostItem();

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("tpac")
                            .then(Commands.literal("reload")    
                                    .requires(source -> source.getSender().hasPermission("teleportationatcost.command.reload"))
                                    .executes(ctx -> {
                                        Utils.updateConfiguration();
                                        Utils.updateCostItem();
                                        ctx.getSource().getSender().sendPlainMessage("TeleportationAtCost Reloaded!");
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .build()
                            )
                            .build(),
                    "Admin command of TeleportationAtCost",
                    List.of()
            );
        });
        this.getServer().getPluginManager().registerEvents(new Listener(this), this);
    }

    @Override
    public void onDisable() {
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
    }

}
