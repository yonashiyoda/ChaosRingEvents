package dev.chaosring.anomaly;

import dev.chaosring.config.Settings;
import dev.chaosring.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AnomalyManager {

    private static final int PULSE_TICKS = 10;
    private static final int EDGE_MARGIN = 2;

    private final Plugin plugin;
    private final Settings.Anomalies cfg;
    private final List<AnomalyZone> zones = new ArrayList<>();
    private final Map<UUID, AnomalyZone> occupancy = new HashMap<>();
    private final Set<UUID> meteors = new HashSet<>();

    private World world;
    private Set<UUID> alive = Set.of();
    private BukkitTask task;
    private long now;
    private int sinceSpawn;
    private int pending;

    public AnomalyManager(Plugin plugin, Settings settings) {
        this.plugin = plugin;
        this.cfg = settings.anomalies();
    }

    public void start(World world, Set<UUID> aliveView) {
        stop();
        this.world = world;
        this.alive = aliveView;
        this.now = 0;
        this.sinceSpawn = 0;
        this.pending = 0;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::pulse, PULSE_TICKS, PULSE_TICKS);
    }

    public void stop() {
        if (task == null) {
            return;
        }
        task.cancel();
        task = null;

        for (AnomalyZone zone : zones) {
            evict(zone);
            zone.deactivate();
        }
        zones.clear();
        occupancy.clear();

        for (UUID id : meteors) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        meteors.clear();
    }

    public void onPhaseStart(int phaseNumber) {
        spawn(cfg.baseCount() + cfg.perPhase() * phaseNumber);
    }

    public boolean consumeMeteor(UUID id) {
        return meteors.remove(id);
    }

    public void track(Player player, Location loc) {
        UUID id = player.getUniqueId();
        AnomalyZone current = occupancy.get(id);

        AnomalyZone found = null;
        for (int i = 0, n = zones.size(); i < n; i++) {
            AnomalyZone zone = zones.get(i);
            if (zone.contains(loc)) {
                found = zone;
                break;
            }
        }
        if (found == current) {
            return;
        }
        if (current != null) {
            current.onExit(player);
        }
        if (found == null) {
            occupancy.remove(id);
        } else {
            occupancy.put(id, found);
            found.onEnter(player);
        }
    }

    public void release(Player player) {
        AnomalyZone zone = occupancy.remove(player.getUniqueId());
        if (zone != null) {
            zone.onExit(player);
        }
    }

    private void pulse() {
        now += PULSE_TICKS;

        var iterator = zones.iterator();
        while (iterator.hasNext()) {
            AnomalyZone zone = iterator.next();
            if (zone.expired(now)) {
                iterator.remove();
                evict(zone);
                zone.deactivate();
            } else {
                zone.pulse();
            }
        }

        for (var entry : occupancy.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                entry.getValue().whilePresent(player);
            }
        }

        sinceSpawn += PULSE_TICKS;
        if (sinceSpawn >= cfg.spawnIntervalSeconds() * 20) {
            sinceSpawn = 0;
            spawn(1);
        }
    }

    private void evict(AnomalyZone zone) {
        occupancy.entrySet().removeIf(entry -> {
            if (entry.getValue() != zone) {
                return false;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                zone.onExit(player);
            }
            return true;
        });
    }

    private void spawn(int count) {
        for (int i = 0; i < count; i++) {
            if (zones.size() + pending >= cfg.maxActive()) {
                return;
            }
            requestZone();
        }
    }

    private void requestZone() {
        World target = world;
        WorldBorder border = target.getWorldBorder();
        AnomalyType type = pickType();
        double radius = radiusOf(type);
        double half = border.getSize() / 2 - radius - EDGE_MARGIN;
        if (half <= 0) {
            return;
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Location center = border.getCenter();
        double px = center.getX() + rnd.nextDouble(-half, half);
        double pz = center.getZ() + rnd.nextDouble(-half, half);
        int bx = (int) Math.floor(px);
        int bz = (int) Math.floor(pz);

        pending++;
        target.getChunkAtAsync(bx >> 4, bz >> 4).whenComplete((chunk, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pending--;
                    if (error != null || task == null || world != target) {
                        return;
                    }
                    place(type, px, pz, radius);
                }));
    }

    private void place(AnomalyType type, double px, double pz, double radius) {
        int bx = (int) Math.floor(px);
        int bz = (int) Math.floor(pz);
        int surface = world.getHighestBlockYAt(bx, bz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (world.getBlockAt(bx, surface, bz).isLiquid()) {
            return;
        }
        for (AnomalyZone existing : zones) {
            if (existing.overlaps(px, pz, radius)) {
                return;
            }
        }

        long expire = now + cfg.durationSeconds() * 20L;
        AnomalyZone zone = switch (type) {
            case GRAVITY -> new LowGravityZone(world, px, pz, surface, radius, expire, cfg.gravityJumpAmplifier());
            case ICE -> new IceZone(world, px, pz, surface, radius, expire);
            case METEOR -> new MeteorZone(world, px, pz, surface, radius, expire,
                    cfg.meteorChance(), cfg.meteorYield(), meteors);
        };

        zone.activate();
        zones.add(zone);
        announce(zone);

        for (UUID id : alive) {
            if (occupancy.containsKey(id)) {
                continue;
            }
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                track(player, player.getLocation());
            }
        }
    }

    private void announce(AnomalyZone zone) {
        var message = Msg.of("<yellow>Anomali: <white>" + zone.type().label()
                + " <gray>(X: " + zone.blockX() + ", Z: " + zone.blockZ() + ")");
        for (UUID id : alive) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    private AnomalyType pickType() {
        int total = cfg.weightGravity() + cfg.weightIce() + cfg.weightMeteor();
        int roll = ThreadLocalRandom.current().nextInt(total);
        if (roll < cfg.weightGravity()) {
            return AnomalyType.GRAVITY;
        }
        if (roll < cfg.weightGravity() + cfg.weightIce()) {
            return AnomalyType.ICE;
        }
        return AnomalyType.METEOR;
    }

    private double radiusOf(AnomalyType type) {
        return switch (type) {
            case GRAVITY -> cfg.gravityRadius();
            case ICE -> cfg.iceRadius();
            case METEOR -> cfg.meteorRadius();
        };
    }
}
