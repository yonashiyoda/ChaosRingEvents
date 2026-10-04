package dev.chaosring.anomaly;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LargeFireball;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MeteorZone extends AnomalyZone {

    private static final int DROP_HEIGHT = 40;

    private final double chance;
    private final float yield;
    private final Set<UUID> registry;

    public MeteorZone(World world, double x, double z, int surfaceY, double radius, long expireTick,
                      double chance, float yield, Set<UUID> registry) {
        super(world, x, z, surfaceY, radius, expireTick);
        this.chance = chance;
        this.yield = yield;
        this.registry = registry;
    }

    @Override
    public AnomalyType type() {
        return AnomalyType.METEOR;
    }

    @Override
    public void pulse() {
        drawRing(Particle.FLAME);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            drop();
        }
    }

    private void drop() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double angle = rnd.nextDouble() * Math.PI * 2;
        double dist = Math.sqrt(rnd.nextDouble()) * radius;
        double tx = x + Math.cos(angle) * dist;
        double tz = z + Math.sin(angle) * dist;
        int bx = (int) Math.floor(tx);
        int bz = (int) Math.floor(tz);
        if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
            return;
        }

        int groundY = world.getHighestBlockYAt(bx, bz);
        double startY = Math.min(groundY + DROP_HEIGHT, world.getMaxHeight() - 2);

        world.spawnParticle(Particle.SOUL_FIRE_FLAME, tx, groundY + 1.1, tz, 8, 0.8, 0.05, 0.8, 0.0);

        Location start = new Location(world, tx, startY, tz);
        world.spawn(start, LargeFireball.class, fireball -> {
            fireball.setShooter(null);
            fireball.setYield(yield);
            fireball.setIsIncendiary(false);
            fireball.setPersistent(false);
            fireball.setDirection(new Vector(0, -1, 0));
            registry.add(fireball.getUniqueId());
        });
    }
}
