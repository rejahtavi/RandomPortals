package com.therandomlabs.verticalendportals.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//Because BlockPortal forces the AXIS property, which only accepts X and Z,
//we have to behave as if the block is on the Y-axis while ignoring the AXIS property,
//which is always X
public class BlockLateralNetherPortal extends BlockNetherPortal {
	public BlockLateralNetherPortal() {
		super(true);
		setTranslationKey("netherPortalLateral");
		setRegistryName("lateral_nether_portal");
	}

	@Override
	public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
		return new ItemStack(this);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState();
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return 1;
	}

	@Override
	public EnumFacing.Axis getAxis(IBlockState state) {
		return EnumFacing.Axis.Y;
	}

	@SuppressWarnings("deprecation")
	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return getDefaultState();
	}
}
