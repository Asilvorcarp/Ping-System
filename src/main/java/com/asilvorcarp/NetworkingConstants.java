package com.asilvorcarp;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class NetworkingConstants {
    public static final Identifier PING_PACKET = new Identifier(ApexMC.MOD_ID, "ping");
}
