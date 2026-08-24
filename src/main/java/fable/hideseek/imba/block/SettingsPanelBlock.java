package fable.hideseek.imba.block;

import fable.hideseek.imba.config.GameSettingsConfig;
import fable.hideseek.imba.config.PanelHitboxConfig;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.net.MaskNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class SettingsPanelBlock extends Block {
    public static final DirectionProperty FACING = net.minecraft.state.property.Properties.HORIZONTAL_FACING;
    public static final IntProperty COLUMN = IntProperty.of("column", 0, 2);
    public static final IntProperty ROW = IntProperty.of("row", 0, 2);

    private static final int BOTTOM_ROW = 0;
    private static final int TOP_ROW = 2;
    /** Must stay identical to WorldPanelRenderer.begin(...). */
    private static final double PANEL_RENDER_SCALE = 0.021D;

    private boolean assembling;
    private boolean dismantling;

    public SettingsPanelBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(COLUMN, 0)
                .with(ROW, BOTTOM_ROW));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (world.isClient || assembling) return;

        Direction facing = placer == null ? Direction.NORTH : placer.getHorizontalFacing().getOpposite();
        Direction right = facing.rotateYCounterclockwise();

        for (int row = BOTTOM_ROW; row <= TOP_ROW; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos part = pos.offset(right, column).up(row);
                if (!part.equals(pos) && !world.getBlockState(part).isReplaceable()) {
                    world.breakBlock(pos, true);
                    return;
                }
            }
        }

        assembling = true;
        try {
            for (int row = BOTTOM_ROW; row <= TOP_ROW; row++) {
                for (int column = 0; column < 3; column++) {
                    BlockPos part = pos.offset(right, column).up(row);
                    if (part.equals(pos) || world.getBlockState(part).isReplaceable()) {
                        world.setBlockState(part, getDefaultState()
                                .with(FACING, facing)
                                .with(COLUMN, column)
                                .with(ROW, row), 3);
                    }
                }
            }
        } finally {
            assembling = false;
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState replacement, boolean moved) {
        if (!state.isOf(replacement.getBlock()) && !world.isClient && !assembling && !dismantling) {
            Direction right = state.get(FACING).rotateYCounterclockwise();
            BlockPos origin = pos.offset(right, -state.get(COLUMN)).down(state.get(ROW));
            dismantling = true;
            try {
                for (int row = BOTTOM_ROW; row <= TOP_ROW; row++) {
                    for (int column = 0; column < 3; column++) {
                        BlockPos part = origin.offset(right, column).up(row);
                        if (!part.equals(pos) && world.getBlockState(part).isOf(this)) {
                            world.removeBlock(part, false);
                        }
                    }
                }
            } finally {
                dismantling = false;
            }
        }
        super.onStateReplaced(state, world, pos, replacement, moved);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity)) return ActionResult.PASS;

        PanelHitboxConfig.Arrow arrow = findClickedArrow(state, pos, hit);
        if (arrow == null) {
            // The physical 3x3 panel is still one multiblock, but only the four
            // configured rectangles are interactive now.
            return ActionResult.SUCCESS;
        }

        switch (arrow) {
            case TIMER_UP -> GameConfig.setRoundSeconds(Math.max(30,
                    Math.min(3600, GameConfig.ROUND_SECONDS + 30)));
            case TIMER_DOWN -> GameConfig.setRoundSeconds(Math.max(30,
                    Math.min(3600, GameConfig.ROUND_SECONDS - 30)));
            case HEARTS_UP -> GameConfig.setSeekerHearts(Math.max(1,
                    Math.min(100, GameConfig.SEEKER_HEARTS + 1)));
            case HEARTS_DOWN -> GameConfig.setSeekerHearts(Math.max(1,
                    Math.min(100, GameConfig.SEEKER_HEARTS - 1)));
        }

        GameSettingsConfig.save();
        MaskNetworking.broadcastPanelData(player.getServer());
        return ActionResult.SUCCESS;
    }

    private static PanelHitboxConfig.Arrow findClickedArrow(BlockState state, BlockPos pos, BlockHitResult hit) {
        Direction facing = state.get(FACING);
        // The visual controls live on the front surface only. Side/back clicks
        // must not accidentally change values.
        if (hit.getSide() != facing) return null;

        Direction right = facing.rotateYCounterclockwise();
        BlockPos origin = pos.offset(right, -state.get(COLUMN)).down(state.get(ROW));

        // Same origin/scale as WorldPanelRenderer.begin(): the renderer is
        // centered on column 1 and halfway up the 3-block panel.
        Vec3d center = new Vec3d(
                origin.getX() + 0.5D + right.getOffsetX(),
                origin.getY() + 1.5D,
                origin.getZ() + 0.5D + right.getOffsetZ());
        Vec3d delta = hit.getPos().subtract(center);

        double horizontal = delta.x * right.getOffsetX() + delta.z * right.getOffsetZ();
        double panelX = horizontal / PANEL_RENDER_SCALE;
        // Renderer applies a negative Y scale, hence the sign flip.
        double panelY = -delta.y / PANEL_RENDER_SCALE;
        return PanelHitboxConfig.hit(panelX, panelY);
    }
}
