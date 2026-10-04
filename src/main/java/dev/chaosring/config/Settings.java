package dev.chaosring.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Settings(String worldName,
                       double centerX,
                       double centerZ,
                       double initialSize,
                       int minPlayers,
                       int countdownSeconds,
                       double damageBuffer,
                       int warningDistance,
                       List<RingPhase> phases,
                       Anomalies anomalies) {

    public record RingPhase(int waitSeconds, int shrinkSeconds, double targetSize, double damage) {
    }

    public record Anomalies(int spawnIntervalSeconds,
                            int maxActive,
                            int durationSeconds,
                            int baseCount,
                            int perPhase,
                            int weightGravity,
                            int weightIce,
                            int weightMeteor,
                            double gravityRadius,
                            int gravityJumpAmplifier,
                            double iceRadius,
                            double meteorRadius,
                            double meteorChance,
                            float meteorYield) {
    }

    public static Settings load(FileConfiguration c) {
        List<RingPhase> phases = new ArrayList<>();
        for (Map<?, ?> raw : c.getMapList("ring.phases")) {
            phases.add(new RingPhase(
                    num(raw, "wait").intValue(),
                    num(raw, "shrink").intValue(),
                    num(raw, "size").doubleValue(),
                    num(raw, "damage").doubleValue()));
        }
        if (phases.isEmpty()) {
            throw new IllegalStateException("ring.phases bos olamaz");
        }

        Anomalies anomalies = new Anomalies(
                c.getInt("anomalies.spawn-interval", 25),
                c.getInt("anomalies.max-active", 6),
                c.getInt("anomalies.duration", 40),
                c.getInt("anomalies.base-count", 1),
                c.getInt("anomalies.per-phase", 1),
                c.getInt("anomalies.weights.low-gravity", 4),
                c.getInt("anomalies.weights.ice", 3),
                c.getInt("anomalies.weights.meteor", 3),
                c.getDouble("anomalies.low-gravity.radius", 12.0),
                c.getInt("anomalies.low-gravity.jump-amplifier", 4),
                c.getDouble("anomalies.ice.radius", 8.0),
                c.getDouble("anomalies.meteor.radius", 14.0),
                c.getDouble("anomalies.meteor.chance", 0.6),
                (float) c.getDouble("anomalies.meteor.yield", 1.6));

        if (anomalies.weightGravity() + anomalies.weightIce() + anomalies.weightMeteor() <= 0) {
            throw new IllegalStateException("anomalies.weights toplami 0'dan buyuk olmali");
        }

        return new Settings(
                c.getString("world", "world"),
                c.getDouble("center.x", 0.0),
                c.getDouble("center.z", 0.0),
                c.getDouble("initial-size", 800.0),
                Math.max(1, c.getInt("min-players", 2)),
                Math.max(1, c.getInt("countdown", 15)),
                c.getDouble("ring.damage-buffer", 2.0),
                c.getInt("ring.warning-distance", 10),
                List.copyOf(phases),
                anomalies);
    }

    private static Number num(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n;
        }
        throw new IllegalStateException("ring.phases icinde '" + key + "' eksik veya sayi degil");
    }
}
