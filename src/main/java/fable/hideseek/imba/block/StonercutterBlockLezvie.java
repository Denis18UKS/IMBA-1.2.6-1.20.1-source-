package fable.hideseek.imba.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class StonercutterBlockLezvie extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0.0, 9.0, 5.0, 16.0, 21.0, 6.0);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0.0, 9.0, 10.0, 16.0, 21.0, 11.0);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(10.0, 9.0, 0.0, 11.0, 21.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(5.0, 9.0, 0.0, 6.0, 21.0, 16.0);

    public StonercutterBlockLezvie(Settings settings) {
        super(settings.nonOpaque()); // ВАЖНО: nonOpaque для прозрачности
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = state.get(FACING);
        return switch (direction) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}