package io.github.mcalgovisualizations.demo;

import io.github.mcalgovisualizations.demo.commands.*;
import io.github.mcalgovisualizations.demo.config.WorldConfig;
import io.github.mcalgovisualizations.visualization.instance.AlgoCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;


public class Main {
    private static AlgoCraft algo = null;

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());
        InstanceContainer instance = WorldConfig.createMainInstance();

        instance.setTimeRate(0);
        instance.setTime(6000);

        registerListeners(instance);

        algo = new AlgoCraft(instance);
        algo.addListener(MinecraftServer.getGlobalEventHandler());

        RegisterAlgo.registerAlgo(algo);
        // Passing command handlers for inventory management
        registerCommands(MinecraftServer.getCommandManager(), algo);
        server.start("0.0.0.0", 25565);
    }


    static void registerListeners(InstanceContainer instance) {
        final var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerSkin skin = PlayerSkin.fromUsername(player.getUsername());
            if (skin != null) {
                player.setSkin(skin);
            }
            event.setSpawningInstance(instance);
            player.setRespawnPoint(new Pos(194.5, 137, -38.5));
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;

            Player player = event.getPlayer();

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlying(true);

            algo.applyDefaultLayout(player);

            player.sendMessage(Component.text(
                    "Right-click the Nether Star to select an algorithm to visualize!", NamedTextColor.YELLOW));
//            player.sendMessage(Component.text(
//                    "Welcome to Algorithm Visualizations!", ));
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
        });
    }

    static void registerCommands(CommandManager cm, AlgoCraft algo) {
        cm.register(new Greet(),
                new Teleport(),
                new Gamemode(),
                new Spawn(algo),
                new Invite(algo),
                new Accept(algo),
                new PendingInvites(algo)
        );
    }


}
