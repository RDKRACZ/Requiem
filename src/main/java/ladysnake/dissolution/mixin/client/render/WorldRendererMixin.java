package ladysnake.dissolution.mixin.client.render;

import ladysnake.dissolution.api.v1.DissolutionPlayer;
import ladysnake.dissolution.api.v1.possession.Possessable;
import ladysnake.dissolution.mixin.client.MinecraftClientAccessorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow @Final private MinecraftClient client;

    /**
     * This sets the possessed entity as the camera in the entity rendering section, to skip rendering that
     * entity completely.
     */
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void preRenderEntities(Entity camera, VisibleRegion frustum, float tickDelta, CallbackInfo info) {
        if (camera instanceof DissolutionPlayer) {
            Possessable possessed = ((DissolutionPlayer) camera).getPossessionComponent().getPossessedEntity();
            if (possessed != null) {
                ((MinecraftClientAccessorMixin)MinecraftClient.getInstance()).setCameraEntityDirect((Entity) possessed);
            }
        }
    }

    /**
     * Reverts the change made in {@link #preRenderEntities(Entity, VisibleRegion, float, CallbackInfo)}
     */
    @Inject(method = "renderEntities", at = @At("RETURN"))
    private void postRenderEntities(Entity camera, VisibleRegion frustum, float tickDelta, CallbackInfo info) {
        if (camera instanceof DissolutionPlayer) {
            Possessable possessed = ((DissolutionPlayer) camera).getPossessionComponent().getPossessedEntity();
            if (possessed == client.getCameraEntity()) {
                ((MinecraftClientAccessorMixin)MinecraftClient.getInstance()).setCameraEntityDirect(camera);
            }
        }
    }
}
