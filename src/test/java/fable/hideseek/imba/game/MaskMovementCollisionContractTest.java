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
        String mixins = read("src/main/resources/imba.mixins.json");
        String network = read("src/main/java/fable/hideseek/imba/mixin/ServerPlayNetworkHandlerMixin.java");

        // The server owner must never carry mask-sized EntityDimensions through vanilla physics.
        assertFalse(serverPlayer.contains("MaskHitbox.getDimensions"));
        // Preserve mask camera/eye-height behavior: this is not a collision change.
        assertTrue(serverPlayer.contains("MaskHitbox.getEyeHeight"));

        // The local client owner uses vanilla dimensions for prediction, while remote masked
        // players still expose the external mask-sized hitbox to the observer/seeker client.
        assertTrue(clientPlayer.contains("MinecraftClient.getInstance().player"));
        assertTrue(clientPlayer.contains("if (self == localPlayer) return;"));
        assertTrue(clientPlayer.contains("MaskHitbox.getDimensions"));
        assertTrue(clientPlayer.contains("MaskHitbox.getEyeHeight"));

        // All v19-v21 owner-physics interception hooks are removed from the active mixin set.
        assertFalse(mixins.contains("\"MaskedMovementCollisionMixin\""));
        assertFalse(mixins.contains("\"client.ClientMaskedMovementCollisionMixin\""));
        assertFalse(mixins.contains("\"client.ClientPlayerMaskPushOutMixin\""));
        assertFalse(mixins.contains("\"DoorCollisionMixin\""));
        assertFalse(mixins.contains("\"client.ClientDoorCollisionMixin\""));
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
