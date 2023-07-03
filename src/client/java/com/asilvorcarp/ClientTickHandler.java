package com.asilvorcarp;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.MinecraftClient;

public class ClientTickHandler implements IClientTickHandler {
    @Override
    public void onClientTick(MinecraftClient mc) {
        if (mc.world != null && mc.player != null) {
            RenderHandler.getInstance().updateData(mc);
        }
    }
}

