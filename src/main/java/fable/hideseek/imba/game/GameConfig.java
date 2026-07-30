package fable.hideseek.imba.game;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;

public final class GameConfig {

    public static boolean SPAWN_TO_LOBBY_IF_GAME_INACTIVE = true;

    public static int SEEKER_HEARTS = 5;
    public static int PREPARE_SECONDS = 40;
    public static int ROUND_SECONDS = 120;
    public static int RETURN_TO_LOBBY_SECONDS = 5;
    public static int SELECTED_LOCATION = 0;
    /** Optional action-bar diagnostics; disabled for normal gameplay. */
    public static boolean SHOW_GAMEPLAY_MESSAGES = false;

    public static Vec3d LOBBY_POS = new Vec3d(0.5, 80.0, 0.5);
    public static float LOBBY_YAW = 0.0f;
    public static float LOBBY_PITCH = 0.0f;
    public static final Vec3d CAMERA_LOBBY_POS = new Vec3d(-131.86, -54.00, 149.71);
    public static final float CAMERA_LOBBY_YAW = 354.30f;
    public static final float CAMERA_LOBBY_PITCH = -3.45f;
    public static final Vec3d SECRET_CINEMA_POS = new Vec3d(-131.38, -53.00, 173.43);
    public static final float SECRET_CINEMA_YAW = -3.45f;
    public static final float SECRET_CINEMA_PITCH = 20.70f;

    public static final List<RoundDefinition> ROUNDS = new ArrayList<>();

    static {
        // Order and default masks follow the supplied "Прятки-минутки" design.
        addLocation("nether_portal", "Портал в ад", 100, 65, RoundDefinition.SourceKind.BLOCK,
                "minecraft:nether_portal", "портал", false);
        addLocation("house", "Домик", 120, 65, RoundDefinition.SourceKind.BLOCK,
                "minecraft:oak_door", "дверь", false);
        addLocation("kitchen", "Кухня", 140, 66, RoundDefinition.SourceKind.ITEM,
                "minecraft:apple", "яблоко", false);
        addLocation("garden_house", "Домик с огородом", 160, 64, RoundDefinition.SourceKind.BLOCK,
                "minecraft:attached_pumpkin_stem", "стебель тыквы", false);
        addLocation("snow_village", "Улица снежной деревни", 180, 64, RoundDefinition.SourceKind.BLOCK,
                "imba:hanging_lantern", "подвесной фонарь", true);
        addLocation("pond", "Пруд", 200, 64, RoundDefinition.SourceKind.BLOCK,
                "imba:grass", "низкая трава", false);
        addLocation("starter_house", "Дом новичка", 220, 64, RoundDefinition.SourceKind.BLOCK,
                "minecraft:ladder", "перевёрнутая лестница", false);
        addLocation("beach", "Пляж", 240, 64, RoundDefinition.SourceKind.BLOCK,
                "minecraft:stone_button", "каменная кнопка", false);
        addLocation("ancient_city", "Древний город", 260, 64, RoundDefinition.SourceKind.BLOCK,
                "minecraft:sculk_vein", "скалк-жила", false);
        addLocation("mine", "Шахта", 280, 64, RoundDefinition.SourceKind.BLOCK,
                "imba:stonrcutter_lezvie", "статичное лезвие", false);
        addLocation("green_cave", "Зелёная пещера", 300, 64, RoundDefinition.SourceKind.BLOCK,
                "imba:glowberries", "ягоды без листвы", false);
        addLocation("witch_house", "Домик ведьмы", 320, 64, RoundDefinition.SourceKind.ITEM,
                "imba:potion_2d", "2D-зелье", false);

        setDefaultLocationPoint(0, 166.73, -60.00, 151.56, -3.10f, 21.60f);
        setDefaultLocationPoint(1, 20.02, -60.06, -95.50, 992.05f, 57.60f);
        setDefaultLocationPoint(2, 176.14, -58.94, 72.31, 698.36f, 26.75f);
        setDefaultLocationPoint(3, 644.72, -57.50, 65.89, -771.91f, 17.18f);
        setDefaultLocationPoint(4, 83.18, -60.06, -322.84, -200.95f, 16.20f);
        setDefaultLocationPoint(5, 79.85, -57.50, -538.72, -3825.50f, 53.10f);
        setDefaultLocationPoint(6, 369.87, -54.50, 147.55, 184.66f, 61.76f);
        setDefaultLocationPoint(7, 73.07, -57.00, -945.34, -1572.85f, 16.05f);
        setDefaultLocationPoint(8, 285.06, -55.00, -44.52, -185.18f, 41.05f);
        setDefaultLocationPoint(9, 86.02, -60.00, -1282.91, -1185.23f, 15.00f);
        setDefaultLocationPoint(10, 40.75, -58.00, -1524.75, -821.70f, 26.40f);
        setDefaultLocationPoint(11, 55.38, -60.00, -1726.30, -164.08f, 27.90f);
    }

