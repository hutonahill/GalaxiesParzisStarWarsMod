package com.parzivail.pswg.item.blaster;

import com.parzivail.pswg.Client;
import com.parzivail.pswg.Resources;
import com.parzivail.pswg.access.util.Matrix4fAccessUtil;
import com.parzivail.pswg.container.SwgSounds;
import com.parzivail.pswg.data.SwgBlasterManager;
import com.parzivail.pswg.item.blaster.data.BlasterDescriptor;
import com.parzivail.pswg.item.blaster.data.BlasterFiringMode;
import com.parzivail.pswg.item.blaster.data.BlasterPowerPack;
import com.parzivail.pswg.item.blaster.data.BlasterTag;
import com.parzivail.pswg.util.BlasterUtil;
import com.parzivail.util.item.ICustomVisualItemEquality;
import com.parzivail.util.item.IDefaultNbtProvider;
import com.parzivail.util.item.ILeftClickConsumer;
import com.parzivail.util.item.IZoomingItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.world.World;

public class BlasterItem extends Item implements ILeftClickConsumer, ICustomVisualItemEquality, IZoomingItem, IDefaultNbtProvider
{
	public BlasterItem(Settings settings)
	{
		super(settings);
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner)
	{
		return false;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand)
	{
		final var stack = player.getStackInHand(hand);

		if (hand != Hand.MAIN_HAND)
			return TypedActionResult.pass(stack);

		if (!world.isClient)
			BlasterTag.mutate(stack, BlasterTag::toggleAds);

		return TypedActionResult.fail(stack);
	}

	@Override
	public boolean hasGlint(ItemStack stack)
	{
		return true;
	}

	public static Identifier getBlasterModel(ItemStack stack)
	{
		var tag = stack.getOrCreateTag();

		var blasterModel = tag.getString("model");
		if (blasterModel.isEmpty())
			blasterModel = "pswg:a280";

		return new Identifier(blasterModel);
	}

	public static void nextFireMode(World world, ServerPlayerEntity player, ItemStack stack)
	{
		var bt = new BlasterTag(stack.getOrCreateTag());
		var bd = getBlasterDescriptor(world, stack);
		var modes = bd.firingModes;
		BlasterFiringMode currentMode;

		currentMode = bt.getFiringMode();
		var currentModeIdx = modes.indexOf(currentMode);

		currentModeIdx++;
		currentModeIdx %= modes.size();

		bt.setFiringMode(currentMode = modes.get(currentModeIdx));

		//			world.playSound(null, player.getBlockPos(), SwgSounds.Lightsaber.START_CLASSIC, SoundCategory.PLAYERS, 1f, 1f);

		bt.serializeAsSubtag(stack);

		player.sendMessage(new TranslatableText(Resources.dotModId("msg", "blaster_mode_changed"), new TranslatableText(currentMode.getTranslation())), true);
	}

	public static BlasterDescriptor getBlasterDescriptor(World world, ItemStack stack)
	{
		var blasterManager = SwgBlasterManager.get(world);
		return blasterManager.getData(getBlasterModel(stack));
	}

	public static BlasterDescriptor getBlasterDescriptorClient(ItemStack stack)
	{
		var blasterManager = Client.ResourceManagers.getBlasterManager();
		return blasterManager.getData(getBlasterModel(stack));
	}

	@Override
	public boolean allowRepeatedLeftHold(World world, PlayerEntity player, Hand mainHand)
	{
		final var stack = player.getStackInHand(mainHand);
		var bt = new BlasterTag(stack.getOrCreateTag());
		var bd = getBlasterDescriptor(world, stack);

		return bd.firingModes.contains(BlasterFiringMode.AUTOMATIC) && bt.getFiringMode() == BlasterFiringMode.AUTOMATIC;
	}

