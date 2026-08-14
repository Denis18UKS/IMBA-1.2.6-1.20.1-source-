package fable.hideseek.imba.mixin;

import fable.hideseek.imba.config.AirFixationConfig;
import fable.hideseek.imba.item.HideButtonHandler;
import fable.hideseek.imba.mask.MaskHitbox;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HideButtonHandler.class)
public abstract class HideButtonSafetyMixin {
    private static final double MAX_AIR_GAP=.30D;
    @Inject(method="handle",at=@At("HEAD"),cancellable=true)
    private static void imba$validateFixation(ServerPlayerEntity player,CallbackInfo ci){MaskState state=MaskState.get(player.getUuid());if(state==null||state.type==MaskType.NONE||state.statue)return;double surface=imba$findSurfaceY(player),feet=player.getBoundingBox().minY;boolean inAir=!Double.isFinite(surface)||feet-surface>MAX_AIR_GAP;if(inAir){if(!AirFixationConfig.isAllowed(state)){player.sendMessage(Text.literal("§cНельзя зафиксироваться в воздухе"),true);ci.cancel();}return;}if(state.block!=null){double x=Math.floor(player.getX())+.5D,z=Math.floor(player.getZ())+.5D;Box target=MaskHitbox.getDimensions(state.type,state.item).getBoxAt(new Vec3d(x,surface,z));if(!player.getWorld().isSpaceEmpty(player,target)){player.sendMessage(Text.literal("§cЗдесь недостаточно места для фиксации маскировки"),true);ci.cancel();}}}
    @Redirect(method="handle",at=@At(value="INVOKE",target="Lnet/minecraft/server/network/ServerPlayerEntity;requestTeleport(DDD)V"))
    private static void imba$anchorWithoutVanillaTeleport(ServerPlayerEntity player,double x,double y,double z){player.setPosition(x,y,z);}
    @Unique private static double imba$findSurfaceY(ServerPlayerEntity player){double x=player.getX(),z=player.getZ(),feet=player.getBoundingBox().minY;for(int depth=0;depth<=1;depth++){BlockPos pos=BlockPos.ofFloored(x,feet-.01D-depth,z);BlockState state=player.getWorld().getBlockState(pos);VoxelShape shape=state.getCollisionShape(player.getWorld(),pos);if(shape.isEmpty())continue;double lx=x-pos.getX(),lz=z-pos.getZ(),top=Double.NEGATIVE_INFINITY;for(Box box:shape.getBoundingBoxes())if(lx>=box.minX-1e-6&&lx<=box.maxX+1e-6&&lz>=box.minZ-1e-6&&lz<=box.maxZ+1e-6)top=Math.max(top,box.maxY);if(!Double.isFinite(top))top=shape.getMax(Direction.Axis.Y);return pos.getY()+top;}return Double.NaN;}
}
