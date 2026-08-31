package fable.hideseek.imba.mixin.client;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Minimal access to the already resource-pack-resolved sprite pixels.
 * Used only to copy one vanilla Nether Portal animation frame into a private
 * buffer sprite inside the same block atlas; it does not replace the model,
 * render layer or portal texture pipeline.
 */
@Mixin(SpriteContents.class)
public interface SpriteContentsUploadAccessor {
    @Accessor("mipmapLevelsImages")
    NativeImage[] imba$getMipmapLevelsImages();

    @Invoker("upload")
    void imba$uploadFrame(int x, int y, int unpackSkipPixels, int unpackSkipRows, NativeImage[] images);
}
