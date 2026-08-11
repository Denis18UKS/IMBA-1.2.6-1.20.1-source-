package fable.hideseek.imba.net;

import fable.hideseek.imba.client.ClientMaskData;
import fable.hideseek.imba.client.PanelData;
import fable.hideseek.imba.mask.MaskResetGeometry;
import fable.hideseek.imba.mask.MaskType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MaskClientNetworking {

    private static final int RESET_RECOVERY_PASSES = 2;
    private static final Map<UUID, Integer> RESET_RECOVERY = new HashMap<>();

    private MaskClientNetworking() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(MaskNetworking.MASK_UPDATE,
                (client, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    MaskType type = MaskType.valueOf(buf.readString());
                    String blockId = buf.readString();
                    String itemId = buf.readString();
                    float rotY = buf.readFloat();
                    float rotX = buf.readFloat();
                    float rotZ = buf.readFloat();
                    boolean doorOpen = buf.readBoolean();
                    boolean buttonPressed = buf.readBoolean();
                    boolean wallAttached = buf.readBoolean();
                    boolean attachedToFrame = buf.readBoolean();
                    Direction attachmentFacing = Direction.byName(buf.readString());
                    int frameRotationStep = buf.readInt();

                    client.execute(() -> {
                        RESET_RECOVERY.remove(uuid);

                        Block block = blockId.isEmpty() ? null : Registries.BLOCK.get(new Identifier(blockId));
                        Item item = itemId.isEmpty() ? null : Registries.ITEM.get(new Identifier(itemId));

                        ClientMaskData.setMask(
                                uuid,
                                type,
                                block,
                                item,
                                rotY,
                                rotX,
                                rotZ,
                                doorOpen,
                                buttonPressed,
                                wallAttached,
                                attachedToFrame,
                                attachmentFacing == null ? Direction.NORTH : attachmentFacing,
                                frameRotationStep);
                        if (client.world != null) {
                            var maskedPlayer = client.world.getPlayerByUuid(uuid);
                            if (maskedPlayer != null) {
                                maskedPlayer.calculateDimensions();
                            }
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(MaskNetworking.MASK_RESET,
                (client, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    client.execute(() -> {
                        ClientMaskData.reset(uuid);
                        if (client.world != null) {
                            var maskedPlayer = client.world.getPlayerByUuid(uuid);
                            fable.hideseek.imba.client.ClientStatueLock.release(maskedPlayer);
                            restoreStandingAfterMaskReset(maskedPlayer);
                            RESET_RECOVERY.put(uuid, RESET_RECOVERY_PASSES);
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(MaskNetworking.RESYNC_CLEAR,
                (client, handler, buf, responseSender) ->
                        client.execute(() -> {
                            RESET_RECOVERY.clear();
                            ClientMaskData.clearAll();
                        }));

        ClientPlayNetworking.registerGlobalReceiver(MaskNetworking.STATUE_SYNC,
                (client, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    boolean statue = buf.readBoolean();
                    double anchorX = buf.readDouble();
                    double anchorY = buf.readDouble();
                    double anchorZ = buf.readDouble();
                    client.execute(() -> {
                        ClientMaskData.setStatue(uuid, statue, anchorX, anchorY, anchorZ);
                        if (client.world != null) {
                            var player = client.world.getPlayerByUuid(uuid);
                            if (statue) {
                                fable.hideseek.imba.client.ClientStatueLock.apply(player);
                            } else {
                                fable.hideseek.imba.client.ClientStatueLock.release(player);
                                if (!ClientMaskData.hasMask(uuid)) {
                                    restoreStandingAfterMaskReset(player);
                                } else if (player != null) {
                                    player.calculateDimensions();
                                }
                            }
                            if (statue && player != null) {
                                player.calculateDimensions();
                            }
                        }
                    });
                });
        ClientPlayNetworking.registerGlobalReceiver(MaskNetworking.GAME_SETTINGS_PACKET,
                (client, handler, buf, responseSender) -> {
                    int seconds = buf.readVarInt();
                    int hearts = buf.readVarInt();
                    int location = buf.readVarInt();
                    int locationCount = buf.readVarInt();
                    String locationName = buf.readString(128);
                    String[] locationNames = new String[locationCount];
                    String[] locationMaskKinds = new String[locationCount];
                    String[] locationMaskIds = new String[locationCount];
                    for (int i = 0; i < locationCount; i++) {
                        locationNames[i] = buf.readString(48);
                        locationMaskKinds[i] = buf.readString(16);
                        locationMaskIds[i] = buf.readString(128);
                    }
                    double offsetX = buf.readDouble(), offsetY = buf.readDouble(), offsetZ = buf.readDouble();
                    client.execute(() -> {
                        PanelData.seconds = seconds;
                        PanelData.hearts = hearts;
                        PanelData.selectedLocation = location;
                        PanelData.locationCount = locationCount;
                        PanelData.locationName = locationName;
                        PanelData.locationNames = locationNames;
                        PanelData.locationMaskKinds = locationMaskKinds;
                        PanelData.locationMaskIds = locationMaskIds;
                        PanelData.potionOffsetX = offsetX;
                        PanelData.potionOffsetY = offsetY;
                        PanelData.potionOffsetZ = offsetZ;
                    });
                });
        ClientPlayNetworking.registerGlobalReceiver(MaskNetworking.GAME_STATE_PACKET,
                (client, handler, buf, responseSender) -> {
                    boolean paused = buf.readBoolean();
                    boolean prepareLocked = buf.readBoolean();
                    client.execute(() -> {
                        fable.hideseek.imba.client.ClientGameState.paused = paused;
                        fable.hideseek.imba.client.ClientGameState.prepareLocked = prepareLocked;
                    });
                });
        ClientPlayNetworking.registerGlobalReceiver(MaskNetworking.LOCATION_PHOTO_SYNC_PACKET,
                (client, handler, buf, responseSender) -> {
                    int location = buf.readVarInt();
                    byte[] png = buf.readByteArray(
                            fable.hideseek.imba.config.LocationPhotoStorage.MAX_PHOTO_BYTES);
                    client.execute(() -> {
                        if (png.length == 0) {
                            fable.hideseek.imba.client.ClientLocationPhotos.remove(location);
                        } else {
                            fable.hideseek.imba.client.ClientLocationPhotos.apply(location, png);
                        }
                    });
                });
    }

    /** Runs from END_CLIENT_TICK so reset geometry survives packet/tick ordering. */
    public static void tickResetRecovery(MinecraftClient client) {
        if (client == null || client.world == null || RESET_RECOVERY.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = RESET_RECOVERY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            UUID uuid = entry.getKey();

            if (ClientMaskData.hasMask(uuid)) {
                iterator.remove();
                continue;
            }

            PlayerEntity player = client.world.getPlayerByUuid(uuid);
            if (player != null) {
                restoreStandingAfterMaskReset(player);
            }

            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private static void restoreStandingAfterMaskReset(PlayerEntity player) {
        MaskResetGeometry.forceStanding(player);
    }
}
