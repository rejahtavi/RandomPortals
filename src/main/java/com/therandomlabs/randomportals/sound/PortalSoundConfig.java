package com.therandomlabs.randomportals.sound;

import java.util.HashMap;
import java.util.Map;
import com.therandomlabs.randomportals.sound.RPOSound.Sounds;

public class PortalSoundConfig {

	public float ambientSoundChance = 1.0F;
	public Map<String, SoundEntry> soundEntries = new HashMap<String, SoundEntry>();

	PortalSoundConfig() {
		for (Sounds soundType : Sounds.values()) {
			soundEntries.put(soundType.name(), new SoundEntry(soundType));
		}
	}

	public void ensureCorrect() {
		ambientSoundChance = clamp(ambientSoundChance, 0.0F, 1.0F);
		for (SoundEntry soundEntry : soundEntries.values()) {
			soundEntry.ensureCorrect();
		}
	}
	
	public void tickCooldowns() {
		for (SoundEntry entry : soundEntries.values()) {
			entry.tickCooldowns();
		}
	}

	public SoundEntry get(Sounds soundType) {
		return soundEntries.get(soundType.name());
	}

	public float clamp(float in, float min, float max) {
		return Math.max(min, Math.min(max, in));
	}
}
