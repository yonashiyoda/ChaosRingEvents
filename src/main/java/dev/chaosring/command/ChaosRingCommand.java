package dev.chaosring.command;

import dev.chaosring.game.GameManager;
import dev.chaosring.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public final class ChaosRingCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("join", "leave", "start", "stop", "status");

    private final GameManager game;

    public ChaosRingCommand(GameManager game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Msg.of("<gray>Kullanım: /" + label + " <join|leave|start|stop|status>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> withPlayer(sender, game::join);
            case "leave" -> withPlayer(sender, game::leave);
            case "start" -> {
                if (requireAdmin(sender)) {
                    game.start(sender);
                }
            }
            case "stop" -> {
                if (requireAdmin(sender)) {
                    game.stop();
                    sender.sendMessage(Msg.of("<gray>Oyun durduruldu."));
                }
            }
            case "status" -> sender.sendMessage(Msg.of("<gray>Durum: <white>" + game.state()
                    + " <gray>| Katılımcı: <white>" + game.participantCount()
                    + " <gray>| Hayatta: <white>" + game.aliveCount()));
            default -> sender.sendMessage(Msg.of("<red>Bilinmeyen alt komut."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
    }

    private void withPlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (sender instanceof Player player) {
            action.accept(player);
        } else {
            sender.sendMessage(Msg.of("<red>Bu komut sadece oyuncular içindir."));
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("chaosring.admin")) {
            return true;
        }
        sender.sendMessage(Msg.of("<red>Yetkin yok."));
        return false;
    }
}
