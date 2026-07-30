package fable.hideseek.imba.net;

import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public final class MaskNetworking {

    public static final Identifier MASK_UPDATE = new Identifier("imba", "mask_update");
    public static final Identifier MASK_RESET = new Identifier("imba", "mask_reset");
    public static final Identifier RESYNC_CLEAR = new Identifier("imba", "resync_clear");
    public static final Identifier STATUE_SYNC = new Identifier("imba", "statue_sync");

    public static final Identifier ROTATE_Y = new Identifier("imba", "rotate_y");
    public static final Identifier ROTATE_X = new Identifier("imba", "rotate_x");
    public static final Identifier ROTATE_Z = new Identifier("imba", "rotate_z");

    public static final Identifier ROTATE_PACKET = ROTATE_Y;
    public static final Identifier ROTATE_AXIS_PACKET = new Identifier("imba", "rotate_axis");
    public static final Identifier WALL_CLIMB_TOGGLE_PACKET = new Identifier("imba", "wall_climb_toggle");

    public static final Identifier SEEKER_MISS_PACKET = new Identifier("imba", "seeker_miss");
    public static final Identifier SEEKER_BLOCK_ATTACK_PACKET = new Identifier("imba", "seeker_block_attack");
    public static final Identifier SEEKER_USE_PACKET = new Identifier("imba", "seeker_use");
    public static final Identifier TELEPORT_SAVE_PACKET = new Identifier("imba", "teleport_save");
    public static final Identifier ATTACHMENT_SAVE_PACKET = new Identifier("imba", "attachment_save");
    public static final Identifier GAME_SETTINGS_PACKET = new Identifier("imba", "game_settings");
    public static final Identifier LOCATION_PHOTO_UPLOAD_PACKET = new Identifier("imba", "location_photo_upload");
    public static final Identifier LOCATION_PHOTO_DELETE_PACKET = new Identifier("imba", "location_photo_delete");
    public static final Identifier LOCATION_PHOTO_SYNC_PACKET = new Identifier("imba", "location_photo_sync");
    public static final Identifier LOCATION_SETTINGS_SAVE_PACKET =
            new Identifier("imba", "location_settings_save");
    public static final Identifier START_BLOCK_RENAME_PACKET = new Identifier("imba", "start_block_rename");
    public static final Identifier GAME_STATE_PACKET = new Identifier("imba", "game_state");
    public static final Identifier LOCATION_TELEPORT_PACKET =
            new Identifier("imba", "location_teleport");

    private MaskNetworking() {
    }

    private static boolean isFrameItem(MaskState state) {
        return (state.type == MaskType.ITEM || state.type == MaskType.WALL_CLIMB)
                && state.statue
                && !fable.hideseek.imba.mask.MaskService.isSpecialPotion(state.item);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ROTATE_Y,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!MaskState.hasMask(player.getUuid()))
                        return;

                    MaskState state = MaskState.get(player.getUuid());
                    if (isFrameItem(state)) {
                        MaskState.rotateFrameStep(player.getUuid());
                    } else {
                        MaskState.rotate(player.getUuid());
                    }

                    refresh(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(ROTATE_X,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!MaskState.hasMask(player.getUuid()))
                        return;
                    MaskState.rotateX(player.getUuid());
                    refresh(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(ROTATE_Z,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!MaskState.hasMask(player.getUuid()))
                        return;
                    MaskState.rotateZ(player.getUuid());
                    refresh(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(ROTATE_AXIS_PACKET,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!MaskState.hasMask(player.getUuid()))
                        return;

                    String axis = "y";
                    if (buf.isReadable()) {
                        axis = buf.readString();
                    }

                    MaskState state = MaskState.get(player.getUuid());

                    if ("x".equalsIgnoreCase(axis)) {
                        MaskState.rotateX(player.getUuid());
                    } else if ("z".equalsIgnoreCase(axis)) {
                        MaskState.rotateZ(player.getUuid());
                    } else {
                        if (isFrameItem(state)) {
                            MaskState.rotateFrameStep(player.getUuid());
                        } else {
                            MaskState.rotate(player.getUuid());
                        }
                    }

                    refresh(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(WALL_CLIMB_TOGGLE_PACKET,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!MaskState.hasMask(player.getUuid()))
                        return;
                    MaskState state = MaskState.get(player.getUuid());
                    state.wallClimbing = !state.wallClimbing;
                    refresh(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(SEEKER_MISS_PACKET,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    GameManager.handleSeekerMiss(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(SEEKER_BLOCK_ATTACK_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    var pos = buf.readBlockPos();
                    server.execute(() -> {
                        if (player.squaredDistanceTo(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 36.0)
                            GameManager.handleSeekerBlockAttack(player, pos);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(SEEKER_USE_PACKET,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    GameManager.handleSeekerUse(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(TELEPORT_SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    String mode = buf.readString(32);
                    double x = buf.readDouble();
                    double y = buf.readDouble();
                    double z = buf.readDouble();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2) || !isFiniteCoordinate(x, y, z)) {
                            return;
                        }
                        GameConfig.setPoint(mode, new net.minecraft.util.math.Vec3d(x, y, z));
                        player.sendMessage(net.minecraft.text.Text.literal(
                                "§aТочка сохранена: §f" + GameConfig.getPointDisplayName(mode)
                                        + " §7→ §f" + x + ", " + y + ", " + z), false);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(ATTACHMENT_SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    buf.readString(32);
                    double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2) || !isFiniteCoordinate(x, y, z)) return;
                        fable.hideseek.imba.config.AttachmentConfig.setOffset(new Identifier("imba", "potion_2d"),
                                new net.minecraft.util.math.Vec3d(x, y, z));
                        broadcastPanelData(server);
                        player.sendMessage(net.minecraft.text.Text.literal("§aАвтопозиция 2D-зелья сохранена"), true);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(GAME_SETTINGS_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    int seconds = buf.readVarInt(), hearts = buf.readVarInt(), location = buf.readVarInt();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        GameConfig.setRoundSeconds(Math.max(30, Math.min(3600, seconds)));
                        GameConfig.setSeekerHearts(Math.max(1, Math.min(100, hearts)));
                        GameConfig.setSelectedLocation(location);
                        fable.hideseek.imba.config.GameSettingsConfig.save();
                        broadcastPanelData(server);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(LOCATION_PHOTO_UPLOAD_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    int location = buf.readVarInt();
                    byte[] png = buf.readByteArray(
                            fable.hideseek.imba.config.LocationPhotoStorage.MAX_PHOTO_BYTES);
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        if (fable.hideseek.imba.config.LocationPhotoStorage.save(location, png)) {
                            broadcastPhoto(server, location, png);
                        } else {
                            player.sendMessage(net.minecraft.text.Text.literal("§cФотография отклонена"), true);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(LOCATION_PHOTO_DELETE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    int location = buf.readVarInt();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        if (location < 0) {
                            fable.hideseek.imba.config.LocationPhotoStorage.deleteAll();
                            for (int i = 0; i < fable.hideseek.imba.config.LocationPhotoStorage.LOCATION_COUNT; i++) {
                                broadcastPhoto(server, i, new byte[0]);
                            }
                            player.sendMessage(net.minecraft.text.Text.literal("§eВсе фотографии удалены"), true);
                        } else {
                            fable.hideseek.imba.config.LocationPhotoStorage.delete(location);
                            broadcastPhoto(server, location, new byte[0]);
                            player.sendMessage(net.minecraft.text.Text.literal(
                                    "§eФотография локации " + (location + 1) + " удалена"), true);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(LOCATION_SETTINGS_SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    int location = buf.readVarInt();
                    String name = buf.readString(48);
                    String kindName = buf.readString(16);
                    String maskId = buf.readString(128);
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) {
                            return;
                        }
                        try {
                            fable.hideseek.imba.game.RoundDefinition.SourceKind kind =
                                    fable.hideseek.imba.game.RoundDefinition.SourceKind.valueOf(kindName);
                            boolean saved = GameConfig.setLocationSettings(
                                    location, name, kind, new Identifier(maskId));
                            if (!saved) {
                                player.sendMessage(net.minecraft.text.Text.literal(
                                        "§cНе удалось сохранить маску локации"), true);
                                return;
                            }
                            fable.hideseek.imba.config.LocationSettingsConfig.save();
                            broadcastPanelData(server);
                            player.sendMessage(net.minecraft.text.Text.literal(
                                    "§aНастройки локации сохранены"), true);
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(net.minecraft.text.Text.literal(
                                    "§cНекорректная маска локации"), true);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(LOCATION_TELEPORT_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    int target = buf.readVarInt();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) {
                            player.sendMessage(net.minecraft.text.Text.literal(
                                    "§cДля телепортации через камеру нужны права оператора"), true);
                            return;
                        }
                        GameConfig.CameraPoint point = GameConfig.getCameraPoint(target);
                        if (point == null) {
                            player.sendMessage(net.minecraft.text.Text.literal(
                                    "§cТочка телепортации не найдена"), true);
                            return;
                        }
                        net.minecraft.server.world.ServerWorld world =
                                server.getWorld(point.worldKey());
                        if (world == null) {
                            player.sendMessage(net.minecraft.text.Text.literal(
                                    "§cМир точки телепортации недоступен"), true);
                            return;
                        }
                        player.teleport(world, point.pos().x, point.pos().y, point.pos().z,
                                point.yaw(), point.pitch());
                        player.sendMessage(net.minecraft.text.Text.literal(
                                "§aТелепортация: §f" + point.name()), true);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(START_BLOCK_RENAME_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    net.minecraft.util.math.BlockPos pos = buf.readBlockPos();
                    String title = buf.readString(32);
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)
                                || player.squaredDistanceTo(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D) > 64.0D) {
                            return;
                        }
                        if (player.getWorld().getBlockEntity(pos)
                                instanceof fable.hideseek.imba.block.entity.StartBlockEntity start) {
                            start.setTitle(title);
                            player.sendMessage(net.minecraft.text.Text.literal("§aНазвание блока запуска изменено"), true);
                        }
                    });
                });
    }

    private static boolean isFiniteCoordinate(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) <= 3.0E7D && Math.abs(y) <= 2048.0D && Math.abs(z) <= 3.0E7D;
    }

    public static void refresh(ServerPlayerEntity owner) {
        if (owner == null)
            return;
        MaskState state = MaskState.get(owner.getUuid());
        sendMaskUpdate(owner, state.type, state.block, state.item);
        sendStatueSync(owner, state.statue);
    }

    public static void sendMaskUpdate(ServerPlayerEntity owner, MaskType type, Block block, Item item) {
        MinecraftServer server = owner.getServer();
        if (server == null)
            return;

        for (ServerPlayerEntity target : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(target, MASK_UPDATE, createMaskUpdateBuf(owner, type, block, item));
        }
    }

    public static void sendMaskReset(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null)
            return;

        for (ServerPlayerEntity target : PlayerLookup.all(server)) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeUuid(owner.getUuid());
            ServerPlayNetworking.send(target, MASK_RESET, buf);
        }
    }

    public static void sendStatueSync(ServerPlayerEntity owner, boolean statue) {
        MinecraftServer server = owner.getServer();
        if (server == null)
            return;

        for (ServerPlayerEntity target : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(target, STATUE_SYNC, createStatueSyncBuf(owner, statue));
        }
    }

    private static PacketByteBuf createStatueSyncBuf(ServerPlayerEntity owner, boolean statue) {
        MaskState state = MaskState.get(owner.getUuid());
        double x = statue ? state.anchorX : owner.getX();
        double y = statue ? state.anchorY : owner.getY();
        double z = statue ? state.anchorZ : owner.getZ();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(owner.getUuid());
        buf.writeBoolean(statue);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        return buf;
    }

    public static void syncAllTo(ServerPlayerEntity joiner) {
        MinecraftServer server = joiner.getServer();
        if (server == null)
            return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!MaskState.hasMask(player.getUuid()))
                continue;

            MaskState state = MaskState.get(player.getUuid());
            ServerPlayNetworking.send(joiner, MASK_UPDATE,
                    createMaskUpdateBuf(player, state.type, state.block, state.item));

            ServerPlayNetworking.send(joiner, STATUE_SYNC,
                    createStatueSyncBuf(player, state.statue));
        }
        sendPanelData(joiner);
        sendGameState(joiner);
        sendAllPhotos(joiner);
    }

    public static void resyncAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, RESYNC_CLEAR,
                    new PacketByteBuf(Unpooled.buffer()));
            syncAllTo(player);
        }
    }

    public static void broadcastPanelData(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity player : PlayerLookup.all(server)) sendPanelData(player);
    }

    private static void sendPanelData(ServerPlayerEntity player) {
        PacketByteBuf settings = new PacketByteBuf(Unpooled.buffer());
        settings.writeVarInt(GameConfig.ROUND_SECONDS);
        settings.writeVarInt(GameConfig.SEEKER_HEARTS);
        settings.writeVarInt(GameConfig.SELECTED_LOCATION);
        settings.writeVarInt(GameConfig.ROUNDS.size());
        settings.writeString(GameConfig.getLocationPanelLabel(GameConfig.SELECTED_LOCATION), 128);
        for (fable.hideseek.imba.game.RoundDefinition round : GameConfig.ROUNDS) {
            settings.writeString(round.locationName == null ? "" : round.locationName, 48);
            settings.writeString(round.sourceKind.name(), 16);
            settings.writeString(round.maskId.toString(), 128);
        }
        net.minecraft.util.math.Vec3d potionOffset =
                fable.hideseek.imba.config.AttachmentConfig.offsetFor(new Identifier("imba", "potion_2d"));
        settings.writeDouble(potionOffset.x);
        settings.writeDouble(potionOffset.y);
        settings.writeDouble(potionOffset.z);
        ServerPlayNetworking.send(player, GAME_SETTINGS_PACKET, settings);
    }

    public static void sendGameState(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        PacketByteBuf state = new PacketByteBuf(Unpooled.buffer());
        state.writeBoolean(GameManager.isPaused());
        state.writeBoolean(GameManager.isPrepareLocked(player));
        ServerPlayNetworking.send(player, GAME_STATE_PACKET, state);
    }

    public static void broadcastGameState(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            sendGameState(player);
        }
    }

    private static void sendAllPhotos(ServerPlayerEntity player) {
        for (int location = 0;
                location < fable.hideseek.imba.config.LocationPhotoStorage.LOCATION_COUNT;
                location++) {
            byte[] png = fable.hideseek.imba.config.LocationPhotoStorage.read(location);
            sendPhoto(player, location, png == null ? new byte[0] : png);
        }
    }

    private static void broadcastPhoto(MinecraftServer server, int location, byte[] png) {
        if (server == null) return;
        for (ServerPlayerEntity player : PlayerLookup.all(server)) sendPhoto(player, location, png);
    }

    private static void sendPhoto(ServerPlayerEntity player, int location, byte[] png) {
        PacketByteBuf photo = new PacketByteBuf(Unpooled.buffer());
        photo.writeVarInt(location);
        photo.writeByteArray(png);
        ServerPlayNetworking.send(player, LOCATION_PHOTO_SYNC_PACKET, photo);
    }

    private static PacketByteBuf createMaskUpdateBuf(ServerPlayerEntity owner, MaskType type, Block block, Item item) {
        MaskState state = MaskState.get(owner.getUuid());

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(owner.getUuid());
        buf.writeString(type.name());
        buf.writeString(block == null ? "" : Registries.BLOCK.getId(block).toString());
        buf.writeString(item == null ? "" : Registries.ITEM.getId(item).toString());
        buf.writeFloat(state.rotation);
        buf.writeFloat(state.rotationX);
        buf.writeFloat(state.rotationZ);
        buf.writeBoolean(state.doorOpen);
        buf.writeBoolean(state.buttonPressed);
        buf.writeBoolean(state.wallAttached);
        buf.writeBoolean(state.attachedToFrame);
        buf.writeString((state.attachmentFacing == null ? Direction.NORTH : state.attachmentFacing).getName());
        buf.writeInt(state.frameRotationStep);
        return buf;
    }
}
