package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskHitboxAllBlocksContractTest {
    private static String read(String rel) throws Exception {
        return Files.readString(Path.of(rel));
    }

    @Test
    void editorSyncIncludesEveryRegisteredBlockAndAllowsEditingFullBlocks() throws Exception {
        String networking = read("src/main/java/fable/hideseek/imba/net/MaskHitboxNetworking.java");
        String screen = read("src/main/java/fable/hideseek/imba/client/MaskHitboxConfigScreen.java");

        assertTrue(networking.contains("Registries.BLOCK.getIds()"),
                "server sync must enumerate the complete block registry");
        assertFalse(networking.contains("MaskBlockConfig.nonFullBlocksSnapshot().contains(id.toString())"),
                "SET must not reject blocks merely because they are classified FULL");
        assertTrue(screen.contains("Хитбоксы маскировок"));
        assertTrue(screen.contains("Поиск блока..."));
        assertFalse(screen.contains("Только блоки, отмеченные как НЕПОЛНЫЕ"));
    }

    @Test
    void stonecutterBladeDefaultIsHorizontalLowerHalfBlockAndMigratesLegacyVerticalShape() throws Exception {
        String config = read("src/main/java/fable/hideseek/imba/config/MaskHitboxConfig.java");
        String collisions = read("src/main/java/fable/hideseek/imba/mask/MaskCollisionShapes.java");

        assertTrue(config.contains("ImbaMod.STONRCUTTER_LEZVIE"));
        assertTrue(config.contains("new Bounds(0,0,0,16,8,16)"));
        assertTrue(config.contains("isLegacyVerticalStonecutter"));
        assertTrue(collisions.contains("state.block == ImbaMod.STONRCUTTER_LEZVIE"),
                "stonecutter must use the configured horizontal default even without a custom override");
    }

    @Test
    void customHitboxOverridesApplyToFullBlocksToo() throws Exception {
        String collisions = read("src/main/java/fable/hideseek/imba/mask/MaskCollisionShapes.java");
        String dimensions = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");

        assertTrue(collisions.contains("MaskHitboxConfig.hasCustom(state.block)"));
        assertFalse(collisions.contains("!MaskBlockConfig.isFull(state.block) && MaskHitboxConfig.hasCustom(state.block)"));
        assertTrue(dimensions.contains("MaskHitboxConfig.hasCustom(block)"));
    }
}
