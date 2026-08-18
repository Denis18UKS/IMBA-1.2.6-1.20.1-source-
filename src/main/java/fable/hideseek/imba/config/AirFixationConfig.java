package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fable.hideseek.imba.mask.MaskState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rules controlling whether a disguise may be fixed without solid support below it. */
public final class AirFixationConfig {
    public enum Mode { DENY, ALLOW, REQUIRE_BLOCK }
    public record Rule(Mode mode, String requiredBlock) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_air_fixation.json");
    private static final Map<String, Rule> OVERRIDES = new LinkedHashMap<>();

    private AirFixationConfig() {}

    public static void load() {
        OVERRIDES.clear();
        if (!Files.exists(PATH)) { save(); return; }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(PATH));
            if (!root.isJsonObject()) return;
            for (var entry : root.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                    OVERRIDES.put(key, new Rule(value.getAsBoolean() ? Mode.ALLOW : Mode.DENY, ""));
                    continue;
                }
                if (!value.isJsonObject()) continue;
                JsonObject object = value.getAsJsonObject();
                Mode mode;
                try { mode = Mode.valueOf(object.has("mode") ? object.get("mode").getAsString() : "DENY"); }
                catch (RuntimeException ignored) { mode = Mode.DENY; }
                String required = object.has("requiredBlock") ? object.get("requiredBlock").getAsString() : "";
                if (mode == Mode.REQUIRE_BLOCK && !validBlock(required)) mode = Mode.DENY;
                OVERRIDES.put(key, new Rule(mode, mode == Mode.REQUIRE_BLOCK ? required : ""));
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить правила фиксации в воздухе: " + e.getMessage());
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (var entry : OVERRIDES.entrySet()) {
            JsonObject object = new JsonObject();
            object.addProperty("mode", entry.getValue().mode().name());
            object.addProperty("requiredBlock", entry.getValue().requiredBlock());
            root.add(entry.getKey(), object);
        }
        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить правила фиксации в воздухе: " + e.getMessage());
        }
    }

    public static Rule effectiveRule(MaskState state) {
        String key = keyFor(state);
        return key == null ? new Rule(Mode.DENY, "") : effectiveRule(key);
    }

    public static Rule effectiveRule(String key) {
        Rule override = OVERRIDES.get(key);
        return override != null ? override : defaultRule(key);
    }

    public static Rule defaultRule(String key) {
        return new Rule(key != null && key.startsWith("item:") ? Mode.ALLOW : Mode.DENY, "");
    }

    public static boolean isAllowedAt(MaskState state, World world, double x, double y, double z, boolean inAir) {
        if (!inAir) return true;
        Rule rule = effectiveRule(state);
        if (rule.mode() == Mode.ALLOW) return true;
        if (rule.mode() == Mode.DENY || world == null || !validBlock(rule.requiredBlock())) return false;
        Identifier required = Identifier.tryParse(rule.requiredBlock());
        Block actual = world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock();
        return required != null && Registries.BLOCK.getId(actual).equals(required);
    }

    public static void set(String key, Mode mode, String requiredBlock) {
        if (key == null || key.isBlank() || mode == null) return;
        String required = requiredBlock == null ? "" : requiredBlock.trim();
        if (mode == Mode.REQUIRE_BLOCK && !validBlock(required)) return;
        Rule value = new Rule(mode, mode == Mode.REQUIRE_BLOCK ? required : "");
        if (value.equals(defaultRule(key))) OVERRIDES.remove(key); else OVERRIDES.put(key, value);
        save();
    }

    public static Map<String, Rule> overridesSnapshot() { return Map.copyOf(OVERRIDES); }
    public static String blockKey(Identifier id) { return id == null ? null : "block:" + id; }
    public static String itemKey(Identifier id) { return id == null ? null : "item:" + id; }
    public static String keyFor(MaskState state) {
        if (state == null) return null;
        if (state.block != null) return blockKey(Registries.BLOCK.getId(state.block));
        if (state.item != null) return itemKey(Registries.ITEM.getId(state.item));
        return "type:" + state.type.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean validBlock(String raw) {
        Identifier id = Identifier.tryParse(raw);
        return id != null && Registries.BLOCK.containsId(id);
    }
}
