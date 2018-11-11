package com.parzivail.swg.block.light;

import com.parzivail.swg.StarWarsGalaxy;
import com.parzivail.swg.tile.light.TileAngledWallLight;
import com.parzivail.util.block.BlockUtils;
import com.parzivail.util.block.HarvestLevel;
import com.parzivail.util.block.PBlockRotate;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockAngledWallLight extends PBlockRotate
{
	public BlockAngledWallLight()
	{
		super("angledWallLight", Material.iron, 8);
		setCreativeTab(StarWarsGalaxy.tab);
		setHardness(50.0F);
		setLightLevel(1);
		setHarvestLevel("pickaxe", HarvestLevel.IRON);
	}

	public AxisAlignedBB getCollisionBoundingBoxFromPool(World worldIn, int x, int y, int z)
	{
		setBlockBoundsBasedOnState(worldIn, x, y, z);
		return super.getCollisionBoundingBoxFromPool(worldIn, x, y, z);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_)
	{
		BlockUtils.setSmall8SidedBounds(p_149719_1_, this, p_149719_2_, p_149719_3_, p_149719_4_);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta)
	{
		return new TileAngledWallLight();
	}

	@Override
	public int getRenderType()
	{
		return -1;
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}
}
