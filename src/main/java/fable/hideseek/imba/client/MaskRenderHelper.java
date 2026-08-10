package fable.hideseek.imba.client;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import java.util.UUID;

public final class MaskRenderHelper {

    private MaskRenderHelper() {
    }

    public static void renderMask(UUID uuid, MatrixStack matrices, VertexConsumerProvider consumers, int light,
            World renderWorld, BlockPos renderPos) {
        if (!ClientMaskData.hasMask(uuid)) {
            return;
        }

        MaskType type = ClientMaskData.TYPES.get(uuid);
        if (type == null || type == MaskType.NONE) {
            return;
        }

        float rotationY = ClientMaskData.ROTATIONS.getOrDefault(uuid, 0f);
        float rotationX = ClientMaskData.ROTATIONS_X.getOrDefault(uuid, 0f);
        float rotationZ = ClientMaskData.ROTATIONS_Z.getOrDefault(uuid, 0f);

        boolean doorOpen = ClientMaskData.DOOR_OPEN.getOrDefault(uuid, false);
        boolean buttonPressed = ClientMaskData.BUTTON_PRESSED.getOrDefault(uuid, false);
        boolean attachedToFrame = ClientMaskData.ATTACHED_TO_FRAME.getOrDefault(uuid, false);
        Direction attachmentFacing = ClientMaskData.ATTACHMENT_FACING.getOrDefault(uuid, Direction.NORTH);
        int frameRotationStep = ClientMaskData.FRAME_ROTATION_STEP.getOrDefault(uuid, 0);
        boolean statue = ClientMaskData.isStatue(uuid);

        matrices.push();
        matrices.translate(-0.5D, 0.0D, -0.5D);

        switch (type) {
            case DOOR -> renderDoor(uuid, matrices, consumers, light, rotationY, doorOpen);
            case PORTAL -> {
                applyCenteredRotation(matrices, rotationX, rotationY, rotationZ);
                renderBlock(matrices, consumers, light, Blocks.NETHER_PORTAL.getDefaultState());
            }
            case LADDER_REVERSED -> renderLadder(matrices, consumers, light, rotationY, renderWorld, renderPos);
            case BUTTON -> renderButton(uuid, matrices, consumers, light, rotationY, buttonPressed);
            case SCULK_VEIN -> {
                applyCenteredRotation(matrices, rotationX, rotationY, rotationZ);
                renderBlock(matrices, consumers, light, Blocks.SCULK_VEIN.getDefaultState()
                        .with(net.minecraft.block.MultifaceGrowthBlock.getProperty(Direction.DOWN), true));
            }
            case LANTERN -> {
                applyCenteredRotation(matrices, rotationX, rotationY, rotationZ);
                Block lantern = ClientMaskData.BLOCKS.get(uuid);
                BlockState lanternState = lantern == null ? Blocks.LANTERN.getDefaultState() : lantern.getDefaultState();
                if (lanternState.contains(net.minecraft.block.LanternBlock.HANGING)) {
                    lanternState = lanternState.with(
                            net.minecraft.block.LanternBlock.HANGING,
                            lantern == ImbaMod.HANGING_LANTERN);
                }
                renderBlock(matrices, consumers, light, lanternState);
            }
            case STEM -> {
                applyCenteredRotation(matrices, rotationX, rotationY, rotationZ);
                renderBlock(matrices, consumers, light, Blocks.ATTACHED_PUMPKIN_STEM.getDefaultState());
            }
            case BLOCK -> {
                Block block = ClientMaskData.BLOCKS.get(uuid);
                if (block != null) {
                    BlockState renderState = block == Blocks.WATER
                            ? ImbaMod.WATER_MASK.getDefaultState()
                            : block == Blocks.LAVA
                                    ? ImbaMod.LAVA_MASK.getDefaultState()
                                    : block.getDefaultState();
                    if (block == ImbaMod.STONRCUTTER_LEZVIE) {
                        applyCenteredRotation(matrices, rotationX, rotationY, rotationZ);
                        renderBlock(matrices, consumers, light, ImbaMod.STONRCUTTER_BLOCK.getDefaultState());
                        matrices.push();
                        matrices.translate(0.0D, 9.0D / 16.0D, 0.0D);
                        renderBlock(matrices, consumers, light, renderState);
                        matrices.pop();
                    } else {
                        applyCenteredRotation(matrices, rotationX, rotationY, rotationZ);
                        if (block == ImbaMod.GLOWBERRIES) {
                            renderWorldAwareBlock(matrices, consumers, light, renderState, renderWorld, renderPos);
                        } else {
                            renderBlock(matrices, consumers, light, renderState);
                        }
                    }
                }
            }
            case ITEM -> {
                var item = ClientMaskData.ITEMS.get(uuid);
                if (item != null) {
                    if (MaskService.isSpecialPotion(item)) {
                        renderPotionStanding(matrices, consumers, light, new ItemStack(item), statue,
                                renderWorld, renderPos);
                    } else {
                        renderWallItem(
                                matrices,
                                consumers,
                                light,
                                new ItemStack(item),
                                rotationY,
                                attachmentFacing,
                                attachedToFrame,
                                frameRotationStep);
                    }
                }
            }
            case WALL_CLIMB -> renderWallItem(
                    matrices,
                    consumers,
                    light,
                    new ItemStack(Items.APPLE),
                    rotationY,
                    attachmentFacing,
                    attachedToFrame,
                    frameRotationStep);
        }

        matrices.pop();
    }

