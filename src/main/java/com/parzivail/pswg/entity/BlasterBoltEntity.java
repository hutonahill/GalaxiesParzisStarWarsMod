package com.parzivail.pswg.entity;

import com.parzivail.pswg.client.event.WorldEvent;
import com.parzivail.pswg.client.sound.SoundHelper;
import com.parzivail.pswg.container.SwgPackets;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlasterBoltEntity extends ThrownEntity
{
	private static final TrackedData<Integer> LIFE = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> HUE = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.FLOAT);

	public BlasterBoltEntity(EntityType<? extends BlasterBoltEntity> type, World world)
	{
		super(type, world);
	}

	public BlasterBoltEntity(EntityType<? extends BlasterBoltEntity> type, LivingEntity owner, World world)
	{
		super(type, owner, world);
	}

	protected boolean shouldCreateScorch()
	{
		return true;
	}

	public void setRange(float range)
	{
		var ticksToLive = (int)(range / getVelocity().length());
		setLife(ticksToLive);
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet)
	{
		super.onSpawnPacket(packet);
		SoundHelper.playBlasterBoltHissSound(this);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound tag)
	{
		super.writeCustomDataToNbt(tag);
		tag.putInt("life", getLife());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound tag)
	{
		super.readCustomDataFromNbt(tag);
		setLife(tag.getInt("life"));
	}

	@Override
	public boolean hasNoGravity()
	{
		return true;
	}

	@Override
	protected void initDataTracker()
	{
		dataTracker.startTracking(LIFE, 0);
		dataTracker.startTracking(HUE, 0.0f);
	}

	private int getLife()
	{
		return dataTracker.get(LIFE);
	}

	private void setLife(int life)
	{
		dataTracker.set(LIFE, life);
	}

	public float getHue()
	{
		return dataTracker.get(HUE);
	}

	public void setHue(float hue)
	{
		dataTracker.set(HUE, hue);
	}

	@Override
	public void tick()
	{
		final var life = getLife() - 1;
		setLife(life);

		if (life <= 0)
		{
			this.discard();
			return;
		}

		var forward = getVelocity().normalize();
		setYaw(-(float)Math.atan2(forward.x, forward.z));
		setPitch((float)Math.asin(forward.y));

		super.tick();
	}

	protected void onCollision(HitResult hitResult)
	{
		super.onCollision(hitResult);

		if (!this.world.isClient && shouldCreateScorch())
		{
			if (hitResult.getType() == HitResult.Type.BLOCK)
			{
				var blockHit = (BlockHitResult)hitResult;

				var incident = this.getVelocity().normalize();
				var normal = new Vec3d(blockHit.getSide().getUnitVector());

				var pos = hitResult.getPos();

				var passedData = WorldEvent.createBuffer(WorldEvent.BLASTER_BOLT_HIT);
				passedData.writeDouble(pos.x);
				passedData.writeDouble(pos.y);
				passedData.writeDouble(pos.z);
				passedData.writeDouble(incident.x);
				passedData.writeDouble(incident.y);
				passedData.writeDouble(incident.z);
				passedData.writeDouble(normal.x);
				passedData.writeDouble(normal.y);
				passedData.writeDouble(normal.z);

				for (var trackingPlayer : PlayerLookup.tracking((ServerWorld)world, blockHit.getBlockPos()))
					ServerPlayNetworking.send(trackingPlayer, SwgPackets.S2C.PacketWorldEvent, passedData);
			}
		}

		this.discard();
	}
}
