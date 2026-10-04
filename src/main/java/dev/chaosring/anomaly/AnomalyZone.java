package dev.chaosring.anomaly;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public abstract class AnomalyZone {

    protected final World world;
    protected final double x;
    protected final double z;
    protected final double radius;
    protected final double radiusSq;
    protected final int surfaceY;
    private final long expireTick;

    protected AnomalyZone(World world, double x, double z, int surfaceY, double radius, long expireTick) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.surfaceY = surfaceY;
        this.radius = radius;
        this.radiusSq = radius * radius;
        this.expireTick = expireTick;
    }

    public abstract AnomalyType type();

    public void activate() {
    }

    public void pulse() {
    }

    public void deactivate() {
    }

    public void onEnter(Player player) {
    }

    public void onExit(Player player) {
    }

    public void whilePresent(Player player) {
    }

    public final boolean contains(Location loc) {
        if (loc.getWorld() != world) {
            return false;
        }
        double dx = loc.getX() - x;
        double dz = loc.getZ() - z;
        return dx * dx + dz * dz <= radiusSq;
    }

    public final boolean overlaps(double ox, double oz, double or) {
        double dx = x - ox;
        double dz = z - oz;
        double sum = radius + or;
        return dx * dx + dz * dz < sum * sum;
    }

    public final boolean expired(long now) {
        return now >= expireTick;
    }

    public final int blockX() {
        return (int) Math.floor(x);
    }

    public final int blockZ() {
        return (int) Math.floor(z);
    }

    protected final void drawRing(Particle particle) {
        int points = (int) Math.min(36, Math.max(16, radius * 2));
        double step = Math.PI * 2 / points;
        double y = surfaceY + 1.2;
        for (int i = 0; i < points; i++) {
            double angle = i * step;
            world.spawnParticle(particle, x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }
}
