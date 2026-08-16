package fable.hideseek.imba.mask;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.List;

/** Shared server/client collision geometry for static disguises. */
public final class MaskCollisionShapes {
    private MaskCollisionShapes() {}
    public static List<Box> create(MaskState state){return state==null?List.of():create(state.type,state.block,state.rotation,state.doorOpen,state.anchorX,state.anchorY,state.anchorZ);}
    public static List<Box> create(MaskType type,net.minecraft.block.Block block,float rotation,boolean doorOpen,double x,double y,double z){
        if(type==MaskType.DOOR&&block instanceof DoorBlock door){
            if(!doorOpen)return List.of(new Box(x-.5,y,z-.5,x+.5,y+2,z+.5));
            Direction facing=Direction.fromHorizontal(Math.floorMod(Math.round(rotation/90f),4));
            BlockState lower=door.getDefaultState().with(DoorBlock.HALF,DoubleBlockHalf.LOWER).with(DoorBlock.FACING,facing).with(DoorBlock.OPEN,true).with(DoorBlock.HINGE,DoorHinge.LEFT);
            BlockState upper=lower.with(DoorBlock.HALF,DoubleBlockHalf.UPPER);
            List<Box> result=new ArrayList<>();append(result,lower,x,y,z);append(result,upper,x,y+1,z);return result;
        }
        if(!MaskService.hasPhysicalCollision(type,block))return List.of();
        BlockState source=block.getDefaultState(); List<Box> result=new ArrayList<>();append(result,source,x,y,z);return result;
    }
    private static void append(List<Box> target,BlockState state,double x,double y,double z){
        for(Box local:state.getCollisionShape(EmptyBlockView.INSTANCE,BlockPos.ORIGIN).getBoundingBoxes())target.add(local.offset(x-.5,y,z-.5));
    }
}
