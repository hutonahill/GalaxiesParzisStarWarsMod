package com.parzivail.util.client.render;

import com.parzivail.pswg.Client;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;

public class MutableAnimatedModel<T extends Entity> extends SinglePartEntityModel<T>
{
	private final ModelAngleAnimator<T> angleAnimator;

	private ModelPart root;

	public MutableAnimatedModel(ModelAngleAnimator<T> angleAnimator)
	{
		this.angleAnimator = angleAnimator;
	}

	public void setRoot(ModelPart root)
	{
		this.root = root;
	}

	@Override
	public ModelPart getPart()
	{
		return root;
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch)
	{
		angleAnimator.setAngles(this, entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch, Client.getTickDelta());
	}
}
