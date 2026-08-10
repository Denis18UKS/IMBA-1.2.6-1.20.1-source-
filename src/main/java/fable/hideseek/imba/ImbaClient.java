package fable.hideseek.imba;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.KeyBindings;
import fable.hideseek.imba.client.TeleportSetupScreen;
import fable.hideseek.imba.client.PotionOffsetScreen;
import fable.hideseek.imba.client.LocationCameraScreen;
import fable.hideseek.imba.client.MaskBlockConfigScreen;
import fable.hideseek.imba.client.MaskBlockConfigClientNetworking;
import fable.hideseek.imba.client.ClientPhotoCapture;
import fable.hideseek.imba.client.StartBlockNameScreen;
import fable.hideseek.imba.client.WorldPanelRenderer;
import fable.hideseek.imba.client.ClientMaskCollision;
import fable.hideseek.imba.client.ModelTokenRenderer;
import fable.hideseek.imba.net.MaskClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.TypedActionResult;

public class ImbaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MaskClientNetworking.registerClient();
        KeyBindings.register();
        ModelTokenRenderer.register();
        MaskBlockConfigClientNetworking.register();

        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.GLOWBERRIES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.GRASS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.LADDER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.STONRCUTTER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.STONRCUTTER_LEZVIE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.WATER_MASK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.LAVA_MASK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.LADDER_MASK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ImbaMod.POTION_RENDER_BLOCK, RenderLayer.getCutout());
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> 0x3F76E4, ImbaMod.WATER_MASK);
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> 0xFFFFFF, ImbaMod.LAVA_MASK);
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> 0x79C05A, ImbaMod.GRASS.asItem());
        WorldPanelRenderer.register();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient && player.getStackInHand(hand).isOf(ImbaMod.TELEPORT_TOOL)) {
                net.minecraft.client.MinecraftClient.getInstance().setScreen(new TeleportSetupScreen());
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            if (world.isClient && player.getStackInHand(hand).isOf(ImbaMod.POTION_OFFSET_TOOL)) {
                net.minecraft.client.MinecraftClient.getInstance().setScreen(new PotionOffsetScreen());
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            if (world.isClient && player.getStackInHand(hand).isOf(ImbaMod.LOCATION_CAMERA)) {
                net.minecraft.client.MinecraftClient.getInstance().setScreen(new LocationCameraScreen());
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            if (world.isClient && player.getStackInHand(hand).isOf(ImbaMod.MASK_BLOCK_CONFIG_TOOL)) {
                net.minecraft.client.MinecraftClient.getInstance().setScreen(new MaskBlockConfigScreen());
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient && player.isSneaking()
                    && world.getBlockState(hit.getBlockPos()).isOf(ImbaMod.START_BLOCK)) {
                net.minecraft.client.MinecraftClient.getInstance()
                        .setScreen(new StartBlockNameScreen(hit.getBlockPos()));
                return net.minecraft.util.ActionResult.SUCCESS;
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPhotoCapture.tick(client);
            ClientMaskCollision.tick(client);
            if (client.player != null) {
                boolean hasMask = ClientMaskData.hasMask(client.player.getUuid());
                boolean hasInvisibility = client.player.hasStatusEffect(StatusEffects.INVISIBILITY);

                if (hasMask && !hasInvisibility) {
                    client.player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.INVISIBILITY,
                            StatusEffectInstance.INFINITE,
                            0, false, false, false));
                } else if (!hasMask && hasInvisibility) {
                    client.player.removeStatusEffect(StatusEffects.INVISIBILITY);
                }
            }
        });
    }
}