    private GameConfig() {
    }

    private static void addLocation(String id, String locationName, int coordinate, int y,
            RoundDefinition.SourceKind sourceKind, String maskId, String displayWord, boolean evening) {
        Identifier identifier = new Identifier(maskId);
        ROUNDS.add(new RoundDefinition(
                id,
                World.OVERWORLD,
                new Vec3d(coordinate + 0.5, y, coordinate + 0.5), 180.0f, 0.0f,
                new Vec3d(coordinate + 8.5, y, coordinate + 0.5), 180.0f, 0.0f,
                sourceKind,
                identifier,
                identifier,
                displayWord,
                locationName,
                evening));
    }

    private static void setDefaultLocationPoint(int index, double x, double y, double z, float yaw, float pitch) {
        RoundDefinition round = ROUNDS.get(index);
        Vec3d point = new Vec3d(x, y, z);
        round.setCameraPoint(point, yaw, pitch);
        /*
         * Camera, hider and seeker now use the same per-location source.
         * TeleportConfig may still override hider/seeker points when an
         * administrator deliberately saved custom values in game.
         */
        round.hiderPos = point;
        round.hiderYaw = yaw;
        round.hiderPitch = pitch;
        round.seekerPos = point;
        round.seekerYaw = yaw;
        round.seekerPitch = pitch;
    }

    public static CameraPoint getCameraPoint(int target) {
        if (target >= 0 && target < ROUNDS.size()) {
            RoundDefinition round = ROUNDS.get(target);
            return new CameraPoint(round.worldKey, round.cameraPos, round.cameraYaw, round.cameraPitch,
                    getLocationName(target));
        }
        if (target == ROUNDS.size()) {
            return new CameraPoint(World.OVERWORLD, CAMERA_LOBBY_POS,
                    CAMERA_LOBBY_YAW, CAMERA_LOBBY_PITCH, "Лобби");
        }
        if (target == ROUNDS.size() + 1) {
            return new CameraPoint(World.OVERWORLD, SECRET_CINEMA_POS,
                    SECRET_CINEMA_YAW, SECRET_CINEMA_PITCH, "Скрытый кинотеатр");
        }
        return null;
    }

    public static String getPointDisplayName(String slot) {
        if (slot == null) {
            return "неизвестная точка";
        }
        if ("lobby".equalsIgnoreCase(slot)) {
            return "лобби";
        }
        String lower = slot.toLowerCase(java.util.Locale.ROOT);
        if (lower.matches("r\\d+_hider")) {
            int split = lower.indexOf('_');
            return "локация " + lower.substring(1, split) + " — прячущийся";
        }
        if (lower.matches("r\\d+_seeker")) {
            int split = lower.indexOf('_');
            return "локация " + lower.substring(1, split) + " — искатель";
        }
        return slot;
    }

    public record CameraPoint(
            net.minecraft.registry.RegistryKey<World> worldKey,
            Vec3d pos,
            float yaw,
            float pitch,
            String name) {}

    public static void setSeekerHearts(int hearts) {
        SEEKER_HEARTS = Math.max(1, hearts);
    }

    public static void setShowGameplayMessages(boolean show) {
        SHOW_GAMEPLAY_MESSAGES = show;
    }

