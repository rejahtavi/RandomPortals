package com.therandomlabs.randomportals.sound;

import java.util.HashMap;
import java.util.Map;

import com.therandomlabs.randomportals.sound.RPOSound.Sounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;

public class SoundEntry {

	public String resource;
	public SoundCategory category = SoundCategory.BLOCKS;
	public float pitchMax = 1.2F;
	public float pitchMin = 0.8F;
	public float volume = 1.0F;
	public int cooldownTicks = 100;
	public int teleportDelayTicks = 100;

	public transient Sounds soundType;
	public transient Map<EntityPlayer, Integer> cooldowns;
	public transient int ambientCooldown;

	public SoundEntry(Sounds soundType) {
		this.soundType = soundType;
	}

	public void ensureCorrect() {
		
		if (resource == null || resource == "") {
			resource = soundType.getDefaultSound();
		} else {
		}

		if (category == null) {
			category = SoundCategory.BLOCKS;
		}

		pitchMax = clamp(pitchMax, 0.5F, 1.5F);
		pitchMin = clamp(pitchMin, 0.5F, pitchMax);
		volume = clamp(volume, 0.0F, 100000.0F);
		cooldownTicks = clamp(cooldownTicks, 20, 600);
		cooldowns = new HashMap<EntityPlayer, Integer>();
		
		RPOSound.queueSoundRegistration(soundType, resource);
	}
	
	public String getResourceString() {
		return resource;
	}

	public boolean waitForCooldown(EntityPlayer player) {
		boolean hasToWait = false;
		if (soundType == Sounds.AMBIENT) {
			if (ambientCooldown > 0) {
				hasToWait = true;
			}
			ambientCooldown = cooldownTicks;
			return hasToWait;
		} else {
			if (player instanceof EntityPlayer && cooldowns != null) {
				if (cooldowns.containsKey(player)) {
					hasToWait = true;
				}
				cooldowns.put(player, cooldownTicks);
			}
			return hasToWait;
		}
	}

	public void tickCooldowns() {
		if (soundType == Sounds.AMBIENT) {
			ambientCooldown = (ambientCooldown <= 0) ? 0 : ambientCooldown - 1;
		} else {
			for (EntityPlayer player : cooldowns.keySet()) {
				Integer ticksLeft = cooldowns.get(player);
				if (ticksLeft == null || ticksLeft <= 0) {
					cooldowns.remove(player);
				} else {
					cooldowns.put(player, cooldowns.get(player) - 1);
				}
			}
		}
	}

	public float clamp(float in, float min, float max) {
		return Math.max(min, Math.min(max, in));
	}

	public int clamp(int in, int min, int max) {
		return Math.max(min, Math.min(max, in));
	}
}