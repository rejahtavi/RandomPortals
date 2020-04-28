package com.therandomlabs.randomportals.api.config;

import java.util.ArrayList;
import java.util.List;
import com.therandomlabs.randomportals.util.RegistryNameAndMeta;
import net.minecraft.item.ItemStack;

public final class ActivationData {
	public enum ConsumeBehavior {
		CONSUME,
		DAMAGE,
		DO_NOTHING
	}

	public boolean canBeActivatedByFire = true;

	public List<PortalActivator> activators = new ArrayList<>();

	public ConsumeBehavior activatorConsumeBehavior = ConsumeBehavior.CONSUME;

	public boolean spawnFireBeforeActivating = true;
	
	public boolean lightningStrikeOnActivate = false;
	public boolean lightningStrikeRequiresSky = false;
	public boolean lightningStrikeSpawnsFire = true;

	@SuppressWarnings("Duplicates")
	public void ensureCorrect() {
		final List<RegistryNameAndMeta> checkedItems = new ArrayList<>();

		for (int i = 0; i < activators.size(); i++) {
			final PortalActivator activator = activators.get(i);
			final RegistryNameAndMeta registryNameAndMeta = new RegistryNameAndMeta(
					activator.registryName, activator.meta
			);

			if (!activator.isValid() || checkedItems.contains(registryNameAndMeta)) {
				activators.remove(i--);
				continue;
			}

			checkedItems.add(registryNameAndMeta);
		}
	}

	public boolean test(ItemStack stack) {
		for (PortalActivator activator : activators) {
			if (activator.test(stack)) {
				return true;
			}
		}

		return false;
	}
}
