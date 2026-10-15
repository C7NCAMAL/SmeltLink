package com.smeltlink;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.smeltlink.config.SmeltLinkConfig;
import com.smeltlink.event.FurnaceBreakHandler;
import com.smeltlink.event.FurnaceInteractHandler;
import com.smeltlink.menu.SmeltLinkMenu;
import com.smeltlink.network.SmeltLinkNetwork;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SmeltLink.MODID)
public class SmeltLink {

    public static final String MODID = "smeltlink";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SmeltLinkMenu>> SMELTLINK_MENU =
            MENUS.register("smeltlink_menu", () ->
                    IMenuTypeExtension.create(SmeltLinkMenu::fromNetwork));

    public SmeltLink(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.LOCAL, SmeltLinkConfig.SPEC);

        MENUS.register(modEventBus);
        modEventBus.addListener(SmeltLinkNetwork::register);

        NeoForge.EVENT_BUS.register(FurnaceInteractHandler.class);
        NeoForge.EVENT_BUS.register(FurnaceBreakHandler.class);

        LOGGER.info("SmeltLink initialized.");
    }
}