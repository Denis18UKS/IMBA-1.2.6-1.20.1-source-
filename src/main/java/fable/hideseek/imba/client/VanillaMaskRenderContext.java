package fable.hideseek.imba.client;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Small thread-local bridge used only while a player-mask is being rendered.
 * Client render code is single-threaded, but a ThreadLocal keeps the mixins
 * completely inert for normal vanilla block/item rendering.
 */
public final class VanillaMaskRenderContext {
    public enum Mode {
        NONE,
        WORLD_BLOCK,
        POTION_ITEM_AS_BLOCK
    }

    public record Context(Mode mode, World world, BlockPos pos) {
    }

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private VanillaMaskRenderContext() {
    }

    public static void begin(Mode mode, World world, BlockPos pos) {
        if (mode == null || mode == Mode.NONE || world == null || pos == null) {
            CURRENT.remove();
            return;
        }
        CURRENT.set(new Context(mode, world, pos.toImmutable()));
    }

    public static Context get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
