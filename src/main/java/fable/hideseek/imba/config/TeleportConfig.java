package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.RoundDefinition;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Persistent coordinates edited by the in-game teleport setup tools. */
public final class TeleportConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_teleports.json");

    private TeleportConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data == null) return;
            if (data.lobby != null) GameConfig.LOBBY_POS = data.lobby.toVec3d();
            if (data.rounds == null) return;
            for (int i = 0; i < Math.min(data.rounds.size(), GameConfig.ROUNDS.size()); i++) {
                RoundPoints points = data.rounds.get(i);
                RoundDefinition round = GameConfig.ROUNDS.get(i);
                if (points.hider != null && !isLegacyPlaceholder(points.hider, i, false)) {
                    round.hiderPos = points.hider.toVec3d();
                }
                if (points.seeker != null && !isLegacyPlaceholder(points.seeker, i, true)) {
                    round.seekerPos = points.seeker.toVec3d();
                }
            }
            // Rewrites legacy test coordinates to the real location defaults.
            save();
        } catch (IOException | RuntimeException e) {
            System.err.println("[IMBA] Не удалось загрузить координаты телепортации: " + e.getMessage());
        }
    }

    public static void save() {
        List<RoundPoints> rounds = new ArrayList<>();
        for (RoundDefinition round : GameConfig.ROUNDS) {
            rounds.add(new RoundPoints(Point.from(round.hiderPos), Point.from(round.seekerPos)));
        }
        Data data = new Data(Point.from(GameConfig.LOBBY_POS), rounds);
        try {
            Files.createDirectories(PATH.getParent());
            Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(data));
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[IMBA] Не удалось сохранить координаты телепортации: " + e.getMessage());
        }
    }

    private record Data(Point lobby, List<RoundPoints> rounds) {}
    private record RoundPoints(Point hider, Point seeker) {}

    private static boolean isLegacyPlaceholder(Point point, int roundIndex, boolean seeker) {
        double base = 100.5D + roundIndex * 20.0D;
        double expectedX = seeker ? base + 8.0D : base;
        return Math.abs(point.x - expectedX) < 0.001D
                && Math.abs(point.z - base) < 0.001D;
    }

    private record Point(double x, double y, double z) {
        private static Point from(Vec3d value) { return new Point(value.x, value.y, value.z); }
        private Vec3d toVec3d() { return new Vec3d(x, y, z); }
    }
}
