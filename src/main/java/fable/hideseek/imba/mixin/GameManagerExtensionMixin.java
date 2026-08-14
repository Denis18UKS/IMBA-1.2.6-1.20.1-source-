package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.RoundRestoreConfig;
import fable.hideseek.imba.config.TeleportConfig;
import fable.hideseek.imba.game.GameConfig;
import fable.hideseek.imba.game.GameManager;
import fable.hideseek.imba.game.RoundDefinition;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(GameManager.class)
public abstract class GameManagerExtensionMixin {
    @Shadow @Final private static Set<UUID> currentSeekers;
    @Shadow @Final private static Set<UUID> eliminatedSeekers;
    @Shadow @Final private static Map<UUID, Vec3d> prepareSeekerAnchors;
    @Shadow private static RoundDefinition currentRound;
    @Shadow private static void teleport(ServerPlayerEntity player,RegistryKey<World> worldKey,Vec3d pos,float yaw,float pitch){throw new AssertionError();}
    @Shadow private static void finishHiderWinByHearts(MinecraftServer server){throw new AssertionError();}
    @Unique private static boolean imba$roundRestored;

    @Inject(method="startNextRound(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V",at=@At("HEAD"))
    private static void imba$resetRestoreFlag(MinecraftServer server,ServerPlayerEntity starter,CallbackInfo ci){imba$roundRestored=false;}

    @Inject(method="startNextRound(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V",at=@At("TAIL"))
    private static void imba$spreadPreparingSeekers(MinecraftServer server,ServerPlayerEntity starter,CallbackInfo ci){
        if(server==null||currentSeekers.isEmpty())return;var ordered=new ArrayList<ServerPlayerEntity>();
        for(ServerPlayerEntity player:server.getPlayerManager().getPlayerList())if(currentSeekers.contains(player.getUuid()))ordered.add(player);
        for(int i=1;i<ordered.size();i++){ServerPlayerEntity seeker=ordered.get(i);TeleportConfig.ExtraSeekerPoint point=TeleportConfig.getExtraSeekerPrepare(i-1);
            if(point!=null)seeker.teleport(server.getOverworld(),point.pos().x,point.pos().y,point.pos().z,point.yaw(),point.pitch());
            else{Vec3d p=imba$fallbackPreparePos(i);seeker.teleport(server.getOverworld(),p.x,p.y,p.z,GameConfig.LOBBY_YAW,GameConfig.LOBBY_PITCH);}prepareSeekerAnchors.put(seeker.getUuid(),seeker.getPos());}
    }

    @Inject(method="releaseSeekers",at=@At("TAIL"))
    private static void imba$spreadReleasedSeekers(MinecraftServer server,CallbackInfo ci){
        if(server==null||currentRound==null||currentSeekers.size()<2)return;var ordered=new ArrayList<ServerPlayerEntity>();
        for(ServerPlayerEntity player:server.getPlayerManager().getPlayerList())if(currentSeekers.contains(player.getUuid())&&!eliminatedSeekers.contains(player.getUuid()))ordered.add(player);
        for(int i=1;i<ordered.size();i++){double angle=(i-1)*(Math.PI/2.0D);double radius=1.15D+((i-1)/4)*0.8D;Vec3d separated=currentRound.seekerPos.add(Math.cos(angle)*radius,0.0D,Math.sin(angle)*radius);teleport(ordered.get(i),currentRound.worldKey,separated,currentRound.seekerYaw,currentRound.seekerPitch);}
    }

    @Inject(method="damageSeekerHeart",at=@At("TAIL"))
    private static void imba$finishWhenEverySeekerIsOut(ServerPlayerEntity seeker,String message,CallbackInfo ci){if(seeker==null||currentSeekers.isEmpty()||!eliminatedSeekers.containsAll(currentSeekers))return;MinecraftServer server=seeker.getServer();if(server!=null)finishHiderWinByHearts(server);}
    @Inject(method="beginReturn",at=@At("HEAD")) private static void imba$restoreAtRoundEnd(MinecraftServer server,net.minecraft.text.Text message,CallbackInfo ci){imba$restoreOnce(server);}
    @Inject(method="finishReturn",at=@At("HEAD")) private static void imba$restoreOnForcedReset(MinecraftServer server,CallbackInfo ci){imba$restoreOnce(server);}
    @Unique private static void imba$restoreOnce(MinecraftServer server){if(!imba$roundRestored&&server!=null){RoundRestoreConfig.restoreAll(server);imba$roundRestored=true;}}
    @Unique private static Vec3d imba$fallbackPreparePos(int index){int slot=index-1;int ring=slot/8+1;double angle=(slot%8)*(Math.PI/4.0D);double radius=1.35D*ring;return GameConfig.LOBBY_POS.add(Math.cos(angle)*radius,0.0D,Math.sin(angle)*radius);}
}
