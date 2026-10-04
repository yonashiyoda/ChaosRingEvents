package dev.chaosring.listener;

import dev.chaosring.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.GameMode;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

public final class PlayerLifecycleListener implements Listener {

    private final Plugin plugin;
    private final GameManager game;

    public PlayerLifecycleListener(Plugin plugin, GameManager game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (game.isAlive(player.getUniqueId())) {
            game.eliminate(player, false);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!game.isEliminated(player.getUniqueId())) {
            return;
        }
        event.setRespawnLocation(game.spectatorSpawn());
        Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        game.handleQuit(event.getPlayer());
    }
}
