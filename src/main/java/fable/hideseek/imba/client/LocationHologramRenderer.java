package fable.hideseek.imba.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Fixed-orientation, depth-tested location photos. No billboard and no see-through layer. */
public final class LocationHologramRenderer {
    private LocationHologramRenderer(){}
    public static void register(){
        WorldRenderEvents.AFTER_ENTITIES.register(ctx->{
            MinecraftClient client=MinecraftClient.getInstance(); if(client.world==null||client.player==null||ctx.matrixStack()==null||ctx.consumers()==null)return;
            String worldId=client.world.getRegistryKey().getValue().toString(); var camera=client.gameRenderer.getCamera().getPos();
            for(var p:HologramClientData.snapshot()){
                if(!worldId.equals(p.world()))continue;
                MatrixStack m=ctx.matrixStack();m.push();m.translate(p.x()-camera.x,p.y()-camera.y,p.z()-camera.z);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-p.yaw()));
                float w=2.4f*p.scale(),h=1.35f*p.scale();int light=(p.light()<<4)|(p.light()<<20);Identifier texture=ClientLocationPhotos.texture(p.location());
                VertexConsumer v=ctx.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
                quad(v,m,-w/2,-h/2,w/2,h/2,light);
                String title=PanelData.locationName(p.location()); if(title!=null&&!title.isBlank()){
                    m.push();m.translate(0,-h/2-.12f,.012f);float ts=.02f*p.scale();m.scale(ts,-ts,ts);float tw=client.textRenderer.getWidth(title);client.textRenderer.draw(title,-tw/2,0,0xFFFFFFFF,false,m.peek().getPositionMatrix(),ctx.consumers(),TextRenderer.TextLayerType.NORMAL,0,light);m.pop();
                }
                m.pop();
            }
        });
    }
    private static void quad(VertexConsumer v,MatrixStack m,float l,float b,float r,float t,int light){var e=m.peek();vertex(v,e,l,b,0,0,1,light);vertex(v,e,r,b,0,1,1,light);vertex(v,e,r,t,0,1,0,light);vertex(v,e,l,t,0,0,0,light);}
    private static void vertex(VertexConsumer v,MatrixStack.Entry e,float x,float y,float z,float u,float vv,int light){v.vertex(e.getPositionMatrix(),x,y,z).color(255,255,255,255).texture(u,vv).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e.getNormalMatrix(),0,0,1).next();}
}
