package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskMovementCollisionContractTest {
    private static String read(String rel) throws Exception {
        return Files.readString(Path.of(rel));
    }

    @Test
    void ownerPhysicsIsVanillaWhileRemoteMaskHitboxStaysBlockLike() throws Exception {
        String serverPlayer = read("src/main/java/fable/hideseek/imba/mixin/PlayerEntityMixin.java");
        String clientPlayer = read("src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java");
        String serverMove = read("src/main/java/fable/hideseek/imba/mixin/MaskedMovementCollisionMixin.java");
        String clientMove = read("src/main/java/fable/hideseek/imba/mixin/client/ClientMaskedMovementCollisionMixin.java");
        String pushOut = read("src/main/java/fable/hideseek/imba/mixin/client/ClientPlayerMaskPushOutMixin.java");
        String network = read("src/main/java/fable/hideseek/imba/mixin/ServerPlayNetworkHandlerMixin.java");

        assertFalse(serverPlayer.contains("MaskHitbox.getDimensions"));
        assertFalse(serverPlayer.contains("MaskHitbox.getEyeHeight"));

        assertTrue(clientPlayer.contains("MinecraftClient.getInstance().player"));
        assertTrue(clientPlayer.contains("if (self == localPlayer) return;"));
        assertTrue(clientPlayer.contains("MaskHitbox.getDimensions"));
        assertTrue(clientPlayer.contains("MaskHitbox.getEyeHeight"));

        assertFalse(serverMove.contains("method = \"move\""));
        assertFalse(serverMove.contains("MaskMovementCollision.ownerMovementBox"));
        assertFalse(clientMove.contains("method = \"move\""));
        assertFalse(clientMove.contains("MaskMovementCollision.ownerMovementBox"));
        assertFalse(pushOut.contains("pushOutOfBlocks"));
        assertFalse(network.contains("isPlayerNotCollidingWithBlocks"));
    }

    @Test
    void potion2dKeepsItsSpecialNonBlockHitbox() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        assertTrue(hitbox.contains("MaskService.isSpecialPotion(item)"));
        assertTrue(hitbox.contains("SPECIAL_POTION"));
    }

    @Test
    void doorVisualHitboxIsExactlyOneByTwoBlocksForRemotePlayers() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        String clientDimensions = read("src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java");
        assertTrue(hitbox.contains("DOOR = EntityDimensions.fixed(1.0F, 2.0F)"));
        assertTrue(clientDimensions.contains("if(type==MaskType.DOOR)"));
        assertTrue(clientDimensions.contains("MaskHitbox.getDimensions(type, item)"));
    }

    @Test
    void existingExternalMaskBoundsAreNotReplacedByPlayerSizedMaskBounds() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        String config = read("src/main/java/fable/hideseek/imba/config/MaskHitboxConfig.java");
        assertTrue(hitbox.contains("BLOCK_LIKE"));
        assertTrue(hitbox.contains("MaskHitboxConfig.boundsFor(block)"));
        assertTrue(config.contains("block == ImbaMod.HANGING_LANTERN"));
        assertFalse(hitbox.contains("if (type != MaskType.ITEM) return DEFAULT_PLAYER"));
    }
}
