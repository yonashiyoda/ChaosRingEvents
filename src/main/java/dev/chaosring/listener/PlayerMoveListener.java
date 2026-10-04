package dev.chaosring.listener;

import dev.chaosring.anomaly.AnomalyManager;
import dev.chaosring.game.GameManager;
import dev.chaosring.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PlayerMoveListener implements Listener {

    private final GameManager game;
    private final AnomalyManager anomalies;

    public PlayerMoveListener(GameManager game, AnomalyManager anomalies) {
        this.game = game;
        this.anomalies = anomalies;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (game.state() != GameState.RUNNING || !event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        if (!game.isAlive(player.getUniqueId())) {
            return;
        }
        anomalies.track(player, event.getTo());
    }
}
