package dev.chaosring.listener;

import dev.chaosring.anomaly.AnomalyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class MeteorListener implements Listener {

    private final AnomalyManager anomalies;

    public MeteorListener(AnomalyManager anomalies) {
        this.anomalies = anomalies;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (anomalies.consumeMeteor(event.getEntity().getUniqueId())) {
            event.blockList().clear();
        }
    }
}
