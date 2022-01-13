package com.parzivail.pswg.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.parzivail.pswg.Resources;
import com.parzivail.pswg.client.render.item.BlasterItemRenderer;
import com.parzivail.pswg.client.screen.widget.AreaButtonWidget;
import com.parzivail.pswg.client.screen.widget.LocalTextureButtonWidget;
import com.parzivail.pswg.client.screen.widget.SimpleTooltipSupplier;
import com.parzivail.pswg.item.blaster.BlasterItem;
import com.parzivail.pswg.item.blaster.data.BlasterAttachmentDescriptor;
import com.parzivail.pswg.item.blaster.data.BlasterDescriptor;
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
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec2f;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class BlasterWorkbenchScreen extends HandledScreen<BlasterWorkbenchScreenHandler> implements ScreenHandlerListener
{
	public static final String I18N_INCOMPAT_ATTACHMENT = Resources.screen("blaster.incompatible_attachment");

	private static final Identifier TEXTURE = Resources.id("textures/gui/container/blaster_workbench.png");
	private static final int numVisibleAttachmentRows = 3;
	private static final float scrollThumbHeight = 15f;
	private static final float scrollThumbHalfHeight = scrollThumbHeight / 2;

	private static final int ROW_STATE_EMPTY = -1;
	private static final int ROW_STATE_DISABLED = 0;
	private static final int ROW_STATE_NORMAL = 1;
	private static final int ROW_STATE_HOVER = 2;

	private ItemStack blaster = ItemStack.EMPTY;
	private BlasterDescriptor blasterDescriptor = null;
	private Identifier blasterModel = null;

	private Vec2f blasterViewportRotation = new Vec2f(0, 0);
	private boolean isDraggingScrollThumb = false;
	private boolean isDraggingBlasterViewport = false;

	private float scrollPosition = 0;

	private List<BlasterAttachmentDescriptor> attachmentList = new ArrayList<>();

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

		this.addDrawableChild(new AreaButtonWidget(x + 52, y + 70, 93, 17, button -> onRowClicked(0)));
		this.addDrawableChild(new AreaButtonWidget(x + 52, y + 87, 93, 17, button -> onRowClicked(1)));
		this.addDrawableChild(new AreaButtonWidget(x + 52, y + 104, 93, 17, button -> onRowClicked(2)));

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

	private void onRowClicked(int row)
	{
		var topRow = getAttachmentListTopRowIdx();
		var rowIdx = topRow + row;

		if (rowIdx >= attachmentList.size())
			return;

		var attachment = attachmentList.get(rowIdx);

		var bt = getBlasterTag();

		var incompat = getIncompatibleAttachments(bt, attachment);

		// TODO: move this to backend and subtract material cost
		//  for new attachments, and give back for removed ones

		// remove incompatible attachments
		for (var a : incompat)
			bt.attachmentBitmask ^= a.bit;

		// apply new attachment
		bt.attachmentBitmask ^= attachment.bit;

		provideDefaultAttachments(bt);

		bt.serializeAsSubtag(blaster);
	}

	private void provideDefaultAttachments(BlasterTag bt)
	{
		for (var attachment : attachmentList)
		{
			// If no attachment is attached in the bitmask category that this
			// attachment belongs to, revert to the default attachment (which
			// may also be zero, but at least we checked). This only works
			// if the mutex for each attachment is set to the bitfield
			// that spans all attachments. Example:
			//
			// Attachment 1: bit 0b01
			// Attachment 2: bit 0b10
			// Mutex for both:   0b11
			//
			// This also means that the "minimum" attachments in a
			// descriptor should only be set to the attachments
			// that form the default in a required set.

			if ((attachment.mutex & bt.attachmentBitmask) == 0)
				bt.attachmentBitmask |= (blasterDescriptor.attachmentMinimum & attachment.mutex);
		}
	}

	private List<BlasterAttachmentDescriptor> getIncompatibleAttachments(BlasterTag bt, BlasterAttachmentDescriptor query)
	{
		var list = new ArrayList<BlasterAttachmentDescriptor>();
		for (var attachment : attachmentList)
		{
			if (attachment == query)
				continue;

			// check if this attachment is both attached, and conflicts with the query attachment
			if ((bt.attachmentBitmask & attachment.bit) != 0 && (attachment.mutex & query.mutex) != 0)
				list.add(attachment);
		}
		return list;
	}

	private List<Text> getAttachmentError(BlasterTag bt, BlasterAttachmentDescriptor attachment)
	{
		var incompat = getIncompatibleAttachments(bt, attachment);
		if (incompat.isEmpty())
			return null;

		var text = new ArrayList<Text>();
		text.add(new TranslatableText(I18N_INCOMPAT_ATTACHMENT));

		for (var a : incompat)
			text.add(BlasterItem.getAttachmentTranslation(blasterModel, a).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xA0A0A0))));

		return text;
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
			var i = attachmentList.size() - numVisibleAttachmentRows;
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
		return attachmentList.size() > 3;
	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
	{
		this.renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);

		var minecraft = MinecraftClient.getInstance();
		Mutable<List<Text>> tooltip = new MutableObject<>();

		if (blaster.getItem() instanceof BlasterItem)
		{
			var bt = new BlasterTag(blaster.getOrCreateNbt());

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

			drawAttachmentList(matrices, blasterModel, bt, attachmentList, this::getAttachmentError, tooltip::setValue, mouseX, mouseY);
		}

		if (tooltip.getValue() != null)
			this.renderTooltip(matrices, tooltip.getValue(), mouseX, mouseY);

		this.drawMouseoverTooltip(matrices, mouseX, mouseY);
	}

	private void drawAttachmentList(MatrixStack matrices, Identifier blasterModel, BlasterTag bt, List<BlasterAttachmentDescriptor> attachments, BiFunction<BlasterTag, BlasterAttachmentDescriptor, List<Text>> errorProvider, Consumer<List<Text>> tooltipPropogator, double mouseX, double mouseY)
	{
		drawScrollbar(matrices, canScroll(), scrollPosition);

		var topRow = getAttachmentListTopRowIdx();

		for (var i = 0; i < numVisibleAttachmentRows; i++)
		{
			var rowIdx = topRow + i;

			if (rowIdx >= attachmentList.size())
				drawAttachmentRow(matrices, i, 0, 0, ROW_STATE_EMPTY, LiteralText.EMPTY);
			else
			{
				var attachment = attachments.get(rowIdx);
				var rowState = ROW_STATE_NORMAL;

				var hovering = MathUtil.rectContains(x + 52, y + 70 + i * 17, 94, 16, mouseX, mouseY);

				// TODO: check if the attachment is attached, and provide a way to disable it
				if ((bt.attachmentBitmask & attachment.bit) != 0)
					rowState = ROW_STATE_HOVER;
				else
				{
					var validityError = errorProvider.apply(bt, attachment);
					if (validityError != null)
					{
						var oldTexture = RenderSystem.getShaderTexture(0);
						if (hovering)
							tooltipPropogator.accept(validityError);
						RenderSystem.setShaderTexture(0, oldTexture);
					}
				}

				var iconU = attachment.icon / 3;
				var iconV = attachment.icon % 3;

				drawAttachmentRow(matrices, i, iconU, iconV, rowState, BlasterItem.getAttachmentTranslation(blasterModel, attachment));
			}
		}
	}

	private int getAttachmentListTopRowIdx()
	{
		return Math.max(Math.round(scrollPosition * (float)(attachmentList.size() - numVisibleAttachmentRows)), 0);
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

		if (blaster.getItem() instanceof BlasterItem)
		{
			var bd = BlasterItem.getBlasterDescriptor(MinecraftClient.getInstance().world, blaster);
			attachmentList = bd.attachmentMap.values().stream().toList();
			blasterModel = BlasterItem.getBlasterModel(blaster);
			blasterDescriptor = bd;
		}
		else
		{
			attachmentList = new ArrayList<>();
			blasterModel = null;
			blasterDescriptor = null;
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
