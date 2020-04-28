package com.therandomlabs.randomportals.sound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.therandomlabs.randomportals.RandomPortals;
import com.therandomlabs.randomportals.api.config.PortalType;
import com.therandomlabs.randomportals.api.config.PortalTypes;
import com.therandomlabs.randomportals.api.event.NetherPortalEvent;
import com.therandomlabs.randomportals.config.RPOConfig;
import com.therandomlabs.randomportals.world.storage.RPOSavedData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/* 
 * ===== NOTES =====
 * 
 * The sound configuration data is separate from RPOConfig because we need to get the names of requested sound events
 * before we can register them for playback. The rest of the config is loaded too late for that to happen.
 * 
 * All SoundEvents must be instantiated inside the Register<SoundEvent> event, then a handle to that sound event can
 * be extracted from the ForgeRegistries.SOUND_EVENTS registry.
 * 
 */

public class RPOSound {

	/*
	 * ===== CONSTANTS & SHARED DATA =====
	 */
	public static final Random rand = new java.util.Random();
	public static final String VANILLA_PORTAL_GROUP = PortalTypes.VANILLA_NETHER_PORTAL_ID;
	public static final String SOUND_CONFIG_FILENAME = "group_sounds.json";

	public static boolean inPreInit = false;
	public static PortalSounds portalSounds = new PortalSounds();
	public static Map<String, SoundEvent> soundEvents = new HashMap<>();
	public static List<String> registrationQueue = new ArrayList<>();

	/*
	 * ===== ENUMS =====
	 */
	public static enum Sounds {
		ACTIVATE(RandomPortals.MOD_ID + ":activate"), AMBIENT(RandomPortals.MOD_ID + ":ambient"),
		TRIGGER(RandomPortals.MOD_ID + ":trigger"), TRAVEL(RandomPortals.MOD_ID + ":travel"),
		DESTROY(RandomPortals.MOD_ID + ":destroy");

		private final String defaultResource;

		private Sounds(String defaultResource) {
			this.defaultResource = defaultResource;
		}

		public void queueRegisterDefaultSound() {
			queueSoundRegistration(this, defaultResource);
		}

		public String getDefaultSound() {
			return defaultResource;
		}
	}

