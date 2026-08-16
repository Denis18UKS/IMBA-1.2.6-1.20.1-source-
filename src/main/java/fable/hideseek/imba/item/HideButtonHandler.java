package fable.hideseek.imba.item;

import fable.hideseek.imba.config.AttachmentConfig;
import fable.hideseek.imba.config.MaskAutoPositionConfig;
import fable.hideseek.imba.config.MaskBlockConfig;
import fable.hideseek.imba.game.GameMessages;
import fable.hideseek.imba.mask.MaskHitbox;
import fable.hideseek.imba.mask.MaskService;
import fable.hideseek.imba.mask.MaskState;
import fable.hideseek.imba.mask.MaskType;
import fable.hideseek.imba.net.MaskNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class HideButtonHandler {
    private static final int EFFECT_FOREVER = Integer.MAX_VALUE;
    private static final double FULL_BLOCK_EPSILON = 1.0E-7D;
    private static final double SUPPORT_SAMPLE_EPSILON = 0.01D;
    private static final double SUPPORT_XZ_EPSILON = 1.0E-6D;
    private HideButtonHandler() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof HideItem)) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) handle(serverPlayer);
            return ActionResult.SUCCESS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (ModelEquipHandler.isModelItem(stack)) return TypedActionResult.pass(stack);
            if (!(stack.getItem() instanceof HideItem)) return TypedActionResult.pass(stack);
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) handle(serverPlayer);
            return TypedActionResult.success(stack, world.isClient);
        });
    }

    private static void handle(ServerPlayerEntity player) {
        var uuid = player.getUuid();
        MaskState state = MaskState.get(uuid);
        if (state.type == MaskType.NONE) { player.sendMessage(Text.literal("§cСначала надень модель"), true); return; }
        if (state.statue) {
            MaskState.disableStatue(uuid); player.setNoGravity(false); player.setVelocity(0,0,0); player.fallDistance=0f; player.calculateDimensions(); player.removeStatusEffect(StatusEffects.INVISIBILITY); MaskNetworking.refresh(player); GameMessages.send(player, Text.literal("§cВы вышли из маскировки-статуи")); return;
        }

        // Real Minecraft sneaking state: no physical Shift key is assumed.
        player.setSneaking(false); player.setPose(EntityPose.STANDING); player.calculateDimensions();

        double x=player.getX(),y=player.getY(),z=player.getZ();
        SupportInfo support=findSupport(player);
        state.attachedToFrame=false; state.attachmentFacing=Direction.NORTH;

        if(state.type==MaskType.ITEM&&state.item!=null&&MaskService.isSpecialPotion(state.item)){
            BlockPos brewing=findBrewingStand(player);
            if(brewing!=null){Identifier id=Registries.ITEM.getId(state.item);Vec3d off=AttachmentConfig.offsetFor(id);x=brewing.getX()+.5D+off.x;y=brewing.getY()+1D+off.y;z=brewing.getZ()+.5D+off.z;}
            else{x=Math.floor(x)+.5D;y=Math.floor(y);z=Math.floor(z)+.5D;}
        }else if(state.type==MaskType.BLOCK){if(MaskBlockConfig.isFull(state.block)){x=Math.floor(x)+.5D;y=snapFullBlockY(y);z=Math.floor(z)+.5D;}}
        else if(shouldCenterOnBlock(state.type)){x=Math.floor(x)+.5D;y=Math.floor(y);z=Math.floor(z)+.5D;}
        else if(state.type==MaskType.ITEM||state.type==MaskType.WALL_CLIMB){Attachment a=findItemFrameAttachment(player);if(a!=null){x=a.pos.x;y=a.pos.y;z=a.pos.z;state.attachedToFrame=a.frame;state.attachmentFacing=a.facing;}else{x=Math.floor(x)+.5D;y=Math.floor(y);z=Math.floor(z)+.5D;}}

        // Keep the existing pair-autoposition math exactly as before.
        if(state.block!=null&&support!=null&&MaskAutoPositionConfig.hasPair(state.block,support.block)){
            MaskAutoPositionConfig.Offset o=MaskAutoPositionConfig.offsetFor(state.block,support.block);x+=o.xPixels/16.0D;y=support.surfaceY+o.yPixels/16.0D;z+=o.zPixels/16.0D;
        }

        double feetY=player.getBoundingBox().minY;
        boolean inAir=support==null||feetY-support.surfaceY>.30D;
        if(!fable.hideseek.imba.config.AirFixationConfig.isAllowedAt(state,player.getWorld(),x,y,z,inAir)){
            var rule=fable.hideseek.imba.config.AirFixationConfig.effectiveRule(state);
            player.sendMessage(Text.literal(rule.mode()==fable.hideseek.imba.config.AirFixationConfig.Mode.REQUIRE_BLOCK?"§cЗдесь нельзя зафиксироваться: требуется блок §f"+rule.requiredBlock():"§cНельзя зафиксироваться в воздухе"),true);return;
        }

        // Validate the final position after all existing snap/autoposition rules.
        Box finalBox=MaskHitbox.getDimensions(state.type,state.item).getBoxAt(new Vec3d(x,y,z));
        if(!player.getWorld().isSpaceEmpty(player,finalBox)){player.sendMessage(Text.literal("§cВ конечной точке маскировки недостаточно места"),true);return;}

        player.setPosition(x,y,z); player.setVelocity(0,0,0); player.fallDistance=0f; player.setNoGravity(true); MaskState.enableStatue(uuid,x,y,z);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,EFFECT_FOREVER,0,false,false,false)); player.calculateDimensions(); MaskNetworking.refresh(player); GameMessages.send(player,Text.literal("§aВы замаскировались"));
    }

    private static SupportInfo findSupport(ServerPlayerEntity player){double x=player.getX(),feetY=player.getBoundingBox().minY,z=player.getZ();BlockPos feet=BlockPos.ofFloored(x,feetY+SUPPORT_XZ_EPSILON,z);SupportInfo inside=supportAt(player,feet,x,z);if(inside!=null)return inside;BlockPos below=BlockPos.ofFloored(x,feetY-SUPPORT_SAMPLE_EPSILON,z);SupportInfo b=supportAt(player,below,x,z);if(b!=null)return b;for(int i=1;i<=2;i++){SupportInfo s=supportAt(player,below.down(i),x,z);if(s!=null)return s;}return null;}
    private static SupportInfo supportAt(ServerPlayerEntity player,BlockPos pos,double wx,double wz){BlockState state=player.getWorld().getBlockState(pos);if(state.isAir())return null;VoxelShape shape=state.getCollisionShape(player.getWorld(),pos);if(shape.isEmpty())return null;double lx=wx-pos.getX(),lz=wz-pos.getZ(),top=Double.NEGATIVE_INFINITY;for(Box box:shape.getBoundingBoxes())if(lx>=box.minX-SUPPORT_XZ_EPSILON&&lx<=box.maxX+SUPPORT_XZ_EPSILON&&lz>=box.minZ-SUPPORT_XZ_EPSILON&&lz<=box.maxZ+SUPPORT_XZ_EPSILON)top=Math.max(top,box.maxY);if(!Double.isFinite(top))top=shape.getMax(Direction.Axis.Y);return new SupportInfo(state.getBlock(),pos,pos.getY()+top);}
    private static double snapFullBlockY(double y){return Math.ceil(y-FULL_BLOCK_EPSILON);}
    private record SupportInfo(Block block,BlockPos pos,double surfaceY){}
    private record Attachment(Vec3d pos,Direction facing,boolean frame){}
    private static Attachment findItemFrameAttachment(ServerPlayerEntity player){Vec3d start=player.getCameraPosVec(1f),end=start.add(player.getRotationVec(1f).multiply(4.5D));Box box=player.getBoundingBox().stretch(end.subtract(start)).expand(1D);ItemFrameEntity best=null;double dist=Double.MAX_VALUE;for(ItemFrameEntity frame:player.getWorld().getEntitiesByClass(ItemFrameEntity.class,box,e->true)){var hit=frame.getBoundingBox().expand(.15D).raycast(start,end);if(hit.isEmpty())continue;double d=hit.get().squaredDistanceTo(start);if(d<dist){dist=d;best=frame;}}return best==null?null:new Attachment(best.getPos(),best.getHorizontalFacing(),true);}
    private static BlockPos findBrewingStand(ServerPlayerEntity player){BlockPos feet=player.getBlockPos();if(player.getWorld().getBlockState(feet).isOf(net.minecraft.block.Blocks.BREWING_STAND))return feet;BlockPos below=feet.down();return player.getWorld().getBlockState(below).isOf(net.minecraft.block.Blocks.BREWING_STAND)?below:null;}
    private static boolean shouldCenterOnBlock(MaskType type){return type==MaskType.BLOCK||type==MaskType.DOOR||type==MaskType.PORTAL||type==MaskType.LADDER_REVERSED||type==MaskType.BUTTON||type==MaskType.SCULK_VEIN||type==MaskType.LANTERN||type==MaskType.STEM;}
}