	@Override
	public TypedActionResult<ItemStack> useLeft(World world, PlayerEntity player, Hand hand)
	{
		final var stack = player.getStackInHand(hand);

		var bd = getBlasterDescriptor(world, stack);
		var bt = new BlasterTag(stack.getOrCreateTag());

		if (!bt.isReady())
			return TypedActionResult.fail(stack);

		bt.shotTimer = bd.automaticRepeatTime;

		if (bt.isOverheatCooling())
		{
			if (world.isClient || !bt.canBypassOverheat)
			{
				bt.serializeAsSubtag(stack);
				return TypedActionResult.fail(stack);
			}

			var profile = bd.cooling;

			final var cooldownTime = bt.overheatTimer / (float)bd.heat.capacity;

			final var primaryBypassStart = profile.primaryBypassTime - profile.primaryBypassTolerance;
			final var primaryBypassEnd = profile.primaryBypassTime + profile.primaryBypassTolerance;
			final var secondaryBypassStart = profile.secondaryBypassTime - profile.secondaryBypassTolerance;
			final var secondaryBypassEnd = profile.secondaryBypassTime + profile.secondaryBypassTolerance;

			var result = TypedActionResult.fail(stack);

			if (profile.primaryBypassTolerance > 0 && cooldownTime >= primaryBypassStart && cooldownTime <= primaryBypassEnd)
			{
				// TODO: primary bypass sound
				bt.overheatTimer = 0;

				result = TypedActionResult.success(stack);
			}
			else if (profile.secondaryBypassTolerance > 0 && cooldownTime >= secondaryBypassStart && cooldownTime <= secondaryBypassEnd)
			{
				// TODO: secondary bypass sound
				bt.overheatTimer = 0;

				result = TypedActionResult.success(stack);
			}
			else
			{
				// TODO: failed bypass sound
				bt.canBypassOverheat = false;
			}

			bt.serializeAsSubtag(stack);

			return result;
		}

		if (!player.isCreative())
		{
			if (bt.shotsRemaining <= 0)
			{
				var nextPack = getAnotherPack(player);

				if (nextPack == null)
				{
					if (!world.isClient)
					{
						world.playSound(null, player.getBlockPos(), SwgSounds.Blaster.DRYFIRE, SoundCategory.PLAYERS, 1f, 1f);
					}

					bt.serializeAsSubtag(stack);
					return TypedActionResult.fail(stack);
				}
				else if (!world.isClient)
				{
					bt.shotsRemaining = nextPack.getRight().numShots();
					player.getInventory().removeStack(nextPack.getLeft(), 1);
					world.playSound(null, player.getBlockPos(), SwgSounds.Blaster.RELOAD, SoundCategory.PLAYERS, 1f, 1f);
				}
			}
		}

		bt.passiveCooldownTimer = bd.heat.passiveCooldownDelay;
		bt.heat += bd.heat.perRound;
		bt.shotsRemaining--;

		if (bt.heat > bd.heat.capacity)
		{
			// TODO: overheat sound
			bt.overheatTimer = bd.heat.capacity + bd.heat.overheatPenalty;
			bt.canBypassOverheat = true;
			bt.heat = 0;
		}

		if (!world.isClient)
		{
			var m = new Matrix4f();
			Matrix4fAccessUtil.loadIdentity(m);

			Matrix4fAccessUtil.multiply(m, new Quaternion(0, -player.getYaw(), 0, true));
			Matrix4fAccessUtil.multiply(m, new Quaternion(player.getPitch(), 0, 0, true));

			var hS = (world.random.nextFloat() * 2 - 1) * bd.spread.horizontal;
			var vS = (world.random.nextFloat() * 2 - 1) * bd.spread.vertical;

			// TODO: stats customization
			float hSR = 1; // - bd.getBarrel().getHorizontalSpreadReduction();
			float vSR = 1; // - bd.getBarrel().getVerticalSpreadReduction();

			Matrix4fAccessUtil.multiply(m, new Quaternion(0, hS * hSR, 0, true));
			Matrix4fAccessUtil.multiply(m, new Quaternion(vS * vSR, 0, 0, true));

			var fromDir = Matrix4fAccessUtil.transform(com.parzivail.util.math.MathUtil.POSZ, m);

			var range = bd.range;
			var damage = bd.damage;

			switch (bt.getFiringMode())
			{
				case SEMI_AUTOMATIC:
				case BURST:
				case AUTOMATIC:
					world.playSound(null, player.getBlockPos(), SwgSounds.getOrDefault(getSound(bd.id), SwgSounds.Blaster.FIRE_A280), SoundCategory.PLAYERS, 1 /* 1 - bd.getBarrel().getNoiseReduction() */, 1 + (float)world.random.nextGaussian() / 10);
					BlasterUtil.fireBolt(world, player, fromDir, range, damage, entity -> {
						entity.setProperties(player, player.getPitch() + vS * vSR, player.getYaw() + hS * hSR, 0.0F, 4.0F, 0);
						entity.setPos(player.getX(), player.getEyeY() - entity.getHeight() / 2f, player.getZ());
					});
					break;
				case STUN:
					world.playSound(null, player.getBlockPos(), SwgSounds.Blaster.STUN, SoundCategory.PLAYERS, 1 /* 1 - bd.getBarrel().getNoiseReduction() */, 1 + (float)world.random.nextGaussian() / 10);
					BlasterUtil.fireStun(world, player, fromDir, range * 0.05f, damage, entity -> {
						entity.setProperties(player, player.getPitch() + vS * vSR, player.getYaw() + hS * hSR, 0.0F, 1.0F, 0);
						entity.setPos(player.getX(), player.getEyeY() - entity.getHeight() / 2f, player.getZ());
					});
					break;
				case SLUGTHROWER:
					world.playSound(null, player.getBlockPos(), SwgSounds.getOrDefault(getSound(bd.id), SwgSounds.Blaster.FIRE_A280), SoundCategory.PLAYERS, 1 /* 1 - bd.getBarrel().getNoiseReduction() */, 1 + (float)world.random.nextGaussian() / 10);
					BlasterUtil.fireSlug(world, player, fromDir, range, damage);
					break;
				case ION:
					// TODO: ion bolts
					break;
			}

			bt.serializeAsSubtag(stack);
		}

		return TypedActionResult.success(stack);
	}

