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
    void blockLikeVisualHitboxIsSeparateFromOwnerMovementCollision() throws Exception {
        String helper = read("src/main/java/fable/hideseek/imba/mask/MaskMovementCollision.java");
        String serverMixin = read("src/main/java/fable/hideseek/imba/mixin/MaskedMovementCollisionMixin.java");
        String clientMixin = read("src/main/java/fable/hideseek/imba/mixin/client/ClientMaskedMovementCollisionMixin.java");
        String mixins = read("src/main/resources/imba.mixins.json");
        assertTrue(helper.contains("OWNER_MAX_WIDTH = 0.60D"));
        assertTrue(helper.contains("OWNER_MAX_HEIGHT = 1.80D"));
        assertTrue(serverMixin.contains("MaskMovementCollision.ownerMovementBox"));
        assertTrue(clientMixin.contains("MaskMovementCollision.ownerMovementBox"));
        assertTrue(mixins.contains("\"MaskedMovementCollisionMixin\""));
        assertTrue(mixins.contains("\"client.ClientMaskedMovementCollisionMixin\""));
    }

    @Test
    void potion2dKeepsItsSpecialNonBlockHitbox() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        String serverMixin = read("src/main/java/fable/hideseek/imba/mixin/MaskedMovementCollisionMixin.java");
        String clientMixin = read("src/main/java/fable/hideseek/imba/mixin/client/ClientMaskedMovementCollisionMixin.java");
        assertTrue(hitbox.contains("MaskService.isSpecialPotion(item)"));
        assertTrue(hitbox.contains("SPECIAL_POTION"));
        assertTrue(serverMixin.contains("MaskService.isSpecialPotion(state.item)"));
        assertTrue(clientMixin.contains("MaskService.isSpecialPotion(item)"));
    }

    @Test
    void doorVisualHitboxIsExactlyOneByTwoBlocksOnBothSides() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        String clientDimensions = read("src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java");
        assertTrue(hitbox.contains("DOOR = EntityDimensions.fixed(1.0F, 2.0F)"));
        assertTrue(clientDimensions.contains("if(type==MaskType.DOOR)"));
        assertTrue(clientDimensions.contains("MaskHitbox.getDimensions(type, item)"));
    }

    @Test
    void existingBlockHitboxesAreNotReplacedByPlayerSizedDimensions() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        String config = read("src/main/java/fable/hideseek/imba/config/MaskHitboxConfig.java");
        assertTrue(hitbox.contains("BLOCK_LIKE"));
        assertTrue(hitbox.contains("MaskHitboxConfig.boundsFor(block)"));
        assertTrue(config.contains("block == ImbaMod.HANGING_LANTERN"));
        assertFalse(hitbox.contains("if (type != MaskType.ITEM) return DEFAULT_PLAYER"));
    }

    @Test
    void maskedOwnerDoesNotGetPushedOutByNeighbourBlocksOnClient() throws Exception {
        String pushOut = read("src/main/java/fable/hideseek/imba/mixin/client/ClientPlayerMaskPushOutMixin.java");
        String mixins = read("src/main/resources/imba.mixins.json");
        assertTrue(pushOut.contains("@Mixin(ClientPlayerEntity.class)"));
        assertTrue(pushOut.contains("method = \"pushOutOfBlocks\""));
        assertTrue(pushOut.contains("ClientMaskData.hasMask"));
        assertTrue(pushOut.contains("ci.cancel()"));
        assertTrue(pushOut.contains("MaskService.isSpecialPotion"));
        assertTrue(mixins.contains("\"client.ClientPlayerMaskPushOutMixin\""));
    }

    @Test
    void serverPostMoveValidationUsesOwnerMovementBoxInsteadOfVisualMaskBox() throws Exception {
        String network = read("src/main/java/fable/hideseek/imba/mixin/ServerPlayNetworkHandlerMixin.java");
        assertTrue(network.contains("method = \"isPlayerNotCollidingWithBlocks("));
        assertTrue(network.contains("MaskMovementCollision.ownerMovementBox"));
        assertTrue(network.contains("MaskState.hasMask(player.getUuid())"));
        assertTrue(network.contains("MaskService.isSpecialPotion(state.item)"));
    }
}
