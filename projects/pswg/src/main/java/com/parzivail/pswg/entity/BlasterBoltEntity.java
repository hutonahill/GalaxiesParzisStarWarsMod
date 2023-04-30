package com.parzivail.pswg.entity;

import com.parzivail.pswg.client.event.WorldEvent;
import com.parzivail.pswg.client.sound.SoundHelper;
import com.parzivail.pswg.container.SwgPackets;
import com.parzivail.pswg.container.SwgParticles;
import com.parzivail.pswg.container.SwgTags;
import com.parzivail.pswg.features.lightsabers.LightsaberItem;
import com.parzivail.util.data.PacketByteBufHelper;
import com.parzivail.util.entity.IPrecisionSpawnEntity;
import com.parzivail.util.entity.IPrecisionVelocityEntity;
import com.parzivail.util.math.MathUtil;
import com.parzivail.util.network.PreciseEntitySpawnS2CPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BlasterBoltEntity extends ThrownEntity implements IPrecisionVelocityEntity, IPrecisionSpawnEntity
{
	private static final TrackedData<Integer> LIFE = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> COLOR = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> LENGTH = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> RADIUS = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Boolean> SMOLDERING = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Byte> ARM = DataTracker.registerData(BlasterBoltEntity.class, TrackedDataHandlerRegistry.BYTE);

	private boolean ignoreWater;

	public BlasterBoltEntity(EntityType<? extends BlasterBoltEntity> type, World world)
	{
		super(type, world);
	}

	public BlasterBoltEntity(EntityType<? extends BlasterBoltEntity> type, LivingEntity owner, World world, boolean ignoreWater)
	{
		super(type, owner, world);
		this.ignoreWater = ignoreWater;
	}

	public void setSourceArm(Arm arm)
	{
		this.dataTracker.set(ARM, (byte)arm.getId());
	}

	public Optional<Arm> getSourceArm()
	{
		var arm = this.dataTracker.get(ARM);
		return deserializeArm(arm);
	}

	@NotNull
	private static Optional<Arm> deserializeArm(byte arm)
	{
		return switch (arm)
		{
			case 0 -> Optional.of(Arm.LEFT);
			case 1 -> Optional.of(Arm.RIGHT);
			default -> Optional.empty();
		};
	}

	protected boolean shouldCreateScorch()
	{
		return true;
	}

	protected boolean shouldDestroyBlocks()
	{
		return true;
	}

	public void setRange(float range)
	{
		var ticksToLive = (int)(range / getVelocity().length());
		setLife(ticksToLive);
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket()
	{
		var entity = this.getOwner();
		return PreciseEntitySpawnS2CPacket.createPacket(SwgPackets.S2C.PreciseEntitySpawn, this, entity == null ? 0 : entity.getId());
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet)
	{
		super.onSpawnPacket(packet);
		SoundHelper.playBlasterBoltHissSound(this);

		if (packet instanceof PreciseEntitySpawnS2CPacket pes)
		{
			this.setVelocity(pes.getVelocity());
			this.readSpawnData(pes.getData());
		}
	}

	@Override
	public void writeSpawnData(NbtCompound tag)
	{
		tag.putInt("life", getLife());
		tag.putBoolean("ignoreWater", ignoreWater);
		tag.putInt("color", getColor());
		tag.putFloat("length", getLength());
		tag.putFloat("radius", getRadius());
		tag.putBoolean("smoldering", isSmoldering());
		tag.putByte("arm", (byte)getSourceArm().orElse(Arm.LEFT).getId());
	}

	@Override
	public void readSpawnData(NbtCompound tag)
	{
		setLife(tag.getInt("life"));
		ignoreWater = tag.getBoolean("ignoreWater");
		setColor(tag.getInt("color"));
		setLength(tag.getFloat("length"));
		setRadius(tag.getFloat("radius"));
		setSmoldering(tag.getBoolean("smoldering"));
		setSourceArm(deserializeArm(tag.getByte("arm")).orElse(Arm.RIGHT));
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound tag)
	{
		super.writeCustomDataToNbt(tag);
		writeSpawnData(tag);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound tag)
	{
		super.readCustomDataFromNbt(tag);
		readSpawnData(tag);
	}

	@Override
	public boolean shouldRender(double distance)
	{
		return true;
	}

	@Override
	public boolean hasNoGravity()
	{
		return true;
	}

	@Override
	protected void initDataTracker()
	{
		dataTracker.startTracking(LIFE, 100);
		dataTracker.startTracking(COLOR, 0);
		dataTracker.startTracking(LENGTH, 1f);
		dataTracker.startTracking(RADIUS, 1f);
		dataTracker.startTracking(SMOLDERING, false);
		dataTracker.startTracking(ARM, (byte)255);
	}

	private int getLife()
	{
		return dataTracker.get(LIFE);
	}

	private void setLife(int life)
	{
		dataTracker.set(LIFE, life);
	}

	public int getColor()
	{
		return dataTracker.get(COLOR);
	}

	public void setColor(int color)
	{
		dataTracker.set(COLOR, color);
	}

	public float getRadius()
	{
		return dataTracker.get(RADIUS);
	}

	public void setRadius(float radius)
	{
		dataTracker.set(RADIUS, radius);
	}

	public float getLength()
	{
		return dataTracker.get(LENGTH);
	}

	public void setLength(float length)
	{
		dataTracker.set(LENGTH, length);
	}

	public boolean isSmoldering()
	{
		return dataTracker.get(SMOLDERING);
	}

	public void setSmoldering(boolean smoldering)
	{
		dataTracker.set(SMOLDERING, smoldering);
	}

	@Override
	public void tick()
	{
		final var life = getLife() - 1;
		setLife(life);

		if (!world.isClient && life <= 0)
		{
			this.discard();
			return;
		}

		if (world.isClient && age > 1 && isSmoldering())
		{
			var vec = getPos();
			var vel = getVelocity();
			var n = 10;
			var dVel = vel.multiply(1f / n);

			for (var i = 0; i < n; i++)
			{
				var dx = 0.01 * world.random.nextGaussian();
				var dy = 0.01 * world.random.nextGaussian();
				var dz = 0.01 * world.random.nextGaussian();

				world.addParticle(SwgParticles.SLUG_TRAIL, vec.x, vec.y, vec.z, dx, dy, dz);

				vec = vec.add(dVel);
			}
		}

		super.tick();
	}

	@Override
	protected void onCollision(HitResult hitResult)
	{
		super.onCollision(hitResult);

		if (hitResult.getType() == HitResult.Type.BLOCK)
		{
			var blockHit = (BlockHitResult)hitResult;

			var blockPos = blockHit.getBlockPos();
			var shouldScorch = true;

			var state = world.getBlockState(blockPos);

			if (state.isIn(SwgTags.Blocks.BLASTER_REFLECT))
			{
				if (deflect(blockHit, state))
					return;
			}

			if (shouldDestroyBlocks())
			{
				if (!this.world.isClient)
				{
					if (state.isIn(SwgTags.Blocks.BLASTER_DESTROY))
					{
						world.breakBlock(blockPos, false, this);
						shouldScorch = false;
					}
					else if (state.isIn(SwgTags.Blocks.BLASTER_EXPLODE))
					{
						world.breakBlock(blockPos, false, this);
						if (state.getBlock() instanceof TntBlock)
						{
							TntEntity tntEntity = new TntEntity(world, (double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5, null);
							tntEntity.setFuse(0);
							world.spawnEntity(tntEntity);
						}
						else
						{
							// TODO: explosion power registry?
							this.world.createExplosion(this, this.getX(), this.getBodyY(0.0625), this.getZ(), 4.0F, World.ExplosionSourceType.BLOCK);
						}

						shouldScorch = false;
					}
				}
			}

			if (shouldCreateScorch() && shouldScorch)
			{
				if (!this.world.isClient)
				{
					if (world.isWater(blockPos) && ignoreWater)
						return;

					var incident = this.getVelocity().normalize();
					var normal = new Vec3d(blockHit.getSide().getUnitVector());

					var pos = hitResult.getPos();

					var passedData = WorldEvent.createBuffer(WorldEvent.BLASTER_BOLT_HIT);
					PacketByteBufHelper.writeVec3d(passedData, pos);
					PacketByteBufHelper.writeVec3d(passedData, incident);
					PacketByteBufHelper.writeVec3d(passedData, normal);

					for (var trackingPlayer : PlayerLookup.tracking((ServerWorld)world, blockHit.getBlockPos()))
						ServerPlayNetworking.send(trackingPlayer, SwgPackets.S2C.WorldEvent, passedData);
				}
			}
		}
		else if (hitResult.getType() == HitResult.Type.ENTITY)
		{
			var entityHit = (EntityHitResult)hitResult;
			var entity = entityHit.getEntity();

			if (entity instanceof LivingEntity le && le.getActiveItem().getItem() instanceof LightsaberItem && le.isUsingItem())
				if (deflect(le))
					return;
		}

		this.discard();
	}

	@Override
	public void remove(RemovalReason reason)
	{
		super.remove(reason);
	}

	protected boolean deflect(LivingEntity entity)
	{
		var speed = this.getVelocity().length();

		var yaw = entity.getHeadYaw();
		var pitch = entity.getPitch();

		float x = -MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
		float y = -MathHelper.sin(pitch * MathHelper.RADIANS_PER_DEGREE);
		float z = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(pitch * MathHelper.RADIANS_PER_DEGREE);

		this.setYaw(yaw);
		this.setPitch(pitch);
		this.setVelocity(x * speed, y * speed, z * speed);

		return true;
	}

	protected boolean deflect(BlockHitResult hit, BlockState state)
	{
		var velocity = this.getVelocity();
		var dir = velocity.normalize();

		var normal = new Vec3d(hit.getSide().getUnitVector());
		var newDir = MathUtil.reflect(dir, normal);

		// TODO: decrease damage on reflection?
		this.setVelocity(newDir.multiply(velocity.length()));

		return true;
	}
}
