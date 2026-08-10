package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.client.VanillaMaskRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The special potion mask keeps the item's existing transforms/size, but its
 * pixels are rendered as a real world block model. This avoids entity/item
 * lighting being darker than the bottles in a nearby brewing stand.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMaskMixin {

    @Shadow
    public abstract BakedModel getModel(ItemStack stack, World world, LivingEntity entity, int seed);

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void imba$renderPotionMaskAsWorldBlock(
            ItemStack stack,
            ModelTransformationMode transformationType,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int seed,
            CallbackInfo ci) {
        VanillaMaskRenderContext.Context context = VanillaMaskRenderContext.get();
        if (context == null
                || context.mode() != VanillaMaskRenderContext.Mode.POTION_ITEM_AS_BLOCK
                || !stack.isOf(ImbaMod.POTION_2D)) {
            return;
        }

        World renderWorld = context.world();
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel itemModel = getModel(stack, renderWorld, null, seed);
        BlockState renderState = ImbaMod.POTION_RENDER_BLOCK.getDefaultState();

        matrices.push();
        // Preserve the exact old item placement/scale so this change affects
        // lighting only, not the visual size or attachment position.
        itemModel.getTransformation().getTransformation(transformationType).apply(false, matrices);
        matrices.translate(-0.5D, -0.5D, -0.5D);

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayers.getBlockLayer(renderState));
        client.getBlockRenderManager().renderBlock(
                renderState,
                context.pos(),
                renderWorld,
                matrices,
                vertices,
                false,
                Random.create(renderState.getRenderingSeed(context.pos())));

        matrices.pop();
        ci.cancel();
    }
}
