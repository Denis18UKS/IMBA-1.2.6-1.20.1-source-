package ru.fifth.horror.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import ru.fifth.horror.FifthMod;
import ru.fifth.horror.block.ScriptComputerBlockEntity;
import ru.fifth.horror.cutscene.CutsceneDefinition;
import ru.fifth.horror.cutscene.CutsceneManager;
import ru.fifth.horror.entity.DirectorNpcEntity;
import ru.fifth.horror.script.FifthScriptEngine;
import ru.fifth.horror.structure.StructureLayerManager;

public final class FifthNetworking {
    private static final Gson GSON = new Gson();
    public static final Identifier OPEN_COMPUTER = FifthMod.id("open_computer");
    public static final Identifier SAVE_COMPUTER = FifthMod.id("save_computer");
    public static final Identifier RUN_COMPUTER = FifthMod.id("run_computer");
    public static final Identifier CREATE_NPC_EGG = FifthMod.id("create_npc_egg");
    public static final Identifier SAVE_NPC = FifthMod.id("save_npc");
    public static final Identifier STRUCTURE_CAPTURE = FifthMod.id("structure_capture");
    public static final Identifier STRUCTURE_ACTIVATE = FifthMod.id("structure_activate");
    public static final Identifier SAVE_CUTSCENE = FifthMod.id("save_cutscene");
    public static final Identifier PLAY_CUTSCENE = FifthMod.id("play_cutscene");
    public static final Identifier CUTSCENE_PAYLOAD = FifthMod.id("cutscene_payload");
    public static final Identifier CUTSCENE_END_TELEPORT = FifthMod.id("cutscene_end_teleport");

    private FifthNetworking() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CREATE_NPC_EGG, (server, player, handler, buf, responseSender) -> {
            String json = buf.readString(32767);
            server.execute(() -> giveNpcEgg(player, json));
        });
        ServerPlayNetworking.registerGlobalReceiver(SAVE_NPC, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readVarInt(); String json = buf.readString(32767);
            server.execute(() -> {
                if (player.getWorld().getEntityById(entityId) instanceof DirectorNpcEntity npc && player.hasPermissionLevel(2)) npc.applyTemplateJson(json);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(SAVE_COMPUTER, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos(); String name = buf.readString(128); String script = buf.readString(1_000_000);
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                if (player.getWorld().getBlockEntity(pos) instanceof ScriptComputerBlockEntity be) {
                    be.setScriptName(name); be.setScript(script); be.markDirty();
                    FifthScriptEngine.saveScript(server, name, script);
                    player.sendMessage(Text.literal("§7Сценарий сохранён: §f" + name), false);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(RUN_COMPUTER, (server, player, handler, buf, responseSender) -> {
            String name = buf.readString(128);
            server.execute(() -> { if (player.hasPermissionLevel(2)) FifthScriptEngine.runNamed(server, name, player); });
        });
        ServerPlayNetworking.registerGlobalReceiver(STRUCTURE_CAPTURE, (server, player, handler, buf, responseSender) -> {
            String build = buf.readString(128); String variant = buf.readString(128); String group = buf.readString(128);
            boolean defaultActive = buf.readBoolean(); boolean restoreOnLoad = buf.readBoolean();
            BlockPos a = buf.readBlockPos(); BlockPos b = buf.readBlockPos();
            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                StructureLayerManager.capture(server, player.getServerWorld(), build, variant, group, defaultActive, restoreOnLoad, a, b);
                player.sendMessage(Text.literal("§7Слой сохранён: §f" + build + "/" + variant), false);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(STRUCTURE_ACTIVATE, (server, player, handler, buf, responseSender) -> {
            String build = buf.readString(128); String variant = buf.readString(128);
            server.execute(() -> { if (player.hasPermissionLevel(2)) StructureLayerManager.activate(server, player.getServerWorld(), build, variant); });
        });
        ServerPlayNetworking.registerGlobalReceiver(SAVE_CUTSCENE, (server, player, handler, buf, responseSender) -> {
            String json = buf.readString(1_000_000);
            server.execute(() -> { if (player.hasPermissionLevel(2)) CutsceneManager.save(server, GSON.fromJson(json, CutsceneDefinition.class)); });
        });
        ServerPlayNetworking.registerGlobalReceiver(PLAY_CUTSCENE, (server, player, handler, buf, responseSender) -> {
            String id = buf.readString(128);
            server.execute(() -> { if (player.hasPermissionLevel(2)) CutsceneManager.play(server, id); });
        });
        ServerPlayNetworking.registerGlobalReceiver(CUTSCENE_END_TELEPORT, (server, player, handler, buf, responseSender) -> {
            String id = buf.readString(128);
            server.execute(() -> {
                CutsceneDefinition scene = CutsceneManager.load(server, id);
                if (scene == null || !scene.teleportPlayerAtEnd || scene.keyframes.isEmpty()) return;
                CutsceneDefinition.Keyframe end = scene.keyframes.get(scene.keyframes.size() - 1);
                double eyeOffset = player.getEyeY() - player.getY();
                player.teleport(player.getServerWorld(), end.x, end.y - eyeOffset, end.z, end.yaw, end.pitch);
            });
        });
    }

    private static void giveNpcEgg(ServerPlayerEntity player, String json) {
        if (!player.hasPermissionLevel(2)) return;
        JsonObject object = GSON.fromJson(json, JsonObject.class);
        ItemStack stack = new ItemStack(FifthMod.NPC_SPAWN_EGG);
        stack.getOrCreateNbt().putString("FifthNpcTemplate", json);
        if (object != null && object.has("name")) stack.setCustomName(Text.literal("NPC: " + object.get("name").getAsString()));
        player.giveItemStack(stack);
    }

    public static void openComputer(ServerPlayerEntity player, ScriptComputerBlockEntity be) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeBlockPos(be.getPos()); out.writeString(be.getScriptName()); out.writeString(be.getScript(), 1_000_000);
        ServerPlayNetworking.send(player, OPEN_COMPUTER, out);
    }
}
