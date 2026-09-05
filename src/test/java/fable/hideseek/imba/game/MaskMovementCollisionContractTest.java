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
    void onlyDoorLadderAndSculkVeinKeepV22OwnerPhysics() throws Exception {
        String serverPlayer = read("src/main/java/fable/hideseek/imba/mixin/PlayerEntityMixin.java");
        String clientPlayer = read("src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java");

        // pr29-final behavior is restored for every other mask: the owner receives
        // the mask's own dimensions again instead of the blanket vanilla-player box from v22.
        assertTrue(serverPlayer.contains("MaskHitbox.getDimensions(state.type, state.block, state.item)"));
        assertTrue(serverPlayer.contains("usesV22OwnerPhysics"));
        assertTrue(serverPlayer.contains("MaskType.DOOR"));
        assertTrue(serverPlayer.contains("MaskType.LADDER_REVERSED"));
        assertTrue(serverPlayer.contains("MaskType.SCULK_VEIN"));

        // Local prediction must use the same three exceptions only. The old v22 blanket
        // `if (self == localPlayer) return` would wrongly turn hanging_lantern and every
        // other disguise back into the vanilla 0.6x1.8 player hitbox.
        assertTrue(clientPlayer.contains("usesV22OwnerPhysics"));
        assertTrue(clientPlayer.contains("self == localPlayer && usesV22OwnerPhysics(type)"));
        assertFalse(clientPlayer.contains("if (self == localPlayer) return;"));
        assertTrue(clientPlayer.contains("MaskHitbox.getDimensions"));
    }

    @Test
    void hangingLanternUsesPr29FullBlockHitbox() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        String config = read("src/main/java/fable/hideseek/imba/config/MaskHitboxConfig.java");

        assertTrue(hitbox.contains("block == ImbaMod.HANGING_LANTERN"));
        assertTrue(config.contains("if (block == ImbaMod.HANGING_LANTERN)"));
        assertTrue(config.contains("return Bounds.FULL.copy();"));
        assertTrue(hitbox.contains("MaskHitboxConfig.boundsFor(block)"));
    }

    @Test
    void potion2dKeepsItsSpecialNonBlockHitbox() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        assertTrue(hitbox.contains("MaskService.isSpecialPotion(item)"));
        assertTrue(hitbox.contains("SPECIAL_POTION"));
    }

    @Test
    void doorVisualHitboxStaysExactlyOneByTwoBlocksFromV22() throws Exception {
        String hitbox = read("src/main/java/fable/hideseek/imba/mask/MaskHitbox.java");
        String clientDimensions = read("src/main/java/fable/hideseek/imba/mixin/client/PlayerEntityClientMixin.java");
        assertTrue(hitbox.contains("DOOR = EntityDimensions.fixed(1.0F, 2.0F)"));
        assertTrue(clientDimensions.contains("if(type==MaskType.DOOR)"));
        assertTrue(clientDimensions.contains("MaskHitbox.getDimensions(type, item)"));
    }

    @Test
    void obsoleteV19ToV21CollisionHooksStayRemoved() throws Exception {
        String mixins = read("src/main/resources/imba.mixins.json");
        String network = read("src/main/java/fable/hideseek/imba/mixin/ServerPlayNetworkHandlerMixin.java");
        assertFalse(mixins.contains("\"MaskedMovementCollisionMixin\""));
        assertFalse(mixins.contains("\"client.ClientMaskedMovementCollisionMixin\""));
        assertFalse(mixins.contains("\"client.ClientPlayerMaskPushOutMixin\""));
        assertFalse(mixins.contains("\"DoorCollisionMixin\""));
        assertFalse(mixins.contains("\"client.ClientDoorCollisionMixin\""));
        assertFalse(network.contains("isPlayerNotCollidingWithBlocks"));
    }
}
