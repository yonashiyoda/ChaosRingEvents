package dev.chaosring.anomaly;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;

public final class IceZone extends AnomalyZone {

    private record Snapshot(Block block, BlockData data) {
    }

    private final List<Snapshot> snapshots = new ArrayList<>();

    public IceZone(World world, double x, double z, int surfaceY, double radius, long expireTick) {
        super(world, x, z, surfaceY, radius, expireTick);
    }

    @Override
    public AnomalyType type() {
        return AnomalyType.ICE;
    }

    @Override
    public void activate() {
        int r = (int) Math.ceil(radius);
        int cx = blockX();
        int cz = blockZ();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                int bx = cx + dx;
                int bz = cz + dz;
                if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
                    continue;
                }
                int y = world.getHighestBlockYAt(bx, bz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                Block block = world.getBlockAt(bx, y, bz);
                Material type = block.getType();
                if (!type.isSolid() || type == Material.BLUE_ICE || type == Material.BEDROCK) {
                    continue;
                }
                if (block.getState(false) instanceof TileState) {
                    continue;
                }
                snapshots.add(new Snapshot(block, block.getBlockData()));
                block.setType(Material.BLUE_ICE, false);
            }
        }
    }

    @Override
    public void pulse() {
        drawRing(Particle.SNOWFLAKE);
    }

    @Override
    public void deactivate() {
        for (Snapshot snapshot : snapshots) {
            snapshot.block().setBlockData(snapshot.data(), false);
        }
        snapshots.clear();
    }
}