    private static void applyCenteredRotation(MatrixStack matrices, float rotationX, float rotationY, float rotationZ) {
        matrices.translate(0.5D, 0.5D, 0.5D);

        if (rotationX != 0f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationX));
        }
        if (rotationY != 0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        }
        if (rotationZ != 0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationZ));
        }

        matrices.translate(-0.5D, -0.5D, -0.5D);
    }

    private static Direction rotationToDirection(float rotationY) {
        int index = Math.floorMod(Math.round(rotationY / 90.0f), 4);
        Direction direction = Direction.fromHorizontal(index);
        return direction == null ? Direction.NORTH : direction;
    }

    private static void renderBlock(MatrixStack matrices, VertexConsumerProvider consumers, int light,
            BlockState state) {
        matrices.push();
        MinecraftClient.getInstance().getBlockRenderManager()
                .renderBlockAsEntity(state, matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    private static void renderWorldAwareBlock(MatrixStack matrices, VertexConsumerProvider consumers, int light,
            BlockState state, World renderWorld, BlockPos renderPos) {
        if (renderWorld == null || renderPos == null) {
            renderBlock(matrices, consumers, light, state);
            return;
        }
        VanillaMaskRenderContext.begin(
                VanillaMaskRenderContext.Mode.WORLD_BLOCK,
                renderWorld,
                renderPos);
        try {
            renderBlock(matrices, consumers, light, state);
        } finally {
            VanillaMaskRenderContext.clear();
        }
    }

    private static void renderDoor(UUID uuid, MatrixStack matrices, VertexConsumerProvider consumers, int light,
            float rotationY, boolean open) {
        Block doorBlock = ClientMaskData.BLOCKS.get(uuid);
        if (!(doorBlock instanceof DoorBlock)) {
            doorBlock = Blocks.OAK_DOOR;
        }

        Direction facing = rotationToDirection(rotationY);

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

        matrices.push();
        MinecraftClient.getInstance().getBlockRenderManager()
                .renderBlockAsEntity(lower, matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();

        matrices.push();
        matrices.translate(0.0D, 1.0D, 0.0D);
        MinecraftClient.getInstance().getBlockRenderManager()
                .renderBlockAsEntity(upper, matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    private static void renderButton(UUID uuid, MatrixStack matrices, VertexConsumerProvider consumers, int light,
            float rotationY, boolean pressed) {
        Direction facing = rotationToDirection(rotationY);
        Block source = ClientMaskData.BLOCKS.get(uuid);
        if (!(source instanceof ButtonBlock)) {
            source = Blocks.STONE_BUTTON;
        }

        BlockState state = source.getDefaultState()
                .with(WallMountedBlock.FACING, facing)
                .with(WallMountedBlock.FACE, WallMountLocation.FLOOR)
                .with(ButtonBlock.POWERED, pressed);

        renderBlock(matrices, consumers, light, state);
    }

    private static void renderLadder(MatrixStack matrices, VertexConsumerProvider consumers, int light,
            float rotationY, World renderWorld, BlockPos renderPos) {
        Direction facing = rotationToDirection(rotationY);
        BlockState state = Blocks.LADDER.getDefaultState()
                .with(net.minecraft.block.LadderBlock.FACING, facing);

        matrices.push();
        matrices.translate(0.5D, 0.5D, 0.5D);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        matrices.translate(-0.5D, -0.5D, -0.5D);
        renderWorldAwareBlock(matrices, consumers, light, state, renderWorld, renderPos);
        matrices.pop();
    }

    private static void renderWallItem(MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light,
            ItemStack stack,
            float rotationY,
            Direction attachmentFacing,
            boolean attachedToFrame,
            int frameRotationStep) {
        Direction facing = attachedToFrame
                ? (attachmentFacing == null ? Direction.NORTH : attachmentFacing)
                : rotationToDirection(rotationY);

        matrices.push();
        matrices.translate(0.5D, 0.5D, 0.5D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));
        matrices.translate(0.0D, 0.0D, 0.4375D);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(frameRotationStep * 45.0f));
        matrices.scale(0.5f, 0.5f, 0.5f);

        MinecraftClient.getInstance().getItemRenderer()
                .renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
                        matrices, consumers, null, 0);

        matrices.pop();
    }

    private static void renderPotionStanding(MatrixStack matrices, VertexConsumerProvider consumers,
            int light, ItemStack stack, boolean statue, World renderWorld, BlockPos renderPos) {
        MinecraftClient client = MinecraftClient.getInstance();

        matrices.push();
        matrices.translate(0.5D, statue ? 0.15D : 0.24D, statue ? 0.53125D : 0.5D);
        matrices.scale(0.5f, 0.5f, 0.5f);

        if (renderWorld != null && renderPos != null) {
            /*
             * renderPos is the air cell occupied by the visible potion, not
             * the brewing-stand/support block below it. Sampling the support
             * cell is what made statue mode darker than the real bottle.
             */
            VanillaMaskRenderContext.begin(
                    VanillaMaskRenderContext.Mode.POTION_ITEM_AS_BLOCK,
                    renderWorld,
                    renderPos);
        }
        try {
            client.getItemRenderer().renderItem(
                    stack,
                    ModelTransformationMode.GROUND,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    consumers,
                    renderWorld,
                    0);
        } finally {
            VanillaMaskRenderContext.clear();
        }

        matrices.pop();
    }
}
