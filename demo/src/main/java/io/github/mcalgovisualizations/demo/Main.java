package io.github.mcalgovisualizations.demo;

import io.github.mcalgovisualizations.visualization.instance.AlgoCraft;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;

public class Main {
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        var algo = new AlgoCraft(instance);
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
        server.start("0.0.0.0", 25565);
    }
}
