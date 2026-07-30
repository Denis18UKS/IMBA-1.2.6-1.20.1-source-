package fable.hideseek.imba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BreakRulesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("imba_break_rules.json");
    private static final Set<Identifier> NO_PENALTY_BREAK_BLOCKS = new HashSet<>();

    private BreakRulesConfig() {}

    public static void load() {
        NO_PENALTY_BREAK_BLOCKS.clear();
        if (!Files.exists(PATH)) { NO_PENALTY_BREAK_BLOCKS.add(Registries.BLOCK.getId(Blocks.BREWING_STAND)); save(); return; }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data != null && data.noPenaltyBreakBlocks != null) {
                for (String id : data.noPenaltyBreakBlocks) NO_PENALTY_BREAK_BLOCKS.add(new Identifier(id));
            }
        } catch (IOException | RuntimeException e) { NO_PENALTY_BREAK_BLOCKS.add(Registries.BLOCK.getId(Blocks.BREWING_STAND)); }
        // This is a mandatory gameplay rule for the 2D-potion location even
        // when an older config file did not yet contain the entry.
        NO_PENALTY_BREAK_BLOCKS.add(Registries.BLOCK.getId(Blocks.BREWING_STAND));
    }

    public static void save() {
        try { Files.createDirectories(PATH.getParent()); Files.writeString(PATH, GSON.toJson(new Data(NO_PENALTY_BREAK_BLOCKS.stream().map(Identifier::toString).toList()))); }
        catch (IOException ignored) {}
    }

    public static boolean shouldPunishForBreak(Block block) { return !NO_PENALTY_BREAK_BLOCKS.contains(Registries.BLOCK.getId(block)); }
    private record Data(List<String> noPenaltyBreakBlocks) {}
}
