package io.github.mcalgovisualizations.demo.commands;


import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class Greet extends Command {
    public Greet() {
        super("greet");
        addSyntax((sender, _) -> {
            if (sender instanceof Player p) sender.sendMessage("Hello " + p.getUsername());
        });
    }
}