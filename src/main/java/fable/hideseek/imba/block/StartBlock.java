package fable.hideseek.imba.block;

import fable.hideseek.imba.block.entity.StartBlockEntity;
import fable.hideseek.imba.game.GameManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class StartBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = net.minecraft.state.property.Properties.HORIZONTAL_FACING;

    public StartBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StartBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
            Hand hand, BlockHitResult hit) {
        if (player.isSneaking()) return ActionResult.SUCCESS;
        if (world.isClient) return ActionResult.SUCCESS;
        if (GameManager.isGameActive()) {
            player.sendMessage(Text.literal("§cИгра уже запущена"), true);
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameManager.startNextRound(world.getServer(), serverPlayer);
        }
        return ActionResult.SUCCESS;
    }
}
