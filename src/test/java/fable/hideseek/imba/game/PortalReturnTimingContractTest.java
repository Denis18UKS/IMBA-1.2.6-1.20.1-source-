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
    void seekerFailSoundRemainsBoundToHeartDeduction() throws Exception {
        String fixes = read("src/main/java/fable/hideseek/imba/mixin/GameplayFixesMixin.java");
        assertTrue(fixes.contains("damageSeekerHeart"));
        assertTrue(fixes.contains("playsound minecraft:entity.generic.hurt player @a ~ ~ ~ 10 1"));
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
