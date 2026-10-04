package dev.chaosring.ring;

import dev.chaosring.anomaly.AnomalyManager;
import dev.chaosring.config.Settings;
import dev.chaosring.config.Settings.RingPhase;
import dev.chaosring.util.Msg;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class RingManager {

    private enum Stage {
        WAITING,
        SHRINKING,
        DONE
    }

    private final Plugin plugin;
    private final Settings settings;
    private final AnomalyManager anomalies;
    private final List<RingPhase> phases;
    private final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

    private World world;
    private BukkitTask task;
    private Stage stage = Stage.DONE;
    private int phaseIndex;
    private int remaining;
    private int stageTotal;

    public RingManager(Plugin plugin, Settings settings, AnomalyManager anomalies) {
        this.plugin = plugin;
        this.settings = settings;
        this.anomalies = anomalies;
        this.phases = settings.phases();
    }

    public void start(World world) {
        stop();
        this.world = world;

        WorldBorder border = world.getWorldBorder();
        border.setCenter(settings.centerX(), settings.centerZ());
        border.setSize(settings.initialSize());
        border.setDamageBuffer(settings.damageBuffer());
        border.setDamageAmount(phases.get(0).damage());
        border.setWarningDistance(settings.warningDistance());

        phaseIndex = 0;
        beginStage(Stage.WAITING, phases.get(0).waitSeconds());
        refreshBar();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (world != null) {
            for (Player player : world.getPlayers()) {
                player.hideBossBar(bar);
            }
            world.getWorldBorder().reset();
            world = null;
        }
        stage = Stage.DONE;
    }

    private void tick() {
        remaining--;
        if (remaining <= 0) {
            advance();
        }
        refreshBar();
    }

    private void advance() {
        if (stage == Stage.WAITING) {
            RingPhase phase = phases.get(phaseIndex);
            WorldBorder border = world.getWorldBorder();
            border.setDamageAmount(phase.damage());
            border.setSize(phase.targetSize(), phase.shrinkSeconds());
            beginStage(Stage.SHRINKING, phase.shrinkSeconds());
            broadcast("<red>Çember daralıyor! <gray>Hedef çap: <white>" + (int) phase.targetSize());
            anomalies.onPhaseStart(phaseIndex + 1);
            return;
        }

        phaseIndex++;
        if (phaseIndex >= phases.size()) {
            stage = Stage.DONE;
            remaining = 0;
            if (task != null) {
                task.cancel();
                task = null;
            }
            return;
        }
        beginStage(Stage.WAITING, phases.get(phaseIndex).waitSeconds());
        broadcast("<yellow>Çember sabitlendi. Sonraki daralma: <white>" + format(remaining));
    }

    private void beginStage(Stage next, int seconds) {
        stage = next;
        remaining = seconds;
        stageTotal = Math.max(1, seconds);
    }

    private void refreshBar() {
        switch (stage) {
            case WAITING -> {
                bar.name(Component.text("Çember daralmasına " + format(remaining), NamedTextColor.YELLOW));
                bar.color(BossBar.Color.YELLOW);
            }
            case SHRINKING -> {
                bar.name(Component.text("Çember daralıyor " + format(remaining), NamedTextColor.RED));
                bar.color(BossBar.Color.RED);
            }
            case DONE -> {
                bar.name(Component.text("Son çember", NamedTextColor.DARK_RED));
                bar.color(BossBar.Color.PURPLE);
            }
        }
        float progress = stage == Stage.DONE ? 1f : remaining / (float) stageTotal;
        bar.progress(Math.max(0f, Math.min(1f, progress)));

        for (Player player : world.getPlayers()) {
            player.showBossBar(bar);
        }
    }

    private void broadcast(String text) {
        Component message = Msg.of(text);
        for (Player player : world.getPlayers()) {
            player.sendMessage(message);
        }
    }

    private static String format(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
