package com.therandomlabs.verticalendportals.block;

import java.util.EnumMap;
import java.util.Map;
import com.therandomlabs.verticalendportals.tileentity.TileEntityVerticalEndPortal;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockVerticalEndPortal extends BlockEndPortal {
	public static final PropertyDirection FACING = BlockHorizontal.FACING;

	private static final Map<EnumFacing, AxisAlignedBB> AABB_BLOCK = new EnumMap<>(EnumFacing.class);

	static {
		AABB_BLOCK.put(EnumFacing.NORTH, new AxisAlignedBB(
				0.0, 0.0, 0.1875, 1.0, 1.0, 1.0
		));

		AABB_BLOCK.put(EnumFacing.SOUTH, new AxisAlignedBB(
				0.0, 0.0, 0.0, 1.0, 1.0, 0.8125
		));

		AABB_BLOCK.put(EnumFacing.WEST, new AxisAlignedBB(
				0.1875, 0.0, 0.0, 1.0, 1.0, 1.0
		));

		AABB_BLOCK.put(EnumFacing.EAST, new AxisAlignedBB(
				0.0, 0.0, 0.0, 0.8125, 1.0, 1.0
		));
	}

	public BlockVerticalEndPortal() {
		super(Material.PORTAL);
		setLightLevel(1.0F);
		setHardness(-1.0F);
		setResistance(6000000.0F);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
		setCreativeTab(CreativeTabs.DECORATIONS);
		setTranslationKey("endPortalVertical");
		setRegistryName("vertical_end_portal");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityVerticalEndPortal();
	}

	@Override
	public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos,
			Entity entity) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos,
			EnumFacing side) {
		final EnumFacing facing = state.getValue(FACING);

		if(side != facing && side != facing.getOpposite()) {
			return false;
		}

		final AxisAlignedBB axisAlignedBB = state.getBoundingBox(world, pos);

		switch(side) {
		case NORTH:
			if(axisAlignedBB.minZ > 0.0D) {
				return true;
			}

			break;
		case SOUTH:
			if(axisAlignedBB.maxZ < 1.0D) {
				return true;
			}

			break;
		case WEST:
			if(axisAlignedBB.minX > 0.0D) {
				return true;
			}

			break;
		case EAST:
			if(axisAlignedBB.maxX < 1.0D) {
				return true;
			}
		}

		return !world.getBlockState(pos.offset(side)).doesSideBlockRendering(
				world, pos.offset(side), side.getOpposite()
		);
	}

	@SuppressWarnings("deprecation")
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return AABB_BLOCK.get(state.getValue(FACING));
	}

	@SuppressWarnings("deprecation")
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX,
			float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@SuppressWarnings("deprecation")
	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex();
	}

	@SuppressWarnings("deprecation")
	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return state.withRotation(mirror.toRotation(state.getValue(FACING)));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}
}