    public static void setPrepareSeconds(int seconds) {
        PREPARE_SECONDS = Math.max(1, seconds);
    }

    public static void setRoundSeconds(int seconds) {
        ROUND_SECONDS = Math.max(1, seconds);
    }

    public static void setReturnToLobbySeconds(int seconds) {
        RETURN_TO_LOBBY_SECONDS = Math.max(1, seconds);
    }

    public static void setSelectedLocation(int index) {
        if (ROUNDS.isEmpty()) {
            SELECTED_LOCATION = 0;
            return;
        }
        SELECTED_LOCATION = Math.floorMod(index, ROUNDS.size());
    }

    public static RoundDefinition getSelectedLocation() {
        if (ROUNDS.isEmpty()) {
            return null;
        }
        setSelectedLocation(SELECTED_LOCATION);
        return ROUNDS.get(SELECTED_LOCATION);
    }

    public static String getLocationName(int index) {
        if (index < 0 || index >= ROUNDS.size()) {
            return "Локация";
        }
        String configured = ROUNDS.get(index).locationName == null ? "" : ROUNDS.get(index).locationName.strip();
        return configured.isEmpty() ? "Локация " + (index + 1) : configured;
    }

    public static String getLocationPanelLabel(int index) {
        if (index < 0 || index >= ROUNDS.size()) {
            return "Локация";
        }
        String configured = ROUNDS.get(index).locationName == null ? "" : ROUNDS.get(index).locationName.strip();
        return configured.isEmpty() ? (index + 1) + "/" + ROUNDS.size() : configured;
    }

    public static boolean setLocationSettings(int index, String name,
            RoundDefinition.SourceKind sourceKind, Identifier maskId) {
        if (index < 0 || index >= ROUNDS.size() || sourceKind == null || maskId == null) {
            return false;
        }
        if (sourceKind == RoundDefinition.SourceKind.BLOCK) {
            if (!Registries.BLOCK.containsId(maskId) || Registries.BLOCK.get(maskId) == Blocks.AIR) {
                return false;
            }
        } else if (!Registries.ITEM.containsId(maskId) || Registries.ITEM.get(maskId) == Items.AIR) {
            return false;
        }

        RoundDefinition round = ROUNDS.get(index);
        round.locationName = sanitizeLocationName(name);
        round.setMask(sourceKind, maskId, maskDisplayName(sourceKind, maskId));
        return true;
    }

    private static String sanitizeLocationName(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = name.strip();
        return cleaned.substring(0, Math.min(48, cleaned.length()));
    }

    public static String maskDisplayName(RoundDefinition.SourceKind sourceKind, Identifier maskId) {
        if (sourceKind == RoundDefinition.SourceKind.BLOCK && Registries.BLOCK.containsId(maskId)) {
            return Registries.BLOCK.get(maskId).getName().getString();
        }
        if (sourceKind == RoundDefinition.SourceKind.ITEM && Registries.ITEM.containsId(maskId)) {
            return Registries.ITEM.get(maskId).getName().getString();
        }
        return maskId.getPath().replace('_', ' ');
    }

    public static void setPoint(String slot, Vec3d pos) {
        if ("lobby".equalsIgnoreCase(slot)) {
            LOBBY_POS = pos;
            fable.hideseek.imba.config.TeleportConfig.save();
            return;
        }

        String lower = slot.toLowerCase();
        if (!lower.startsWith("r"))
            return;

        int underscore = lower.indexOf('_');
        if (underscore < 0)
            return;

        String roundPart = lower.substring(1, underscore);
        String rolePart = lower.substring(underscore + 1);

        int roundIndex;
        try {
            roundIndex = Integer.parseInt(roundPart) - 1;
        } catch (NumberFormatException e) {
            return;
        }

        if (roundIndex < 0 || roundIndex >= ROUNDS.size())
            return;

        RoundDefinition round = ROUNDS.get(roundIndex);

        if ("hider".equals(rolePart)) {
            round.hiderPos = pos;
        } else if ("seeker".equals(rolePart)) {
            round.seekerPos = pos;
        } else {
            return;
        }
        fable.hideseek.imba.config.TeleportConfig.save();
    }
}
