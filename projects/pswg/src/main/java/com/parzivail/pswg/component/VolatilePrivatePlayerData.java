package com.parzivail.pswg.component;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class VolatilePrivatePlayerData implements ComponentV3, AutoSyncedComponent
{
	private final PlayerEntity provider;

	private static final int CREDITS_SYNCOP = 1;
	private int credits;

	public VolatilePrivatePlayerData(PlayerEntity provider)
	{
		this.provider = provider;
	}

	public int getCredits()
	{
		return credits;
	}

	public void setCredits(int credits)
	{
		this.credits = credits;
		PlayerData.VOLATILE_PRIVATE.sync(provider, (buf, recipient) -> writeSyncPacket(buf, recipient, CREDITS_SYNCOP));
	}

	@Override
	public void readFromNbt(NbtCompound tag)
	{
		credits = tag.getInt("credits");
	}

	@Override
	public void writeToNbt(NbtCompound tag)
	{
		tag.putInt("credits", credits);
	}

	/**
	 * See comment on {@link PersistentPublicPlayerData#syncAll}
	 */
	public void syncAll()
	{
		PlayerData.VOLATILE_PRIVATE.sync(provider);
	}

	@Override
	public void writeSyncPacket(PacketByteBuf buf, ServerPlayerEntity recipient)
	{
		writeSyncPacket(buf, recipient, 0);
	}

	public void writeSyncPacket(PacketByteBuf buf, ServerPlayerEntity recipient, int syncOp)
	{
		buf.writeInt(syncOp);

		switch (syncOp)
		{
			case 0 -> { // Full sync
				var tag = new NbtCompound();
				writeToNbt(tag);
				buf.writeNbt(tag);
			}
			case CREDITS_SYNCOP -> buf.writeInt(credits);
		}
	}

	@Override
	public void applySyncPacket(PacketByteBuf buf)
	{
		var syncOp = buf.readInt();

		// It seems like a bad idea to read arbitrary fields
		// on command from a packet, but this packet will only
		// be used from server->client, so the server will
		// always have the correct data. The client should
		// never do anything other than visual changes based
		// on this data

		switch (syncOp)
		{
			case 0 -> { // Full sync
				var tag = buf.readNbt();
				if (tag != null)
					this.readFromNbt(tag);
			}
			case CREDITS_SYNCOP -> credits = buf.readInt();
		}
	}

	@Override
	public boolean shouldSyncWith(ServerPlayerEntity player)
	{
		// Sync only to the owner
		return player == provider;
	}
}