	private Identifier getSound(Identifier id)
	{
		return new Identifier(id.getNamespace(), "blaster.fire." + id.getPath());
	}

	@Override
	public String getTranslationKey(ItemStack stack)
	{
		var tag = stack.getOrCreateTag();

		var model = tag.getString("model");
		if (model.isEmpty())
			return super.getTranslationKey(stack);

		var bdId = new Identifier(model);

		return "item." + bdId.getNamespace() + ".blaster_" + bdId.getPath();
	}

	@Override
	public NbtCompound getDefaultTag(ItemConvertible item, int count)
	{
		var tag = new NbtCompound();

		tag.putString("model", Resources.id("a280").toString());

		return tag;
	}

	@Override
	public void appendStacks(ItemGroup group, DefaultedList<ItemStack> stacks)
	{
		if (!this.isIn(group))
			return;

		var manager = Client.ResourceManagers.getBlasterManager();

		for (var entry : manager.getData().entrySet())
			stacks.add(forType(entry.getValue()));
	}

	private ItemStack forType(BlasterDescriptor descriptor)
	{
		var stack = new ItemStack(this);

		stack.getOrCreateTag().putString("model", descriptor.id.toString());

		var bd = getBlasterDescriptorClient(stack);

		BlasterTag.mutate(stack, blasterTag -> {
			if (bd.firingModes.isEmpty())
				blasterTag.setFiringMode(BlasterFiringMode.SEMI_AUTOMATIC);
			else
				blasterTag.setFiringMode(bd.firingModes.get(0));
		});

		return stack;
	}

	private Pair<Integer, BlasterPowerPack> getAnotherPack(PlayerEntity player)
	{
		for (var i = 0; i < player.getInventory().size(); i++)
		{
			var s = player.getInventory().getStack(i);
			var a = BlasterPowerPackItem.getPackType(s);
			if (a == null)
				continue;

			return new Pair<>(i, a);
		}
		return null;
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected)
	{
		var bd = getBlasterDescriptor(world, stack);

		BlasterTag.mutate(stack, blasterTag -> {
			if (blasterTag.overheatTimer > 0)
				blasterTag.overheatTimer -= bd.heat.overheatDrainSpeed;

			if (blasterTag.heat > 0 && blasterTag.passiveCooldownTimer == 0)
				blasterTag.heat -= bd.heat.drainSpeed;

			blasterTag.tick();
		});
	}

	@Override
	public boolean areStacksVisuallyEqual(ItemStack original, ItemStack updated)
	{
		if (!(original.getItem() instanceof BlasterItem) || original.getItem() != updated.getItem())
			return false;

		var bt1 = new BlasterTag(original.getOrCreateTag());
		var bt2 = new BlasterTag(updated.getOrCreateTag());
		return bt1.serialNumber == bt2.serialNumber;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public double getFovMultiplier(ItemStack stack, World world, PlayerEntity entity)
	{
		var bt = new BlasterTag(stack.getOrCreateTag());

		// TODO: blaster variable zoom
		var lerp = bt.getAdsLerp();
		return MathHelper.lerp(lerp, 1, 0.2f);
	}
}
