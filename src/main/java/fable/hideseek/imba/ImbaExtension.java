package fable.hideseek.imba;

import com.mojang.brigadier.arguments.StringArgumentType;
import fable.hideseek.imba.config.BreakRulesConfig;
import fable.hideseek.imba.config.GameSettingsConfig;
import fable.hideseek.imba.config.HologramConfig;
import fable.hideseek.imba.config.RoundRestoreConfig;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.GameRoles;
import fable.hideseek.imba.item.RoundRestoreToolHandler;
import fable.hideseek.imba.net.BlockRulesNetworking;
import fable.hideseek.imba.net.HologramNetworking;
import fable.hideseek.imba.net.MaskNetworking;
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
        BlockRulesNetworking.register();
        RoundRestoreNetworking.register();
        HologramNetworking.register();
        RoundRestoreToolHandler.register();

        // A hider never gets to use vanilla entity attacks. The damage mixin below
        // is the second server-side safety net for indirect/projectile damage.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && GameRoles.isHider(player) && GameManager.isCurrentParticipant(player)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                HologramNetworking.sendSync(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_termination")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        RoundRestoreConfig.restoreAll(server);
                        if (!GameManager.resetRound(server)) {
                            GameManager.stopStandaloneTimer(server);
                        }
                        server.getPlayerManager().broadcast(
                                Text.literal("§cИгра аварийно завершена администратором"), false);
                        return 1;
                    }));

            dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_start_game")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(net.minecraft.server.command.CommandManager.argument(
                                    "location", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> {
                                for (var round : GameConfig.ROUNDS) {
                                    if (round.locationName != null && !round.locationName.isBlank()) {
                                        builder.suggest(round.locationName);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String requested = StringArgumentType.getString(ctx, "location").trim();
                                int found = -1;
                                for (int i = 0; i < GameConfig.ROUNDS.size(); i++) {
                                    var round = GameConfig.ROUNDS.get(i);
                                    if (requested.equalsIgnoreCase(round.locationName)
                                            || requested.equalsIgnoreCase(round.id)) {
                                        found = i;
                                        break;
                                    }
                                }
                                if (found < 0) {
                                    ctx.getSource().sendError(Text.literal(
                                            "Локация не найдена: " + requested));
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
}
