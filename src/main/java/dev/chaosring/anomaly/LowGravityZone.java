package dev.chaosring.anomaly;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class LowGravityZone extends AnomalyZone {

    private static final int EFFECT_TICKS = 30;

    private final int amplifier;

    public LowGravityZone(World world, double x, double z, int surfaceY, double radius, long expireTick, int amplifier) {
        super(world, x, z, surfaceY, radius, expireTick);
        this.amplifier = amplifier;
    }

    @Override
    public AnomalyType type() {
        return AnomalyType.GRAVITY;
    }

    @Override
    public void pulse() {
        drawRing(Particle.END_ROD);
    }

    @Override
    public void onEnter(Player player) {
        apply(player);
    }

    @Override
    public void whilePresent(Player player) {
        apply(player);
    }

    @Override
    public void onExit(Player player) {
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
    }

    private void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, EFFECT_TICKS, amplifier, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, EFFECT_TICKS, 0, false, false, true));
    }
}
