package fable.hideseek.imba;

import com.mojang.brigadier.arguments.StringArgumentType;
import fable.hideseek.imba.config.*;
import fable.hideseek.imba.game.*;
import fable.hideseek.imba.item.RoundRestoreToolHandler;
import fable.hideseek.imba.item.OverlayBarrierToolHandler;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.net.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ImbaExtension implements ModInitializer {
    public static final Item BLOCK_RULES_TOOL=new Item(new Item.Settings().maxCount(1));
    public static final Item BLOCK_RESTORE_TOOL=new Item(new Item.Settings().maxCount(1));
    public static final Item STRUCTURE_LAYER_TOOL=new Item(new Item.Settings().maxCount(1));
    public static final Item HOLOGRAM_PROJECTOR_TOOL=new Item(new Item.Settings().maxCount(1));
    public static final Item PANEL_SETTINGS_TOOL=new Item(new Item.Settings().maxCount(1));
    public static final Item AIR_FIXATION_TOOL=new Item(new Item.Settings().maxCount(1));
    public static final Item MESSAGE_SETTINGS_TOOL=new Item(new Item.Settings().maxCount(1));
    public static final Item OVERLAY_BARRIER=new Item(new Item.Settings().maxCount(1));
    public static final Item MASK_HITBOX_TOOL=new Item(new Item.Settings().maxCount(1));

    @Override public void onInitialize(){
        Registry.register(Registries.ITEM,new Identifier("imba","block_rules_tool"),BLOCK_RULES_TOOL);
        Registry.register(Registries.ITEM,new Identifier("imba","block_restore_tool"),BLOCK_RESTORE_TOOL);
        Registry.register(Registries.ITEM,new Identifier("imba","structure_layer_tool"),STRUCTURE_LAYER_TOOL);
        Registry.register(Registries.ITEM,new Identifier("imba","hologram_projector_tool"),HOLOGRAM_PROJECTOR_TOOL);
        Registry.register(Registries.ITEM,new Identifier("imba","panel_settings_tool"),PANEL_SETTINGS_TOOL);
        Registry.register(Registries.ITEM,new Identifier("imba","air_fixation_tool"),AIR_FIXATION_TOOL);
        Registry.register(Registries.ITEM,new Identifier("imba","message_settings_tool"),MESSAGE_SETTINGS_TOOL);
        Registry.register(Registries.ITEM,new Identifier("imba","overlay_barrier"),OVERLAY_BARRIER);
        Registry.register(Registries.ITEM,new Identifier("imba","mask_hitbox_tool"),MASK_HITBOX_TOOL);
        RegistryKey<ItemGroup> group=RegistryKey.of(RegistryKeys.ITEM_GROUP,new Identifier("imba","main"));
        ItemGroupEvents.modifyEntriesEvent(group).register(e->{e.add(BLOCK_RULES_TOOL);e.add(BLOCK_RESTORE_TOOL);e.add(STRUCTURE_LAYER_TOOL);e.add(HOLOGRAM_PROJECTOR_TOOL);e.add(PANEL_SETTINGS_TOOL);e.add(AIR_FIXATION_TOOL);e.add(MESSAGE_SETTINGS_TOOL);e.add(OVERLAY_BARRIER);e.add(MASK_HITBOX_TOOL);});
        AirFixationConfig.load();BreakRulesConfig.load();RoundRestoreConfig.load();HologramConfig.load();PanelSettingsConfig.load();MessageSettingsConfig.load();OverlayBarrierConfig.load();MaskHitboxConfig.load();
        AirFixationNetworking.register();BlockRulesNetworking.register();RoundRestoreNetworking.register();HologramNetworking.register();PanelSettingsNetworking.register();MessageSettingsNetworking.register();MaskHitboxNetworking.register();RoundRestoreToolHandler.register();OverlayBarrierToolHandler.register();DoorMaskCollisionHandler.register();
        AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->!world.isClient&&GameRoles.isHider(player)&&GameManager.isCurrentParticipant(player)?ActionResult.FAIL:ActionResult.PASS);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{HologramNetworking.sendSync(handler.getPlayer());PanelSettingsNetworking.sendSync(handler.getPlayer());MessageSettingsNetworking.sendSync(handler.getPlayer());OverlayBarrierNetworking.sendSync(handler.getPlayer());MaskHitboxNetworking.sendSync(handler.getPlayer());});
        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.getGameRules().get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, server));
        registerCommands();
    }

    private static void registerCommands(){CommandRegistrationCallback.EVENT.register((dispatcher,registryAccess,environment)->{
        dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_termination").requires(s->s.hasPermissionLevel(2)).executes(ctx->{ForcedTerminationService.terminate(ctx.getSource().getServer());return 1;}));
        dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_reset_locations").requires(s->s.hasPermissionLevel(2)).executes(ctx->{var r=RoundRestoreConfig.restoreEnabled(ctx.getSource().getServer());if(!r.anythingRestored()){ctx.getSource().sendFeedback(()->Text.literal("§eНет сохранённых элементов для восстановления"),false);return 0;}ctx.getSource().sendFeedback(()->Text.literal("§aЛокации восстановлены §7• точек: §f"+r.points()+" §7• слоёв: §f"+r.layers()+" §7• блоков всего: §f"+r.blocks()),true);return 1;}));
        dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_reset").requires(s->s.hasPermissionLevel(2)).then(net.minecraft.server.command.CommandManager.argument("player",GameProfileArgumentType.gameProfile()).executes(ctx->{int count=0;for(var profile:GameProfileArgumentType.getProfileArgument(ctx,"player")){var uuid=profile.getId();ServerPlayerEntity online=ctx.getSource().getServer().getPlayerManager().getPlayer(uuid);if(online!=null)MaskService.resetMask(online);else MaskState.reset(uuid);count++;}final int n=count;ctx.getSource().sendFeedback(()->Text.literal("§aСброшено маскировок: §f"+n),true);return n;})));
        dispatcher.register(net.minecraft.server.command.CommandManager.literal("imba_start_game").requires(s->s.hasPermissionLevel(2)).then(net.minecraft.server.command.CommandManager.argument("location",StringArgumentType.greedyString()).suggests((ctx,builder)->{String needle=normalizeLocation(builder.getRemaining().replace("\"","").replace("«","").replace("»",""));for(int i=0;i<GameConfig.ROUNDS.size();i++){String name=GameConfig.getLocationName(i),id=GameConfig.ROUNDS.get(i).id;if(needle.isEmpty()||normalizeLocation(name).contains(needle)||normalizeLocation(id).contains(needle))builder.suggest(name.contains(" ")?"\""+name.replace("\"","\\\"")+"\"":name);}return builder.buildFuture();}).executes(ctx->{String raw=StringArgumentType.getString(ctx,"location");int found=findLocationIndex(raw);if(found==-2){ctx.getSource().sendError(Text.literal("Найдено несколько локаций. Уточните название: "+stripOuterQuotes(raw).trim()));return 0;}if(found<0){ctx.getSource().sendError(Text.literal("Локация не найдена: "+stripOuterQuotes(raw).trim()));return 0;}GameConfig.setSelectedLocation(found);GameSettingsConfig.save();MaskNetworking.broadcastPanelData(ctx.getSource().getServer());ServerPlayerEntity starter=ctx.getSource().getEntity() instanceof ServerPlayerEntity p?p:null;GameManager.startNextRound(ctx.getSource().getServer(),starter);return 1;})));
    });}

    private static int findLocationIndex(String raw){String requested=normalizeLocation(raw);if(requested.isEmpty())return -1;List<Integer> partial=new ArrayList<>();for(int i=0;i<GameConfig.ROUNDS.size();i++){var round=GameConfig.ROUNDS.get(i);String configured=normalizeLocation(GameConfig.getLocationName(i)),rawName=normalizeLocation(round.locationName),id=normalizeLocation(round.id);if(requested.equals(configured)||requested.equals(rawName)||requested.equals(id)||requested.equals(Integer.toString(i+1)))return i;if(configured.contains(requested)||rawName.contains(requested)||id.contains(requested))partial.add(i);}return partial.size()==1?partial.get(0):partial.size()>1?-2:-1;}
    private static String normalizeLocation(String value){String clean=stripOuterQuotes(value==null?"":value).replaceAll("§.","");clean=Normalizer.normalize(clean,Normalizer.Form.NFKC).trim().replaceAll("\\s+"," ");return clean.toLowerCase(Locale.ROOT);}
    private static String stripOuterQuotes(String value){String clean=value==null?"":value.trim();if(clean.length()>=2){char a=clean.charAt(0),b=clean.charAt(clean.length()-1);if((a=='\"'&&b=='\"')||(a=='\''&&b=='\'')||(a=='«'&&b=='»')||(a=='“'&&b=='”'))clean=clean.substring(1,clean.length()-1);}return clean;}
}
