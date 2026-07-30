package fable.hideseek.imba.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import fable.hideseek.imba.boosty_toggle.BoostyToogler;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.config.GameSettingsConfig;
import fable.hideseek.imba.item.SeekerSwordUtil;
import fable.hideseek.imba.item.TeleportToolHandler;
import fable.hideseek.imba.mask.MaskService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CommandInit {
        public static void register() {
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {

                        dispatcher.register(CommandManager.literal("imba_mask")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager
                                                        .argument("block",
                                                                        BlockStateArgumentType
                                                                                        .blockState(registryAccess))
                                                        .executes(ctx -> {
                                                                ServerPlayerEntity p = ctx.getSource()
                                                                                .getPlayerOrThrow();
                                                                var block = BlockStateArgumentType
                                                                                .getBlockState(ctx, "block")
                                                                                .getBlockState().getBlock();
                                                                MaskService.applyBlockMask(p, block);
                                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                                                "§aМаска установлена: блок §6" + block
                                                                                                .getName().getString()),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_mask_item")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager
                                                        .argument("item",
                                                                        ItemStackArgumentType.itemStack(registryAccess))
                                                        .executes(ctx -> {
                                                                ServerPlayerEntity p = ctx.getSource()
                                                                                .getPlayerOrThrow();
                                                                var item = ItemStackArgumentType
                                                                                .getItemStackArgument(ctx, "item")
                                                                                .getItem();
                                                                MaskService.applyItemMask(p, item);
                                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                                                "§aМаска установлена: предмет §6" + item
                                                                                                .getName().getString()),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_reset")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                                MaskService.resetMask(p);
                                                ctx.getSource().sendFeedback(() -> Text.literal("§aМаска сброшена!"),
                                                                true);
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_resync")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                fable.hideseek.imba.net.MaskNetworking.resyncAll(
                                                                ctx.getSource().getServer());
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal(
                                                                                "§aСостояние масок синхронизировано заново"),
                                                                true);
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_boosty")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.literal("on").executes(ctx -> {
                                                BoostyToogler.setEnabled(true);
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal("§aBoosty режим включён"), true);
                                                return 1;
                                        }))
                                        .then(CommandManager.literal("off").executes(ctx -> {
                                                BoostyToogler.setEnabled(false);
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal("§cBoosty режим выключен"), true);
                                                return 1;
                                        }))
                                        .then(CommandManager.literal("toggle").executes(ctx -> {
                                                boolean state = BoostyToogler.toggle();
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal(state ? "§aBoosty режим включён"
                                                                                : "§cBoosty режим выключен"),
                                                                true);
                                                return 1;
                                        })));

                        dispatcher.register(CommandManager.literal("imba_messages")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                boolean enabled = GameConfig.SHOW_GAMEPLAY_MESSAGES;
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal(enabled
                                                                                ? "§aИгровые уведомления включены"
                                                                                : "§7Игровые уведомления выключены"),
                                                                false);
                                                return 1;
                                        })
                                        .then(CommandManager.literal("on").executes(ctx -> {
                                                GameConfig.setShowGameplayMessages(true);
                                                GameSettingsConfig.save();
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal("§aИгровые уведомления включены"),
                                                                false);
                                                return 1;
                                        }))
                                        .then(CommandManager.literal("off").executes(ctx -> {
                                                GameConfig.setShowGameplayMessages(false);
                                                GameSettingsConfig.save();
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal("§7Игровые уведомления выключены"),
                                                                false);
                                                return 1;
                                        }))
                                        .then(CommandManager.literal("toggle").executes(ctx -> {
                                                boolean enabled = !GameConfig.SHOW_GAMEPLAY_MESSAGES;
                                                GameConfig.setShowGameplayMessages(enabled);
                                                GameSettingsConfig.save();
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal(enabled
                                                                                ? "§aИгровые уведомления включены"
                                                                                : "§7Игровые уведомления выключены"),
                                                                false);
                                                return 1;
                                        })));

                        dispatcher.register(CommandManager.literal("imba_round_start")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                GameManager.startNextRound(ctx.getSource().getServer());
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal(
                                                                                "§aКоманда запуска раунда отправлена"),
                                                                true);
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_round_stop")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                GameManager.stopAndReturnToLobby(ctx.getSource().getServer());
                                                ctx.getSource().sendFeedback(() -> Text.literal("§cРаунд остановлен"),
                                                                true);
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_round_pause")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                if (!GameManager.pauseGame(ctx.getSource().getServer())) {
                                                        ctx.getSource().sendError(Text.literal(
                                                                        "Активный раунд уже на паузе или отсутствует"));
                                                        return 0;
                                                }
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_round_resume")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                if (!GameManager.resumeGame(ctx.getSource().getServer())) {
                                                        ctx.getSource().sendError(Text.literal(
                                                                        "Нет приостановленного раунда"));
                                                        return 0;
                                                }
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_round_reset")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                if (!GameManager.resetRound(ctx.getSource().getServer())) {
                                                        ctx.getSource().sendError(Text.literal(
                                                                        "Нет активного раунда для сброса"));
                                                        return 0;
                                                }
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_tp_tool")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                player.getInventory().insertStack(TeleportToolHandler.createTool());
                                                ctx.getSource().sendFeedback(() -> Text
                                                                .literal("§aВыдан инструмент настройки телепортов"),
                                                                true);
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_tp_mode")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                                ServerPlayerEntity player = ctx.getSource()
                                                                                .getPlayerOrThrow();
                                                                String mode = StringArgumentType.getString(ctx, "mode");
                                                                TeleportToolHandler.setMode(player, mode);
                                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                                                "§aТочка настройки: §f"
                                                                                                + GameConfig.getPointDisplayName(mode)),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_set_seeker_hearts")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("hearts", IntegerArgumentType.integer(1, 100))
                                                        .executes(ctx -> {
                                                                int hearts = IntegerArgumentType.getInteger(ctx,
                                                                                "hearts");
                                                                GameConfig.setSeekerHearts(hearts);
                                                                fable.hideseek.imba.config.GameSettingsConfig.save();
                                                                fable.hideseek.imba.net.MaskNetworking.broadcastPanelData(
                                                                                ctx.getSource().getServer());
                                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                                                "§aСердца искателя установлены: §f"
                                                                                                + hearts),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_set_prepare_time")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 36000))
                                                        .executes(ctx -> {
                                                                int seconds = IntegerArgumentType.getInteger(ctx,
                                                                                "seconds");
                                                                GameConfig.setPrepareSeconds(seconds);
                                                                ctx.getSource().sendFeedback(() -> Text
                                                                                .literal("§aВремя подготовки: §f"
                                                                                                + seconds + " сек."),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_set_round_time")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 36000))
                                                        .executes(ctx -> {
                                                                int seconds = IntegerArgumentType.getInteger(ctx,
                                                                                "seconds");
                                                                GameConfig.setRoundSeconds(seconds);
                                                                fable.hideseek.imba.config.GameSettingsConfig.save();
                                                                fable.hideseek.imba.net.MaskNetworking.broadcastPanelData(
                                                                                ctx.getSource().getServer());
                                                                ctx.getSource().sendFeedback(
                                                                                () -> Text.literal("§aВремя раунда: §f"
                                                                                                + seconds + " сек."),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_set_location")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("number", IntegerArgumentType.integer(1, 12))
                                                        .executes(ctx -> {
                                                                int number = IntegerArgumentType.getInteger(ctx, "number");
                                                                GameConfig.setSelectedLocation(number - 1);
                                                                fable.hideseek.imba.config.GameSettingsConfig.save();
                                                                fable.hideseek.imba.net.MaskNetworking.broadcastPanelData(
                                                                                ctx.getSource().getServer());
                                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                                                "§aВыбрана "
                                                                                                + GameConfig.getLocationName(number - 1)),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_set_return_time")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 36000))
                                                        .executes(ctx -> {
                                                                int seconds = IntegerArgumentType.getInteger(ctx,
                                                                                "seconds");
                                                                GameConfig.setReturnToLobbySeconds(seconds);
                                                                ctx.getSource().sendFeedback(() -> Text
                                                                                .literal("§aВремя возврата в лобби: §f"
                                                                                                + seconds + " сек."),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_timer_test_start")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 36000))
                                                        .executes(ctx -> {
                                                                int seconds = IntegerArgumentType.getInteger(ctx,
                                                                                "seconds");
                                                                boolean started = GameManager.startStandaloneTimer(
                                                                                ctx.getSource().getServer(), seconds);

                                                                if (!started) {
                                                                        ctx.getSource().sendError(Text.literal(
                                                                                        "Сейчас нельзя запустить тестовый таймер: игра уже активна"));
                                                                        return 0;
                                                                }

                                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                                                "§aТестовый таймер запущен на §f"
                                                                                                + seconds + " §aсек."),
                                                                                true);
                                                                return 1;
                                                        })));

                        dispatcher.register(CommandManager.literal("imba_timer_test_stop")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                GameManager.stopStandaloneTimer(ctx.getSource().getServer());
                                                ctx.getSource().sendFeedback(
                                                                () -> Text.literal("§cТестовый таймер остановлен"),
                                                                true);
                                                return 1;
                                        }));

                        dispatcher.register(CommandManager.literal("imba_give_seeker_sword")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                player.getInventory().insertStack(SeekerSwordUtil.createSword());
                                                ctx.getSource().sendFeedback(() -> Text.literal("§aВыдан меч искателя"),
                                                                true);
                                                return 1;
                                        }));
                });
        }
}
