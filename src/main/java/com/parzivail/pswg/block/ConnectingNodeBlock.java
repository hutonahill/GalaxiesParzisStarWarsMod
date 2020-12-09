package com.parzivail.pswg.block;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Map;

public abstract class ConnectingNodeBlock extends Block
{
	public static final BooleanProperty NORTH;
	public static final BooleanProperty EAST;
	public static final BooleanProperty SOUTH;
	public static final BooleanProperty WEST;
	public static final BooleanProperty UP;
	public static final BooleanProperty DOWN;
	public static final Map<Direction, BooleanProperty> FACING_PROPERTIES;

	static
	{
		NORTH = Properties.NORTH;
		EAST = Properties.EAST;
		SOUTH = Properties.SOUTH;
		WEST = Properties.WEST;
		UP = Properties.UP;
		DOWN = Properties.DOWN;
		FACING_PROPERTIES = Util.make(Maps.newEnumMap(Direction.class), (enumMap) -> {
			enumMap.put(Direction.NORTH, NORTH);
			enumMap.put(Direction.EAST, EAST);
			enumMap.put(Direction.SOUTH, SOUTH);
			enumMap.put(Direction.WEST, WEST);
			enumMap.put(Direction.UP, UP);
			enumMap.put(Direction.DOWN, DOWN);
		});
	}

	public ConnectingNodeBlock(Settings settings)
	{
		super(settings);
		setDefaultState(this.stateManager.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(UP, false).with(DOWN, false));
	}

	abstract boolean shouldConnectTo(WorldAccess world, BlockState state, BlockState otherState, BlockPos otherPos, Direction direction);

	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return createConnectedState(ctx.getWorld(), ctx.getBlockPos());
	}

	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState otherState, WorldAccess world, BlockPos pos, BlockPos otherPos)
	{
		if (!state.canPlaceAt(world, pos))
		{
			world.getBlockTickScheduler().schedule(pos, this, 1);
			return super.getStateForNeighborUpdate(state, direction, otherState, world, pos, otherPos);
		}
		else
		{
			return state.with(FACING_PROPERTIES.get(direction), shouldConnectTo(world, state, otherState, otherPos, direction));
		}
	}

	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit)
	{
		if (!player.abilities.allowModifyWorld || !player.isSneaking())
		{
			return ActionResult.PASS;
		}
		else
		{
			world.setBlockState(pos, state.cycle(FACING_PROPERTIES.get(hit.getSide())), 3);
			return ActionResult.success(world.isClient);
		}
	}

	private BlockState createConnectedState(WorldAccess world, BlockPos pos)
	{
		BlockState state = this.getDefaultState();

		for (Map.Entry<Direction, BooleanProperty> pair : FACING_PROPERTIES.entrySet())
		{
			BlockPos neighborPos = pos.offset(pair.getKey());
			BlockState neighborState = world.getBlockState(neighborPos);
			state = state.with(pair.getValue(), shouldConnectTo(world, state, neighborState, neighborPos, pair.getKey()));
		}

		return state;
	}

	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}
}