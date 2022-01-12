package com.parzivail.pswg.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.parzivail.pswg.Resources;
import com.parzivail.pswg.client.render.item.BlasterItemRenderer;
import com.parzivail.pswg.client.screen.widget.LocalTextureButtonWidget;
import com.parzivail.pswg.client.screen.widget.SimpleTooltipSupplier;
import com.parzivail.pswg.item.blaster.BlasterItem;
import com.parzivail.pswg.item.blaster.data.BlasterAttachmentDescriptor;
import com.parzivail.pswg.item.blaster.data.BlasterTag;
import com.parzivail.pswg.screen.BlasterWorkbenchScreenHandler;
import com.parzivail.util.math.MathUtil;
import com.parzivail.util.math.Matrix4fUtil;
import com.parzivail.util.math.MatrixStackUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec2f;

import java.util.List;

@Environment(EnvType.CLIENT)
public class BlasterWorkbenchScreen extends HandledScreen<BlasterWorkbenchScreenHandler> implements ScreenHandlerListener
{
	private static final Identifier TEXTURE = Resources.id("textures/gui/container/blaster_workbench.png");
	private static final int numVisibleAttachmentRows = 3;
	private static final float scrollThumbHeight = 15f;
	private static final float scrollThumbHalfHeight = scrollThumbHeight / 2;

	private ItemStack blaster = ItemStack.EMPTY;

	private Vec2f blasterViewportRotation = new Vec2f(0, 0);
	private boolean isDraggingScrollThumb = false;
	private boolean isDraggingBlasterViewport = false;

	private float scrollPosition = 0;

	private int numAttachments = 10;

	public BlasterWorkbenchScreen(BlasterWorkbenchScreenHandler handler, PlayerInventory inventory, Text title)
	{
		super(handler, inventory, title);
		backgroundWidth = 176;
		backgroundHeight = 256;

		this.titleY -= 1;
	}

	protected void init()
	{
		super.init();

		this.playerInventoryTitleX = 7;
		this.playerInventoryTitleY = this.backgroundHeight - 92;
		this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;

		//		var passedData = new PacketByteBuf(Unpooled.buffer());
		//		passedData.writeNbt(getBlasterTag().toTag());
		//		ClientPlayNetworking.send(SwgPackets.C2S.PacketBlasterWorkbenchApply, passedData);

		this.addDrawableChild(new LocalTextureButtonWidget(x + 51, y + 124, 22, 12, 178, 3, 178, 17, 256, 256, this::onBuildClicked, new SimpleTooltipSupplier(this, this::getBuildTooltip), LiteralText.EMPTY));

		this.addDrawableChild(new LocalTextureButtonWidget(x + 76, y + 124, 22, 12, 203, 3, 203, 17, this::onCancelClicked));

		this.handler.addListener(this);
	}

	private List<? extends OrderedText> getBuildTooltip()
	{
		return null;
	}

	private void onBuildClicked(ButtonWidget sender)
	{

	}

	private void onCancelClicked(ButtonWidget sender)
	{

	}

	private boolean attachmentListContains(double mouseX, double mouseY)
	{
		return MathUtil.rectContains(x + 51, y + 69, 110, 53, mouseX, mouseY) && canScroll();
	}

	private boolean scrollbarContains(double mouseX, double mouseY)
	{
		return MathUtil.rectContains(x + 148, y + 70, 12, 51, mouseX, mouseY) && canScroll();
	}

