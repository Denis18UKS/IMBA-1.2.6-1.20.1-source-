package fable.hideseek.imba.net;

import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Dedicated network channel for wall-climb mode, isolated from mask packets. */
public final class WallClimbNetworking implements ModInitializer {
    public static final Identifier TOGGLE = new Identifier("imba", "wall_climb_toggle_v2");
    public static final Identifier STATE = new Identifier("imba", "wall_climb_state");

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!MaskState.hasMask(player.getUuid())) {
                        player.sendMessage(Text.literal("§cСначала наденьте маску"), true);
                        return;
                    }

                    MaskState state = MaskState.get(player.getUuid());
                    if (!MaskService.supportsWallClimbing(state)) {
                        player.sendMessage(Text.literal("§cЭта маска не умеет ползать по стенам"), true);
                        return;
                    }

                    state.wallClimbing = !state.wallClimbing;
                    PacketByteBuf stateBuf = new PacketByteBuf(Unpooled.buffer());
                    stateBuf.writeBoolean(state.wallClimbing);
                    ServerPlayNetworking.send(player, STATE, stateBuf);
                    player.sendMessage(Text.literal(state.wallClimbing
                            ? "§aПолзание по стенам: ВКЛ"
                            : "§cПолзание по стенам: ВЫКЛ"), true);
                }));
    }
}
