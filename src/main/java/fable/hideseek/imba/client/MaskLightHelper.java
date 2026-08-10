package fable.hideseek.imba.client;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/** Samples the surrounding air lighting used by vanilla wall/stand models. */
public final class MaskLightHelper {
    private MaskLightHelper() {
    }

    public static int resolve(UUID uuid, World world, double fallbackX, double fallbackY, double fallbackZ) {
        if (world == null) {
            return 0;
        }
        Vec3d anchor = ClientMaskData.getStatueAnchor(uuid);
        double x = anchor == null ? fallbackX : anchor.x;
        double y = anchor == null ? fallbackY : anchor.y;
        double z = anchor == null ? fallbackZ : anchor.z;
        BlockPos base = BlockPos.ofFloored(x, y + 0.25D, z);
        BlockPos[] samples = new BlockPos[] {
                base, base.up(), base.down(), base.north(), base.south(), base.east(), base.west(),
                base.up().north(), base.up().south(), base.up().east(), base.up().west()
        };

        int maxBlock = 0;
        int maxSky = 0;
        for (BlockPos pos : samples) {
            int packed = WorldRenderer.getLightmapCoordinates(world, pos);
            maxBlock = Math.max(maxBlock, (packed >> 4) & 0xF);
            maxSky = Math.max(maxSky, (packed >> 20) & 0xF);
        }
        return (maxBlock << 4) | (maxSky << 20);
    }
}