	private boolean blasterViewportContains(double mouseX, double mouseY)
	{
		return MathUtil.rectContains(x + 52, y + 15, 108, 50, mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount)
	{
		if (attachmentListContains(mouseX, mouseY))
		{
			var i = numAttachments - numVisibleAttachmentRows;
			this.scrollPosition = MathHelper.clamp((float)(this.scrollPosition - amount / i), 0, 1);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
	{
		if (this.isDraggingScrollThumb)
		{
			var trackTop = y + 70;
			var trackBottom = trackTop + 51;
			this.scrollPosition = MathHelper.clamp((float)(mouseY - trackTop - scrollThumbHalfHeight) / (trackBottom - trackTop - scrollThumbHeight), 0, 1);
			return true;
		}
		else
		{
			if (isDraggingBlasterViewport)
				blasterViewportRotation = new Vec2f(blasterViewportRotation.x + (float)deltaX, MathHelper.clamp(blasterViewportRotation.y + (float)deltaY, -20, 20));

			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if (button == 0)
		{
			if (!isDraggingBlasterViewport && blasterViewportContains(mouseX, mouseY))
			{

				isDraggingBlasterViewport = true;
				return true;
			}
			else if (this.scrollbarContains(mouseX, mouseY))
			{
				this.isDraggingScrollThumb = true;
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if (button == 0)
		{
			this.isDraggingScrollThumb = false;
			this.isDraggingBlasterViewport = false;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	private BlasterTag getBlasterTag()
	{
		return new BlasterTag(blaster.getOrCreateNbt());
	}

	public void removed()
	{
		super.removed();
		this.handler.removeListener(this);
	}

	private boolean canScroll()
	{
		return numAttachments > 3;
	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
	{
		this.renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(matrices, mouseX, mouseY);

		var minecraft = MinecraftClient.getInstance();

		if (blaster.getItem() instanceof BlasterItem)
		{
			var bd = BlasterItem.getBlasterDescriptor(MinecraftClient.getInstance().world, blaster);
			var bt = new BlasterTag(blaster.getOrCreateNbt());
			var model = BlasterItem.getBlasterModel(blaster.getOrCreateNbt());

			matrices.push();

			matrices.translate(x + 105, y + 45, 10);

			// TODO: scale based on bounds
			MatrixStackUtil.scalePos(matrices, -60, 60, 1);

			matrices.multiply(new Quaternion(180 - blasterViewportRotation.y, 0, 0, true));
			matrices.multiply(new Quaternion(0, 90 + blasterViewportRotation.x, 0, true));

			matrices.translate(0, 0, 0.22f);

			DiffuseLighting.enableForLevel(Matrix4fUtil.IDENTITY);

			var immediate = minecraft.getBufferBuilders().getEntityVertexConsumers();
			BlasterItemRenderer.INSTANCE.render(blaster, ModelTransformation.Mode.NONE, false, matrices, immediate, 0xf000f0, OverlayTexture.DEFAULT_UV, null);
			immediate.draw();

			DiffuseLighting.enableGuiDepthLighting();

			matrices.pop();

			RenderSystem.setShaderTexture(0, TEXTURE);

			// damage
			drawStackedStatBar(matrices, 0.25f, 0.3f, 20, 142);

			// accuracy
			drawStackedStatBar(matrices, 0.5f, 0.25f, 103, 142);

			// cooling
			drawStackedStatBar(matrices, 0.5f, 0.75f, 20, 155);

			// speed
			drawStackedStatBar(matrices, 1, 0.75f, 103, 155);

			drawAttachmentList(matrices, model, bd.attachmentMap.values().stream().toList());
		}
	}

	private void drawAttachmentList(MatrixStack matrices, Identifier blasterModel, List<BlasterAttachmentDescriptor> attachments)
	{
		drawScrollbar(matrices, canScroll(), scrollPosition);

		var topRow = Math.max(Math.round(scrollPosition * (float)(numAttachments - numVisibleAttachmentRows)), 0);

		for (var i = 0; i < numVisibleAttachmentRows; i++)
		{
			var rowIdx = topRow + i;
			var attachment = attachments.get(rowIdx);

			var iconU = attachment.icon / 3;
			var iconV = attachment.icon % 3;

			if (rowIdx >= numAttachments)
				drawAttachmentRow(matrices, i, 0, 0, -1, LiteralText.EMPTY);
			else
				drawAttachmentRow(matrices, i, iconU, iconV, 1, BlasterItem.getAttachmentTranslation(blasterModel, attachment));
		}
	}

	private void drawScrollbar(MatrixStack matrices, boolean enabled, float percent)
	{
		if (!enabled)
			percent = 0;

		drawTexture(matrices, x + 148, y + 70 + Math.round(36 * percent), enabled ? 228 : 243, 3, 12, 15, 256, 256);
	}

	private void drawAttachmentRow(MatrixStack matrices, int row, int iconUi, int iconVi, int state, Text attachmentText)
	{
		if (state == -1)
			return;

		RenderSystem.setShaderTexture(0, TEXTURE);
		drawTexture(matrices, x + 68, y + 70 + row * 17, 178, 31 + state * 17, 77, 17, 256, 256);
		drawTexture(matrices, x + 51, y + 70 + row * 17, 178, 85 + state * 17, 17, 17, 256, 256);

		if (state == 0)
			RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
		drawTexture(matrices, x + 52, y + 71 + row * 17, 199 + iconUi * 17, 86 + iconVi * 17, 15, 15, 256, 256);
		RenderSystem.setShaderColor(1, 1, 1, 1);

		this.textRenderer.drawWithShadow(matrices, attachmentText, x + 71, y + 74 + row * 17, state > 0 ? 0xFFFFFF : 0xA0A0A0);
	}

	private void drawStackedStatBar(MatrixStack matrices, float newValue, float oldValue, int targetX, int targetY)
	{
		if (newValue > oldValue)
		{
			drawTexture(matrices, x + targetX, y + targetY, 179, 142, Math.round(67 * newValue), 4, 256, 256);
			drawTexture(matrices, x + targetX, y + targetY, 179, 147, Math.round(67 * oldValue), 4, 256, 256);
		}
		else
		{
			drawTexture(matrices, x + targetX, y + targetY, 179, 147, Math.round(67 * oldValue), 4, 256, 256);
			drawTexture(matrices, x + targetX, y + targetY, 179, 142, Math.round(67 * newValue), 4, 256, 256);
		}
	}

	@Override
	public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack)
	{
		switch (slotId)
		{
			case 0 -> {
				blaster = stack.copy();
				onBlasterChanged();
			}
		}
	}

	private void onBlasterChanged()
	{
		scrollPosition = 0;
		numAttachments = 0;

		if (blaster.getItem() instanceof BlasterItem)
		{
			var bd = BlasterItem.getBlasterDescriptor(MinecraftClient.getInstance().world, blaster);
			numAttachments = bd.attachmentMap.size();
		}
	}

	@Override
	public void onPropertyUpdate(ScreenHandler handler, int property, int value)
	{
	}

	protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY)
	{
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);
		var i = (this.width - this.backgroundWidth) / 2;
		var j = (this.height - this.backgroundHeight) / 2;
		this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
	}
}
