package fable.hideseek.imba.net;

import fable.hideseek.imba.ImbaExtension;
import fable.hideseek.imba.config.RoundRestoreConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class RoundRestoreNetworking {
    public static final Identifier REQUEST = new Identifier("imba", "restore_request");
    public static final Identifier SAVE_LAYER = new Identifier("imba", "restore_layer_save");
    public static final Identifier TOGGLE_LAYER = new Identifier("imba", "restore_layer_toggle");
    public static final Identifier DELETE_LAYER = new Identifier("imba", "restore_layer_delete");
    public static final Identifier RESTORE_LAYER = new Identifier("imba", "restore_layer_now");
    public static final Identifier RENAME_LAYER = new Identifier("imba", "restore_layer_rename");
    public static final Identifier RESTORE_SINGLE = new Identifier("imba", "restore_single_now");
    public static final Identifier RECAPTURE_SINGLE = new Identifier("imba", "restore_single_recapture");
    public static final Identifier DELETE_SINGLE = new Identifier("imba", "restore_single_delete");
    public static final Identifier SYNC = new Identifier("imba", "restore_sync");

    private RoundRestoreNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST,
                (server, player, handler, buf, sender) -> server.execute(() -> {
                    if (player.hasPermissionLevel(2)) sendSync(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(SAVE_LAYER,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    String name = buf.readString(64);
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        ItemStack tool = findTool(player);
                        Selection selection = readSelection(tool);
                        if (selection == null) {
                            player.sendMessage(Text.literal("§cСначала укажите точки A (ПКМ) и B (Shift+ПКМ) в одном мире"), true);
                            return;
                        }
                        ServerWorld world = server.getWorld(selection.worldKey());
                        if (world == null) return;
                        long volume = selection.volume();
                        if (volume > RoundRestoreConfig.MAX_LAYER_BLOCKS) {
                            player.sendMessage(Text.literal("§cСлой слишком большой: §f" + volume
                                    + " §cблоков (макс. " + RoundRestoreConfig.MAX_LAYER_BLOCKS + ")"), true);
                            return;
                        }
                        var meta = RoundRestoreConfig.saveLayer(world, selection.a(), selection.b(), index, name);
                        if (meta == null) {
                            player.sendMessage(Text.literal("§cНе удалось сохранить слой"), true);
                            return;
                        }
                        player.sendMessage(Text.literal("§aСохранён §f" + meta.name()
                                + " §7(" + meta.blockCount() + " блоков)"), true);
                        sendSync(player);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_LAYER,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    boolean enabled = buf.readBoolean();
                    server.execute(() -> {
                        if (player.hasPermissionLevel(2) && RoundRestoreConfig.setLayerEnabled(index, enabled)) {
                            sendSync(player);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(RENAME_LAYER,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    String name = buf.readString(64);
                    server.execute(() -> {
                        if (player.hasPermissionLevel(2) && RoundRestoreConfig.renameLayer(index, name)) {
                            player.sendMessage(Text.literal("§aСлой переименован"), true);
                            sendSync(player);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(DELETE_LAYER,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    server.execute(() -> {
                        if (player.hasPermissionLevel(2) && RoundRestoreConfig.deleteLayer(index)) {
                            player.sendMessage(Text.literal("§eСлой удалён"), true);
                            sendSync(player);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(RESTORE_LAYER,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        boolean ok = RoundRestoreConfig.restoreLayer(server, index);
                        player.sendMessage(Text.literal(ok ? "§aВыбранный слой восстановлен" : "§cНе удалось восстановить слой"), true);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(RESTORE_SINGLE,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        boolean ok = RoundRestoreConfig.restoreSingle(server, index);
                        player.sendMessage(Text.literal(ok ? "§aТочка восстановлена" : "§cНе удалось восстановить точку"), true);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(RECAPTURE_SINGLE,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        boolean ok = RoundRestoreConfig.recaptureSingle(server, index);
                        player.sendMessage(Text.literal(ok ? "§aТочка перезаписана текущим состоянием" : "§cНе удалось перезаписать точку"), true);
                        if (ok) sendSync(player);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(DELETE_SINGLE,
                (server, player, handler, buf, sender) -> {
                    int index = buf.readInt();
                    server.execute(() -> {
                        if (player.hasPermissionLevel(2) && RoundRestoreConfig.deleteSingle(index)) {
                            player.sendMessage(Text.literal("§eТочка восстановления удалена"), true);
                            sendSync(player);
                        }
                    });
                });
    }

    private static ItemStack findTool(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ImbaExtension.STRUCTURE_LAYER_TOOL)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static Selection readSelection(ItemStack tool) {
        if (tool.isEmpty() || tool.getNbt() == null) return null;
        NbtCompound nbt = tool.getNbt();
        if (!nbt.contains("imba_layer_a_world") || !nbt.contains("imba_layer_b_world")) return null;
        String aWorld = nbt.getString("imba_layer_a_world");
        String bWorld = nbt.getString("imba_layer_b_world");
        if (!aWorld.equals(bWorld)) return null;
        Identifier id = Identifier.tryParse(aWorld);
        if (id == null) return null;
        var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, id);
        BlockPos a = new BlockPos(nbt.getInt("imba_layer_a_x"), nbt.getInt("imba_layer_a_y"), nbt.getInt("imba_layer_a_z"));
        BlockPos b = new BlockPos(nbt.getInt("imba_layer_b_x"), nbt.getInt("imba_layer_b_y"), nbt.getInt("imba_layer_b_z"));
        return new Selection(key, a, b);
    }

    public static void sendSync(ServerPlayerEntity player) {
        List<RoundRestoreConfig.SingleMeta> singles = RoundRestoreConfig.singleMetadata();
        List<RoundRestoreConfig.LayerMeta> layers = RoundRestoreConfig.layerMetadata();
        PacketByteBuf out = PacketByteBufs.create();

        out.writeVarInt(singles.size());
        for (var single : singles) {
            out.writeInt(single.index());
            out.writeString(single.world(), 128);
            out.writeInt(single.x());
            out.writeInt(single.y());
            out.writeInt(single.z());
            out.writeString(single.block(), 128);
            out.writeVarInt(single.inventoryItems());
        }

        out.writeVarInt(layers.size());
        for (var layer : layers) {
            out.writeInt(layer.index());
            out.writeString(layer.name(), 64);
            out.writeBoolean(layer.enabled());
            out.writeVarInt(layer.blockCount());
            out.writeString(layer.world(), 128);
            out.writeInt(layer.minX());
            out.writeInt(layer.minY());
            out.writeInt(layer.minZ());
            out.writeInt(layer.maxX());
            out.writeInt(layer.maxY());
            out.writeInt(layer.maxZ());
        }
        ServerPlayNetworking.send(player, SYNC, out);
    }

    private record Selection(net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey, BlockPos a, BlockPos b) {
        long volume() {
            return (long) (Math.abs(a.getX() - b.getX()) + 1)
                    * (Math.abs(a.getY() - b.getY()) + 1)
                    * (Math.abs(a.getZ() - b.getZ()) + 1);
        }
    }
}
