package fable.hideseek.imba;

import com.mojang.brigadier.arguments.StringArgumentType;
import fable.hideseek.imba.config.BreakRulesConfig;
import fable.hideseek.imba.config.GameSettingsConfig;
import fable.hideseek.imba.config.HologramConfig;
import fable.hideseek.imba.config.PanelSettingsConfig;
import fable.hideseek.imba.config.RoundRestoreConfig;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.GameRoles;
import fable.hideseek.imba.item.RoundRestoreToolHandler;
import fable.hideseek.imba.net.BlockRulesNetworking;
import fable.hideseek.imba.net.HologramNetworking;
import fable.hideseek.imba.net.MaskNetworking;
import fable.hideseek.imba.net.PanelSettingsNetworking;
import fable.hideseek.imba.net.RoundRestoreNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.text.Normalizer;
import java.util.Locale;

/** Extra admin/gameplay systems kept separate from the original core initializer. */
public final class ImbaExtension implements ModInitializer {
    public static final Item BLOCK_RULES_TOOL = new Item(new Item.Settings().maxCount(1));
    public static final Item BLOCK_RESTORE_TOOL = new Item(new Item.Settings().maxCount(1));
    public static final Item STRUCTURE_LAYER_TOOL = new Item(new Item.Settings().maxCount(1));
    public static final Item HOLOGRAM_PROJECTOR_TOOL = new Item(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier("imba", "block_rules_tool"), BLOCK_RULES_TOOL);
        Registry.register(Registries.ITEM, new Identifier("imba", "block_restore_tool"), BLOCK_RESTORE_TOOL);
        Registry.register(Registries.ITEM, new Identifier("imba", "structure_layer_tool"), STRUCTURE_LAYER_TOOL);
        Registry.register(Registries.ITEM, new Identifier("imba", "hologram_projector_tool"), HOLOGRAM_PROJECTOR_TOOL);

        RegistryKey<ItemGroup> imbaGroup = RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier("imba", "main"));
        ItemGroupEvents.modifyEntriesEvent(imbaGroup).register(entries -> {
            entries.add(BLOCK_RULES_TOOL);
            entries.add(BLOCK_RESTORE_TOOL);
            entries.add(STRUCTURE_LAYER_TOOL);
            entries.add(HOLOGRAM_PROJECTOR_TOOL);
        });

        BreakRulesConfig.load();
        RoundRestoreConfig.load();
        HologramConfig.load();
        PanelSettingsConfig.load();
        BlockRulesNetworking.register();
        RoundRestoreNetworking.register();
        HologramNetworking.register();
        PanelSettingsNetworking.register();
        RoundRestoreToolHandler.register();

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && GameRoles.isHider(player) && GameManager.isCurrentParticipant(player)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            HologramNetworking.sendSync(handler.getPlayer());
            PanelSettingsNetworking.sendSync(handler.getPlayer());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_termination")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        if (!GameManager.resetRound(server)) {
                            RoundRestoreConfig.restoreEnabled(server);
                            GameManager.stopStandaloneTimer(server);
                        }
                        server.getPlayerManager().broadcast(
                                Text.literal("§cИгра аварийно завершена администратором"), false);
                        return 1;
                    }));

            dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_reset_locations")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ctx -> {
                        var result = RoundRestoreConfig.restoreEnabled(ctx.getSource().getServer());
                        if (!result.anythingRestored()) {
                            ctx.getSource().sendFeedback(() -> Text.literal("§eНет сохранённых элементов для восстановления"), false);
                            return 0;
                        }
                        ctx.getSource().sendFeedback(() -> Text.literal(
                                "§aЛокации восстановлены §7• точек: §f" + result.points()
                                        + " §7• слоёв: §f" + result.layers()
                                        + " §7• блоков всего: §f" + result.blocks()), true);
                        return 1;
                    }));

            dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_start_game")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(net.minecraft.server.command.CommandManager.argument(
                                    "location", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> {
                                for (var round : GameConfig.ROUNDS) {
                                    if (round.locationName != null && !round.locationName.isBlank()) {
                                        String value = round.locationName.contains(" ")
                                                ? "\"" + round.locationName + "\"" : round.locationName;
                                        builder.suggest(value);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String raw = StringArgumentType.getString(ctx, "location");
                                String requested = normalizeLocation(raw);
                                int found = -1;
                                for (int i = 0; i < GameConfig.ROUNDS.size(); i++) {
                                    var round = GameConfig.ROUNDS.get(i);
                                    if (requested.equals(normalizeLocation(round.locationName))
                                            || requested.equals(normalizeLocation(round.id))) {
                                        found = i;
                                        break;
                                    }
                                }
                                if (found < 0) {
                                    ctx.getSource().sendError(Text.literal(
                                            "Локация не найдена: " + stripOuterQuotes(raw).trim()));
                                    return 0;
                                }
                                GameConfig.setSelectedLocation(found);
                                GameSettingsConfig.save();
                                MaskNetworking.broadcastPanelData(ctx.getSource().getServer());
                                ServerPlayerEntity starter = ctx.getSource().getEntity() instanceof ServerPlayerEntity p
                                        ? p : null;
                                GameManager.startNextRound(ctx.getSource().getServer(), starter);
                                return 1;
                            })));
        });
    }

    private static String normalizeLocation(String value) {
        String clean = stripOuterQuotes(value == null ? "" : value);
        clean = clean.replaceAll("§.", "");
        clean = Normalizer.normalize(clean, Normalizer.Form.NFKC);
        clean = clean.trim().replaceAll("\\s+", " ");
        return clean.toLowerCase(Locale.ROOT);
    }

    private static String stripOuterQuotes(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() >= 2) {
            char first = clean.charAt(0);
            char last = clean.charAt(clean.length() - 1);
            boolean quoted = (first == '"' && last == '"')
                    || (first == '\'' && last == '\'')
                    || (first == '«' && last == '»')
                    || (first == '“' && last == '”');
            if (quoted) clean = clean.substring(1, clean.length() - 1);
        }
        return clean;
    }
}
