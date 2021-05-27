package com.parzivail.pswg.client.render.fish;

import com.parzivail.pswg.Resources;
import com.parzivail.pswg.client.model.fish.LaaModel;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class LaaEntityRenderer extends MobEntityRenderer<FishEntity, LaaModel<FishEntity>>
{
	public LaaEntityRenderer(EntityRenderDispatcher entityRenderDispatcher)
	{
		super(entityRenderDispatcher, new LaaModel<>(), 0.5f);
	}

	@Override
	public Identifier getTexture(FishEntity entity)
	{
		return Resources.identifier("textures/entity/fish/laa.png");
	}

	@Override
	protected void setupTransforms(FishEntity entity, MatrixStack matrixStack, float f, float g, float h)
	{
		super.setupTransforms(entity, matrixStack, f, g, h);
		float i = 4.3F * MathHelper.sin(0.6F * f);
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(i));
		if (!entity.isTouchingWater())
		{
			matrixStack.translate(0.10000000149011612D, 0.10000000149011612D, -0.10000000149011612D);
			matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
		}
	}
}
