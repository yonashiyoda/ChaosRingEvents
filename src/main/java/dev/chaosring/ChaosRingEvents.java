package dev.chaosring;

import dev.chaosring.anomaly.AnomalyManager;
import dev.chaosring.command.ChaosRingCommand;
import dev.chaosring.config.Settings;
import dev.chaosring.game.GameManager;
import dev.chaosring.listener.MeteorListener;
import dev.chaosring.listener.PlayerLifecycleListener;
import dev.chaosring.listener.PlayerMoveListener;
import dev.chaosring.ring.RingManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChaosRingEvents extends JavaPlugin {

    private GameManager game;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Settings settings;
        try {
            settings = Settings.load(getConfig());
        } catch (RuntimeException ex) {
            getLogger().severe("config.yml okunamadi: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AnomalyManager anomalies = new AnomalyManager(this, settings);
        RingManager ring = new RingManager(this, settings, anomalies);
        game = new GameManager(this, settings, ring, anomalies);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerMoveListener(game, anomalies), this);
        pm.registerEvents(new PlayerLifecycleListener(this, game), this);
        pm.registerEvents(new MeteorListener(anomalies), this);

        PluginCommand command = getCommand("chaosring");
        if (command != null) {
            ChaosRingCommand handler = new ChaosRingCommand(game);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.stop();
        }
    }
}
