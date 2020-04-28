package com.therandomlabs.randomportals.sound;

import java.util.HashMap;
import java.util.Map;
import com.therandomlabs.randomportals.api.config.PortalType;
import com.therandomlabs.randomportals.world.storage.RPOSavedData;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortalSounds {

	private static Map<String, PortalSoundConfig> portalSoundConfigs = new HashMap<>();

	PortalSounds() {
	}

	public int size() {
		return portalSoundConfigs.size();
	}

	public void put(String groupName, PortalSoundConfig portalSoundConfig) {
		portalSoundConfigs.put(groupName, portalSoundConfig);
	}

	public PortalSoundConfig get(World world, BlockPos pos) {
		PortalType portalType = RPOSavedData.get(world).getNetherPortalByInner(pos).getType();
		return get(portalType);
	}

	public PortalSoundConfig get(PortalType portalType) {
		return portalSoundConfigs.get(portalType.toString().split(":")[0]);
	}
	
	public void tickCooldowns() {
		for (PortalSoundConfig psc : portalSoundConfigs.values()) {
			psc.tickCooldowns();
		}
	}
}
