package fable.hideseek.imba.game;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalReturnTimingContractTest {
    private static String read(String rel) throws Exception {
        return Files.readString(Path.of(rel));
    }

    @Test
    void seekerFailSoundUsesRequestedGenericHurtFromSeekerPosition() throws Exception {
        String fixes = read("src/main/java/fable/hideseek/imba/mixin/GameplayFixesMixin.java");
        String deduction = read("src/main/java/fable/hideseek/imba/mixin/GameManagerExtensionMixin.java");

        assertFalse(fixes.contains("damageSeekerHeart"),
                "a later competing callback can be skipped after the deduction mixin cancels");
        assertTrue(deduction.contains("playsound minecraft:entity.generic.hurt player @a ~ ~ ~ 10 1"));
        assertFalse(deduction.contains("playsound minecraft:entity.player.hurt player @a ~ ~ ~ 10 1"));
        assertTrue(deduction.contains("seeker.getCommandSource().withLevel(4)"));
        assertFalse(deduction.contains("server.getCommandSource(), SEEKER_FAIL_SOUND_COMMAND"));

        int guard = deduction.indexOf("if (seeker == null || eliminatedSeekers.contains(seeker.getUuid()))");
        int sound = deduction.indexOf("imba$playSeekerFailSound(seeker);");
        int healthDeduction = deduction.indexOf("float newHealth = seeker.getHealth() - 2.0F;");
        assertTrue(guard >= 0 && guard < sound && sound < healthDeduction,
                "sound must run exactly on the live deduction path, after rejection guards and before heart loss");
    }

    @Test
    void returnTimingHasTwoLiveServerDelays() throws Exception {
        Path configPath = Path.of("src/main/java/fable/hideseek/imba/config/ReturnTimingConfig.java");
        assertTrue(Files.exists(configPath));
        String config = read(configPath.toString());
        assertTrue(config.contains("PRE_FADE_DEFAULT_TICKS = 40"));
        assertTrue(config.contains("PRE_TELEPORT_DEFAULT_TICKS = 40"));
        assertTrue(config.contains("preFadeTicks"));
        assertTrue(config.contains("preTeleportTicks"));

        String mixin = read("src/main/java/fable/hideseek/imba/mixin/LobbyReturnMixin.java");
        assertTrue(mixin.contains("ReturnTimingConfig"));
        assertTrue(mixin.contains("tickReturn"));
        assertTrue(mixin.contains("FADE_IN_TICKS"));
        assertFalse(mixin.contains("method = \"beginReturn\", at = @At(\"HEAD\")"));
    }

    @Test
    void returnTimingToolIsSeparateAndOpensLiveSettings() throws Exception {
        String extension = read("src/main/java/fable/hideseek/imba/ImbaExtension.java");
        String client = read("src/main/java/fable/hideseek/imba/ImbaClientExtension.java");
        assertTrue(extension.contains("RETURN_TIMING_TOOL"));
        assertTrue(extension.contains("return_timing_tool"));
        assertTrue(extension.contains("ReturnTimingNetworking.register()"));
        assertTrue(client.contains("ReturnTimingClientNetworking.register()"));
        assertTrue(client.contains("new ReturnTimingScreen()"));
    }

    @Test
    void playerPortalAnimationHasConfigurableAccumulatingFreeze() throws Exception {
        Path configPath = Path.of("src/main/java/fable/hideseek/imba/config/PortalAnimationConfig.java");
        Path clockPath = Path.of("src/main/java/fable/hideseek/imba/client/PortalMaskAnimationClock.java");
        assertTrue(Files.exists(configPath));
        assertTrue(Files.exists(clockPath));

        String config = read(configPath.toString());
        String clock = read(clockPath.toString());
        String render = read("src/main/java/fable/hideseek/imba/mixin/client/PortalMaskAnimationMixin.java");
        assertTrue(config.contains("DEFAULT_FREEZE_TICKS = 10"));
        assertTrue(clock.contains("freezeTicks"));
        assertTrue(clock.contains("holdTicks"));
        assertTrue(render.contains("PortalMaskAnimationClock"));
        assertTrue(render.contains("renderPortalMaskFrame"));
    }

    @Test
    void playerPortalFreezePreservesTheWorkingV10VanillaBlockAtlasPath() throws Exception {
        String helper = read("src/main/java/fable/hideseek/imba/client/MaskRenderHelper.java");
        String clock = read("src/main/java/fable/hideseek/imba/client/PortalMaskAnimationClock.java");
        String mixins = read("src/main/resources/imba.mixins.json");

        // The working v10 renderer must stay the source of portal geometry/state.
        assertTrue(helper.contains("Blocks.NETHER_PORTAL.getDefaultState()"));
        assertTrue(helper.contains("renderBlock(matrices, consumers, light, Blocks.NETHER_PORTAL.getDefaultState())"));
        assertFalse(helper.contains("PortalMaskAnimationClock"));

        // Freeze may only swap the animated atlas sprite; it must not use a raw PNG/entity layer.
        assertTrue(clock.contains("PORTAL_BUFFER_SPRITE"));
        assertTrue(clock.contains("SpriteContentsUploadAccessor"));
        assertTrue(clock.contains("consumers.getBuffer(requestedLayer)"));
        assertTrue(clock.contains("block/nether_portal"));
        assertFalse(clock.contains("RenderLayer.getEntityTranslucent"));
        assertFalse(clock.contains("PORTAL_TEXTURE"));
        assertTrue(mixins.contains("client.SpriteContentsUploadAccessor"));
    }

    @Test
    void portalAnimationToolIsSeparateAndLiveSynced() throws Exception {
        String extension = read("src/main/java/fable/hideseek/imba/ImbaExtension.java");
        String client = read("src/main/java/fable/hideseek/imba/ImbaClientExtension.java");
        assertTrue(extension.contains("PORTAL_ANIMATION_TOOL"));
        assertTrue(extension.contains("portal_animation_tool"));
        assertTrue(extension.contains("PortalAnimationNetworking.register()"));
        assertTrue(client.contains("PortalAnimationClientNetworking.register()"));
        assertTrue(client.contains("new PortalAnimationScreen()"));
    }

    @Test
    void vanillaPortalContactIsBoundToTickWorldAndPortal() throws Exception {
        String portal = read("src/main/java/fable/hideseek/imba/mixin/NetherPortalBlockMixin.java");
        assertTrue(portal.contains("PortalTransitGuard"));
        assertTrue(portal.contains("world.getTime()"));
        assertTrue(portal.contains("BlockPos"));
        assertFalse(portal.contains("Map<UUID, Integer> IMBA_PORTAL_TICKS"));
    }

    @Test
    void playerPortalContactCannotDoubleCountOrCrossDimensions() throws Exception {
        Path mixinPath = Path.of("src/main/java/fable/hideseek/imba/mixin/GameManagerPortalTransitMixin.java");
        assertTrue(Files.exists(mixinPath));
        String mixin = read(mixinPath.toString());
        assertTrue(mixin.contains("PortalTransitGuard"));
        assertTrue(mixin.contains("portalContacts"));
        assertTrue(mixin.contains("tickPortalMasks"));
        assertTrue(mixin.contains("PLAYER_PORTAL_TICKS = 75"));

        String mixins = read("src/main/resources/imba.mixins.json");
        assertTrue(mixins.contains("\"GameManagerPortalTransitMixin\""));
    }

    @Test
    void reversePortalConfigKeepsOverworldFallbackWhenWorldFieldIsMissing() throws Exception {
        String portalConfig = read("src/main/java/fable/hideseek/imba/config/PortalConfig.java");
        assertTrue(portalConfig.contains("mergeWithDefaults"));
        assertTrue(portalConfig.contains("minecraft:overworld"));
    }
}
