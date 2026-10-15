package com.smeltlink;

import com.smeltlink.screen.SmeltLinkScreen;
import com.smeltlink.screen.VanillaFurnaceScreenHandler;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SmeltLink.MODID, dist = Dist.CLIENT)
public class SmeltLinkClient {

    public SmeltLinkClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::registerMenuScreens);
        NeoForge.EVENT_BUS.register(VanillaFurnaceScreenHandler.class);
    }

    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SmeltLink.SMELTLINK_MENU.get(), SmeltLinkScreen::new);
    }
}