	/*
	 * ===== EVENT HANDLERS =====
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void onRegisterSoundEvent(RegistryEvent.Register<SoundEvent> event) {

		soundEvents.clear();

		// for safety checks -- it is now OK to register sounds
		inPreInit = true;

		// register all built-in sounds
		// registerSound(null, FALLBACK_RESOURCE);
		for (Sounds sound : Sounds.values()) {
			sound.queueRegisterDefaultSound();
		}

		// load in user-requested configuration
		loadConfigFromDisk();

		RandomPortals.LOGGER.warn("found " + portalSounds.size() + " portal types.");

		RandomPortals.LOGGER.warn("registrationQueue contains:");
		for (String s : registrationQueue) {
			RandomPortals.LOGGER.warn(s);
		}

		// load all requested sounds
		for (String resource : registrationQueue) {
			if (!soundEvents.containsKey(resource)) {
				try {
					ResourceLocation resourceLocation = new ResourceLocation(resource);
					SoundEvent soundEvent = new SoundEvent(resourceLocation).setRegistryName(resourceLocation);
					event.getRegistry().register(soundEvent);
					soundEvents.put(resource, event.getRegistry().getValue(resourceLocation));
				} catch (Exception e) {
					RandomPortals.LOGGER
							.error("Something went wrong trying to register SoundEvent for '" + resource + "'!:");
					RandomPortals.LOGGER
							.error("If you are using custom sounds, please check your resource pack's sounds.json "
									+ "as well as your resource loader's configuration. More details:");
					RandomPortals.LOGGER.error(e.getMessage());
					RandomPortals.LOGGER.error(e.getStackTrace());
				}
			}
		}

		// never try to register sounds after this point or you will get NPEs
		inPreInit = false;
	}

	// Blocks the vanilla portal sounds triggered outside of our control
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void blockVanillaTeleportSounds(PlaySoundEvent event) {
		if ((event.getName().equals("block.portal.trigger")) || (event.getName().equals("block.portal.travel"))) {
			event.setResultSound(null);
		}
	}

	// Trigger for portal destroyed sounds
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
	public static void netherPortalBroken(NetherPortalEvent.Remove event) {
		PortalType portalType = event.getPortal().getType();
		World world = event.getWorld();
		BlockPos pos = getCenterBlock(event.getPortal().getFrame().getCornerBlockPositions(), false);
		playSound(Sounds.DESTROY, portalType, null, world, pos);
	}

	// Trigger for portal travel sounds
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
	public static void onEntityTravelToDimensionEvent(EntityTravelToDimensionEvent event) {
		if (event.getEntity() instanceof EntityPlayer) {
			playSound(Sounds.TRAVEL, null, (EntityPlayer) event.getEntity(), null, null);
		}
	}

	// responsible for decrementing each sound's per-player cooldown timers
//	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
//	public static void tickCooldowns(TickEvent event) {
//		portalSounds.tickCooldowns();
//	}

	/*
	 * ===== CONFIGURATION & INIT =====
	 */
	public static void loadConfigFromDisk() {

		boolean loadingCompleted = false;
		final Path portalTypesDir = RPOConfig.getDirectory(PortalTypes.PORTAL_TYPES_DIRECTORY);
		List<Path> dirPaths;

		try (final Stream<Path> pathStream = Files.list(portalTypesDir)) {

			// Get a directory listing from the portal types folder
			dirPaths = pathStream.collect(Collectors.toList());

			// Walk the directory list
			for (Path path : dirPaths) {

				// Skip over anything that isn't a subdirectory
				if (!Files.isDirectory(path)) {
					continue;
				}

				// Got a subdirectory, check inside it for sound configuration data
				final String portalFolder = path.getFileName().toString();
				final Path configFile = path.resolve(SOUND_CONFIG_FILENAME);

				// Try to load configuration data into a temporary object
				PortalSoundConfig tempConfig;

				if (Files.exists(configFile) && Files.isRegularFile(configFile)) {

					tempConfig = RPOConfig.readJson(configFile, PortalSoundConfig.class);

					if (tempConfig == null) {
						tempConfig = new PortalSoundConfig();
						RandomPortals.LOGGER.warn(
								"Failed to read sound data from folder '" + portalFolder + "'. Creating defaults.");
					}

				} else {
					tempConfig = new PortalSoundConfig();
					RandomPortals.LOGGER
							.warn("No sound data found in folder '" + portalFolder + "'. Creating defaults.");
				}

				// check the temporary object for errors, and fix them
				tempConfig.ensureCorrect();

				// copy the loaded and checked config into our global state
				portalSounds.put(portalFolder, tempConfig);

				// write a corrected file back to disk
				RPOConfig.writeJson(configFile, tempConfig);
				RandomPortals.LOGGER.warn("Writing config for folder '" + portalFolder + "' to disk.");
			}
		} catch (IOException e) {
			RandomPortals.LOGGER.error("Failed to load sound configuration! Custom portal sounds may not work!");
			RandomPortals.LOGGER.error(e.getMessage());
			RandomPortals.LOGGER.error(e.getStackTrace().toString());
		}
	}

	public static void queueSoundRegistration(Sounds soundType, String resource) {
		SoundEvent soundEvent;
		if (inPreInit) {
			if (resource == null) {
				resource = soundType.getDefaultSound();
				RandomPortals.LOGGER.error(
						"attempted to register null resource. check your sounds.json/resource pack/resource loader!");
			}
			if (!registrationQueue.contains(resource)) {
				registrationQueue.add(resource);
			}
		} else {
			RandomPortals.LOGGER
					.error("Sound Registration attempt outside of registration window! *** This is a bug! ***");
		}
	}

