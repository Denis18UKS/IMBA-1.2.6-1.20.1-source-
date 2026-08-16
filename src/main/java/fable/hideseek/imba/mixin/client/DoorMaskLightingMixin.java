package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.MaskRenderHelper;
import fable.hideseek.imba.client.VanillaMaskRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** Door disguises use the same world-aware block renderer as real doors. */
@Mixin(MaskRenderHelper.class)
public abstract class DoorMaskLightingMixin {
    @Inject(method = "renderDoor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void imba$renderDoorWithVanillaWorldLight(UUID uuid, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, float rotationY, boolean open, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        PlayerEntity player = client.world.getPlayerByUuid(uuid);
        if (player == null) return;

        Block doorBlock = ClientMaskData.BLOCKS.get(uuid);
        if (!(doorBlock instanceof DoorBlock)) doorBlock = Blocks.OAK_DOOR;
        int horizontal = Math.floorMod(Math.round(rotationY / 90.0F), 4);
        Direction facing = Direction.fromHorizontal(horizontal);
        if (facing == null) facing = Direction.NORTH;

        BlockState lower = doorBlock.getDefaultState()
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .with(DoorBlock.FACING, facing)
                .with(DoorBlock.OPEN, open)
                .with(DoorBlock.HINGE, DoorHinge.LEFT);
        BlockState upper = doorBlock.getDefaultState()
                .with(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .with(DoorBlock.FACING, facing)
                .with(DoorBlock.OPEN, open)
                .with(DoorBlock.HINGE, DoorHinge.LEFT);

        BlockPos lowerPos = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ());
        renderWorldAware(client, matrices, consumers, light, lower, lowerPos);
        matrices.push();
        matrices.translate(0.0D, 1.0D, 0.0D);
        renderWorldAware(client, matrices, consumers, light, upper, lowerPos.up());
        matrices.pop();
        ci.cancel();
    }

    private static void renderWorldAware(MinecraftClient client, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, BlockState state, BlockPos pos) {
        VanillaMaskRenderContext.begin(VanillaMaskRenderContext.Mode.WORLD_BLOCK, client.world, pos);
        try {
            client.getBlockRenderManager().renderBlockAsEntity(
                    state, matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        } finally {
            VanillaMaskRenderContext.clear();
        }
    }
}
