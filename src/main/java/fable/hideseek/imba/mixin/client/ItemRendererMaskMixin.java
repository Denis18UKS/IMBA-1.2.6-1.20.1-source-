package fable.hideseek.imba.mixin.client;

import fable.hideseek.imba.ImbaMod;
import fable.hideseek.imba.client.VanillaMaskRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
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
 * Renders the special 2D potion mask through BlockModelRenderer while the mask
 * context is active. This gives its quads the same world/block lighting path as
 * the bottles that are part of a real brewing-stand block model.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMaskMixin {

    @Shadow
    public abstract BakedModel getModel(ItemStack stack, World world, LivingEntity entity, int seed);

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void imba$renderPotionMaskAsWorldModel(
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
        BakedModel model = getModel(stack, renderWorld, null, seed);
        BlockState lightingState = Blocks.BREWING_STAND.getDefaultState();

        matrices.push();
        model.getTransformation().getTransformation(transformationType).apply(false, matrices);
        matrices.translate(-0.5D, -0.5D, -0.5D);

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getCutout());
        MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer().render(
                renderWorld,
                model,
                lightingState,
                context.pos(),
                matrices,
                vertices,
                false,
                Random.create(lightingState.getRenderingSeed(context.pos())),
                lightingState.getRenderingSeed(context.pos()),
                overlay);

        matrices.pop();
        ci.cancel();
    }
}
