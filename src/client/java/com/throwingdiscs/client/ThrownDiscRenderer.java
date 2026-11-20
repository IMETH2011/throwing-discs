package com.throwingdiscs.client;

import com.throwingdiscs.entity.ThrownDiscEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Re-uses vanilla's "flying item" state (it already builds the item model for us) but draws it
 * lying FLAT (parallel to the ground) and spinning like a frisbee, instead of facing the camera.
 */
@Environment(EnvType.CLIENT)
public class ThrownDiscRenderer extends FlyingItemEntityRenderer<ThrownDiscEntity> {
	public ThrownDiscRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(FlyingItemEntityRenderState state, MatrixStack matrices,
			OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
		if (state.itemRenderState.isEmpty()) {
			return;
		}
		matrices.push();
		matrices.translate(0.0f, 0.1f, 0.0f);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.age * 55.0f)); // spin
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));            // lay flat
		matrices.scale(1.6f, 1.6f, 1.6f);
		state.itemRenderState.render(matrices, queue, state.light, OverlayTexture.DEFAULT_UV, state.outlineColor);
		matrices.pop();
	}
}
