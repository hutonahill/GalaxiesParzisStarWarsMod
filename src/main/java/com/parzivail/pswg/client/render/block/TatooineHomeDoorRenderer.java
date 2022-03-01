package com.parzivail.pswg.client.render.block;

import com.parzivail.pswg.Resources;
import com.parzivail.pswg.blockentity.TatooineHomeDoorBlockEntity;
import com.parzivail.pswg.client.render.p3d.P3dManager;
import com.parzivail.pswg.container.SwgBlocks;
import com.parzivail.util.block.rotating.RotatingBlock;
import com.parzivail.util.math.ClientMathUtil;
import com.parzivail.util.math.Ease;
import com.parzivail.util.math.Matrix4fUtil;
import com.parzivail.util.math.QuatUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

public class TatooineHomeDoorRenderer implements BlockEntityRenderer<TatooineHomeDoorBlockEntity>
{
	private static final Identifier MODEL = Resources.id("block/tatooine_home_door");
	private static final Identifier TEXTURE_FRAME = Resources.id("textures/model/door/tatooine_home/frame.png");
	private static final Identifier TEXTURE_DOOR = Resources.id("textures/model/door/tatooine_home/door.png");

	public TatooineHomeDoorRenderer(BlockEntityRendererFactory.Context ctx)
	{
	}

	private static Matrix4f transform(TatooineHomeDoorBlockEntity target, String objectName, float tickDelta)
	{
		var m = new Matrix4f();
		m.loadIdentity();

		if (objectName.equals("door"))
		{
			var timer = target.getAnimationTime(tickDelta);

			if (target.isOpening())
				m.multiplyByTranslation(0, 0, 0.845f * Ease.outCubic(1 - timer));
			else
				m.multiplyByTranslation(0, 0, 0.845f * Ease.inCubic(timer));
		}

		m.multiply(Matrix4fUtil.SCALE_10_16THS);
		m.multiply(QuatUtil.ROT_Y_POS90);

		return m;
	}

	private static VertexConsumer provideLayer(VertexConsumerProvider vertexConsumerProvider, TatooineHomeDoorBlockEntity target, String objectName)
	{
		var texture = TEXTURE_DOOR;
		if (objectName.equals("frame"))
			texture = TEXTURE_FRAME;

		return vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutout(texture));
	}

	@Override
	public void render(TatooineHomeDoorBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		var world = blockEntity.getWorld();
		if (world == null)
			return;

		var state = world.getBlockState(blockEntity.getPos());
		if (!state.isOf(SwgBlocks.Door.TatooineHomeBottom))
			return;

		var model = P3dManager.INSTANCE.get(MODEL);
		if (model == null)
			return;

		var rotation = state.get(RotatingBlock.FACING);

		matrices.push();

		matrices.translate(0.5, 0, 0.5);
		matrices.multiply(ClientMathUtil.getRotation(rotation));

		model.render(matrices, vertexConsumers, blockEntity, TatooineHomeDoorRenderer::transform, TatooineHomeDoorRenderer::provideLayer, light, tickDelta);
		matrices.pop();
	}
}
