package fable.hideseek.imba.client;

import fable.hideseek.imba.net.MaskNetworking;
import fable.hideseek.imba.net.WallClimbNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

        public static KeyBinding rotateKey;
        public static KeyBinding rotateXKey;
        public static KeyBinding rotateZKey;
        public static KeyBinding wallClimbKey;

        private static boolean attackPressedLastTick = false;
        private static boolean usePressedLastTick = false;

        public static void register() {
                rotateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                                "key.imba.rotate",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_R,
                                "category.imba.mask"));

                rotateXKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                                "key.imba.rotate_x",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_X,
                                "category.imba.mask"));

                rotateZKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                                "key.imba.rotate_z",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_Z,
                                "category.imba.mask"));

                wallClimbKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                                "key.imba.wall_climb",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_C,
                                "category.imba.mask"));

                ClientTickEvents.END_CLIENT_TICK.register(client -> {
                        if (client.player == null) {
                                attackPressedLastTick = false;
                                usePressedLastTick = false;
                                return;
                        }

                        while (rotateKey.wasPressed()) {
                                ClientPlayNetworking.send(MaskNetworking.ROTATE_PACKET, PacketByteBufs.empty());
                        }

                        while (rotateXKey.wasPressed()) {
                                var buf = PacketByteBufs.create();
                                buf.writeString("x");
                                ClientPlayNetworking.send(MaskNetworking.ROTATE_AXIS_PACKET, buf);
                        }

                        while (rotateZKey.wasPressed()) {
                                var buf = PacketByteBufs.create();
                                buf.writeString("z");
                                ClientPlayNetworking.send(MaskNetworking.ROTATE_AXIS_PACKET, buf);
                        }

                        while (wallClimbKey.wasPressed()) {
                                ClientPlayNetworking.send(WallClimbNetworking.TOGGLE, PacketByteBufs.empty());
                        }

                        boolean attackNow = client.options.attackKey.isPressed();
                        boolean useNow = client.options.useKey.isPressed();

                        if (client.currentScreen == null) {
                                if (attackNow && !attackPressedLastTick) {
                                        if (client.crosshairTarget instanceof BlockHitResult blockHit) {
                                                var buf = PacketByteBufs.create();
                                                buf.writeBlockPos(blockHit.getBlockPos());
                                                ClientPlayNetworking.send(MaskNetworking.SEEKER_BLOCK_ATTACK_PACKET, buf);
                                        } else if (client.crosshairTarget == null
                                                        || client.crosshairTarget.getType() == HitResult.Type.MISS) {
                                                ClientPlayNetworking.send(MaskNetworking.SEEKER_MISS_PACKET,
                                                                PacketByteBufs.empty());
                                        }
                                }

                                if (useNow && !usePressedLastTick) {
                                        ClientPlayNetworking.send(MaskNetworking.SEEKER_USE_PACKET,
                                                        PacketByteBufs.empty());
                                }
                        }

                        attackPressedLastTick = attackNow;
                        usePressedLastTick = useNow;
                });
        }
}
