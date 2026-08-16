package fable.hideseek.imba.mixin.client;
import fable.hideseek.imba.client.ModelTokenRenderer;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.Inject;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The model-token now deliberately uses its normal 16x16 generated item model. */
@Mixin(value=ModelTokenRenderer.class,remap=false) public abstract class ModelTokenRendererDisableMixin {@Inject(method="register",at=@At("HEAD"),cancellable=true,remap=false)private static void imba$useFlatInventoryIcon(CallbackInfo ci){ci.cancel();}}
