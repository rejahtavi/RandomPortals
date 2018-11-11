package com.therandomlabs.verticalendportals;

import com.therandomlabs.randompatches.TileEntityEndPortalRenderer;
import com.therandomlabs.verticalendportals.command.CommandVEPReload;
import com.therandomlabs.verticalendportals.tileentity.TileEntityVerticalEndPortal;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;

public final class ClientProxy extends CommonProxy {
	@Override
	public void preInit() {
		super.preInit();

		if(VEPConfig.client.vepreloadclientCommand) {
			ClientCommandHandler.instance.registerCommand(new CommandVEPReload(Side.CLIENT));
		}

		ClientRegistry.bindTileEntitySpecialRenderer(
				TileEntityVerticalEndPortal.class, new TileEntityEndPortalRenderer()
		);
	}
}
