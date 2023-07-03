package com.asilvorcarp;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

import static com.asilvorcarp.ApexMC.MOD_ID;

public class InitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        RenderHandler renderer = RenderHandler.getInstance();
        RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
        RenderEventHandler.getInstance().registerTooltipLastRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());

//        KeyCallbacks.init();
    }
}