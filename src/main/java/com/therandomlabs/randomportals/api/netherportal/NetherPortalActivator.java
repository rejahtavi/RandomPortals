package com.therandomlabs.randomportals.api.netherportal;

import java.util.function.BiFunction;
import com.therandomlabs.randomportals.api.config.NetherPortalType;
import com.therandomlabs.randomportals.api.config.NetherPortalTypes;
import com.therandomlabs.randomportals.api.event.NetherPortalEvent;
import com.therandomlabs.randomportals.api.frame.Frame;
import com.therandomlabs.randomportals.api.util.StatePredicate;
import com.therandomlabs.randomportals.block.BlockNetherPortal;
import com.therandomlabs.randomportals.block.RPOBlocks;
import com.therandomlabs.randomportals.frame.NetherPortalFrames;
import com.therandomlabs.randomportals.world.storage.RPOSavedData;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class NetherPortalActivator {
	private static final class PortalContainer {
		NetherPortal portal;
	}

	private NetherPortalType forcePortalType;
	private NetherPortalType[] portalTypes;
	private boolean userCreated = true;
	private boolean activatedByFire;

	public NetherPortalType getForcedPortalType() {
		return forcePortalType;
	}

	public NetherPortalActivator forcePortalType(NetherPortalType type) {
		forcePortalType = type;
		return this;
	}

	public NetherPortalType[] getPortalTypes() {
		return portalTypes == null ? null : portalTypes.clone();
	}

	public NetherPortalActivator setPortalTypes(NetherPortalType... portalTypes) {
		if(portalTypes.length == 0) {
			portalTypes = null;
		}

		for(NetherPortalType type : portalTypes) {
			if(type == null) {
				throw new NullPointerException("portalTypes");
			}
		}

		if(portalTypes != null && forcePortalType != null) {
			throw new IllegalStateException(
					"setPortalTypes cannot be called when forcePortalType is not null"
			);
		}

		this.portalTypes = portalTypes;
		return this;
	}

	public boolean isUserCreated() {
		return userCreated;
	}

	public NetherPortalActivator setUserCreated(boolean flag) {
		userCreated = flag;
		return this;
	}

	public boolean isActivatedByFire() {
		return activatedByFire;
	}

	public NetherPortalActivator setActivatedByFire(boolean flag) {
		activatedByFire = flag;
		return this;
	}

	public NetherPortal activate(World world, BlockPos pos) {
		return activate(world, pos, (axis, color) -> {
			final IBlockState state;

			switch(axis) {
			case X:
				state = ((BlockNetherPortal) Blocks.PORTAL).getByColor(color).getDefaultState();
				break;
			case Y:
				state = RPOBlocks.purple_lateral_nether_portal.getByColor(color).getDefaultState();
				break;
			default:
				state = ((BlockNetherPortal) Blocks.PORTAL).getByColor(color).getDefaultState().
						withProperty(BlockPortal.AXIS, EnumFacing.Axis.Z);
			}

			return state.withProperty(BlockNetherPortal.USER_PLACED, false);
		});
	}

	public NetherPortal activate(World world, BlockPos pos, IBlockState lateralPortal,
			IBlockState verticalXPortal, IBlockState verticalZPortal) {
		return activate(world, pos, (axis, color) -> {
			switch(axis) {
			case X:
				return verticalXPortal;
			case Y:
				return lateralPortal;
			default:
				return verticalZPortal;
			}
		});
	}

	public NetherPortal activate(World world, BlockPos pos,
			BiFunction<EnumFacing.Axis, EnumDyeColor, IBlockState> portalBlocks) {
		final StatePredicate validBlocks;

		if(forcePortalType == null) {
			if(portalTypes == null) {
				validBlocks = NetherPortalTypes.getValidBlocks();
			} else {
				validBlocks = NetherPortalTypes.getValidBlocks(portalTypes);
			}
		} else {
			validBlocks = NetherPortalTypes.getValidBlocks(forcePortalType);
		}

		final PortalContainer portal = new PortalContainer();
		BlockPos framePos = null;

		for(EnumFacing facing : EnumFacing.values()) {
			final BlockPos offset = pos.offset(facing);
			final IBlockState state = world.getBlockState(offset);

			if(!validBlocks.test(world, pos, state)) {
				continue;
			}

			NetherPortalFrames.EMPTY_FRAMES.detectWithCondition(world, offset, potentialFrame -> {
				final NetherPortal result = testFrame(potentialFrame, offset, facing.getOpposite());

				if(result == null) {
					return false;
				}

				portal.portal = result;
				return true;
			});

			if(portal.portal != null) {
				framePos = offset;
			}

			break;
		}

		if(portal.portal == null) {
			return null;
		}

		final NetherPortalEvent.Activate event = new NetherPortalEvent.Activate(
				world, portal.portal, framePos, userCreated, activatedByFire
		);

		if(MinecraftForge.EVENT_BUS.post(event)) {
			return null;
		}

		final NetherPortal result = event.getPortal();
		onActivate(world, result, portalBlocks);
		return result;
	}

	protected void onActivate(World world, NetherPortal portal,
			BiFunction<EnumFacing.Axis, EnumDyeColor, IBlockState> portalBlocks) {
		RPOSavedData.get(world).addNetherPortal(portal, userCreated);

		final Frame frame = portal.getFrame();
		final IBlockState state = portalBlocks.apply(
				frame.getType().getAxis(), portal.getType().color
		);

		for(BlockPos innerPos : frame.getInnerBlockPositions()) {
			world.setBlockState(innerPos, state, 2);
		}
	}

	protected NetherPortal testFrame(Frame frame, BlockPos framePos, EnumFacing inwards) {
		if(!frame.isFacingInwards(framePos, inwards)) {
			return null;
		}

		if(forcePortalType != null) {
			return new NetherPortal(frame, null, forcePortalType);
		}

		final NetherPortalType[] types;

		if(portalTypes == null) {
			types = NetherPortalTypes.getTypes().values().toArray(new NetherPortalType[0]);
		} else {
			types = portalTypes;
		}

		for(NetherPortalType type : types) {
			if((!activatedByFire || type.canBeActivatedByFire) && type.test(frame)) {
				return new NetherPortal(frame, null, type);
			}
		}

		return null;
	}
}