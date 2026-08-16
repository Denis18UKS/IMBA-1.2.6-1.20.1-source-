package fable.hideseek.imba.block;

import fable.hideseek.imba.config.GameSettingsConfig;
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
import net.minecraft.world.World;

public final class SettingsPanelBlock extends Block {
    public static final DirectionProperty FACING = net.minecraft.state.property.Properties.HORIZONTAL_FACING;
    public static final IntProperty COLUMN = IntProperty.of("column", 0, 2);
    public static final IntProperty ROW = IntProperty.of("row", 0, 2);
    private boolean assembling;
    private boolean dismantling;

    public SettingsPanelBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(COLUMN, 0).with(ROW, 0));
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING, COLUMN, ROW); }

    @Override public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (world.isClient || assembling) return;
        Direction facing = placer == null ? Direction.NORTH : placer.getHorizontalFacing().getOpposite();
        Direction right = facing.rotateYCounterclockwise();
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            BlockPos part = pos.offset(right, column).up(row);
            if (!part.equals(pos) && !world.getBlockState(part).isReplaceable()) { world.breakBlock(pos, true); return; }
        }
        assembling = true;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            BlockPos part = pos.offset(right, column).up(row);
            if (part.equals(pos) || world.getBlockState(part).isReplaceable())
                world.setBlockState(part, getDefaultState().with(FACING, facing).with(COLUMN, column).with(ROW, row), 3);
        }
        assembling = false;
    }

    @Override public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.isOf(replacement.getBlock()) && !world.isClient && !assembling && !dismantling) {
            Direction right = state.get(FACING).rotateYCounterclockwise();
            BlockPos origin = pos.offset(right, -state.get(COLUMN)).down(state.get(ROW));
            dismantling = true;
            try {
                for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
                    BlockPos part = origin.offset(right, x).up(y);
                    if (!part.equals(pos) && world.getBlockState(part).isOf(this)) world.removeBlock(part, false);
                }
            } finally { dismantling = false; }
        }
        super.onStateReplaced(state, world, pos, replacement, moved);
    }

    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity)) return ActionResult.PASS;
        int column = state.get(COLUMN), row = state.get(ROW);
        if (row == 1) return ActionResult.SUCCESS;

        // ROW=0 — физически нижний ряд панели, ROW=2 — верхний.
        // Верхняя стрелка увеличивает значение, нижняя уменьшает.
        int direction = row == 2 ? 1 : -1;

        if (column == 0) GameConfig.setRoundSeconds(Math.max(30, Math.min(3600, GameConfig.ROUND_SECONDS + direction * 30)));
        else if (column == 2) GameConfig.setSeekerHearts(Math.max(1, Math.min(100, GameConfig.SEEKER_HEARTS + direction)));
        else return ActionResult.SUCCESS;
        GameSettingsConfig.save();
        MaskNetworking.broadcastPanelData(player.getServer());
        return ActionResult.SUCCESS;
    }
}