	/*
	 * ===== UTILITY FUNCTIONS =====
	 */
	public static PortalType getPortalTypeByPos(World world, BlockPos pos) {
		return RPOSavedData.get(world).getNetherPortalByInner(pos).getType();
	}

	// set gravity=true to get the bottom center position
	// set gravity=false to get the center of mass
	public static BlockPos getCenterBlock(List<BlockPos> blocks, boolean gravity) {
		int c = 0;
		int x = 0;
		int z = 0;
		int y = 256;
		for (BlockPos pos : blocks) {
			c++;
			x += pos.getX();
			y = (gravity) ? Math.min(y, pos.getY()) : y + pos.getY();
			z += pos.getZ();
		}
		x = x / c;
		y = (gravity) ? y : y / c;
		z = z / c;
		return new BlockPos(x, y, z);
	}

	public static float getPitch(SoundEntry entry) {
		return ((rand.nextFloat() * (entry.pitchMax - entry.pitchMin)) + entry.pitchMin);
	}

	/*
	 * ===== SOUND FUNCTIONS =====
	 */

	// This function is tolerant of multiple nulls in the input.
	// Minimum input requirements are ONE of the following patterns:
	// soundType, null, player, null, null
	// soundType, null, null, world, pos
	// if it's available, providing the portal type at calling time
	// will save some cycles versus looking it up from RPOSavedData.

	public static void playSound(Sounds soundType, PortalType portalType, EntityPlayer player, World world,
			BlockPos pos) {

		// derive missing information
		if (world == null && player instanceof EntityPlayer) {
			world = player.world;
		}
		if (pos == null && player instanceof EntityPlayer) {
			pos = player.getPosition();
		}

		SoundEntry entry = null;
		if (portalType != null) {
			entry = portalSounds.get(portalType).get(soundType);
		} else {
			if (world != null && pos != null) {
				entry = portalSounds.get(world, pos).get(soundType);
			}
		}

		if (entry != null) {

			if (player != null) {
				// creative & spectator modes traverse portals instantly,
				// so don't bother playing the trigger sound.
				if (soundType == Sounds.TRIGGER && (player.isCreative() || player.isSpectator())) {
					return;
				}

				// make sure the player didn't recently play
				// this particular sound effect, and reset
				// their cooldown timer either way.
				if (entry.waitForCooldown(player)) {
					return;
				}
			}
			playSoundEntry(entry, world, pos);
		}
	}

	// Do not call this directly; use playSound instead.
	public static void playSoundEntry(SoundEntry entry, World world, BlockPos pos) {

		String resourceName = null;
		SoundEvent soundEvent = null;

		try {
			resourceName = entry.getResourceString();
			if (resourceName == null) {
				RandomPortals.LOGGER.error("got null ResourceLocation when attempting to play '" + resourceName + "'");
				return;
			}

			soundEvent = soundEvents.get(resourceName);
			if (soundEvent == null) {
				RandomPortals.LOGGER.error("got null SoundEvent when attempting to play '" + resourceName + "'");
				return;
			}

			RandomPortals.LOGGER.warn("playing sound '" + resourceName + "'");

			world.playSound(null, pos, soundEvent, entry.category, entry.volume, getPitch(entry));

		} catch (Exception e) {
			// OUCH! -- Let's not try that again. Remove the offending object >:/
			soundEvents.remove(entry.getResourceString());
			RandomPortals.LOGGER.error("Ouch! Something went wrong trying to play a sound ("
					+ ((resourceName == null) ? "null" : resourceName.toString())
					+ "). If you are using custom sounds, "
					+ "please check your sounds.json and resource pack or resource loader!");
			RandomPortals.LOGGER.error(e.getMessage());
			RandomPortals.LOGGER.error(e.getStackTrace());
		}
	}

}
