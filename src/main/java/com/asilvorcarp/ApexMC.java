package com.asilvorcarp;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import static com.asilvorcarp.NetworkingConstants.PING_PACKET;

public class ApexMC implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final String MOD_ID = "apex_mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ArrayList<ApexTeam> teams = new ArrayList<>();
    public static boolean ENABLE_TEAMS = false;

    public static final Identifier PING_LOCATION_SOUND = new Identifier("apex_mc:ping_location");
    public static SoundEvent PING_LOCATION_EVENT = SoundEvent.of(PING_LOCATION_SOUND);
    public static final Identifier PING_ITEM_SOUND = new Identifier("apex_mc:ping_item");
    public static SoundEvent PING_ITEM_EVENT = SoundEvent.of(PING_ITEM_SOUND);
    public static final Identifier PING_ENEMY_SOUND = new Identifier("apex_mc:ping_enemy");
    public static SoundEvent PING_ENEMY_EVENT = SoundEvent.of(PING_ENEMY_SOUND);
    public static final Identifier MOZAMBIQUE_LIFELINE_SOUND = new Identifier("apex_mc:mozambique_lifeline");
    public static SoundEvent MOZAMBIQUE_LIFELINE_EVENT = SoundEvent.of(MOZAMBIQUE_LIFELINE_SOUND);
    // TODO be able to config this
    public static final SoundEvent DEFAULT_SOUND_EVENT = PING_LOCATION_EVENT;
    // maybe SoundEvents.BLOCK_ANVIL_BREAK

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Make MC Apex Again!");

        // teams.add(new ApexTeam("Asc", "Eyn"));
        // TODO add to team command, save state to file

        // register receiver
        ServerPlayNetworking.registerGlobalReceiver(NetworkingConstants.PING_PACKET, ((server, player, handler, buf, responseSender) -> {
            multicastPing(player, PING_PACKET, buf);
        }));

        // register sound events
        Registry.register(Registries.SOUND_EVENT, PING_LOCATION_SOUND, PING_LOCATION_EVENT);
        Registry.register(Registries.SOUND_EVENT, PING_ITEM_SOUND, PING_ITEM_EVENT);
        Registry.register(Registries.SOUND_EVENT, PING_ENEMY_SOUND, PING_ENEMY_EVENT);
        Registry.register(Registries.SOUND_EVENT, MOZAMBIQUE_LIFELINE_SOUND, MOZAMBIQUE_LIFELINE_EVENT);
    }

    public static void multicastPing(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
        if (ENABLE_TEAMS) {
            // TODO implement teams
        } else {
            for (ServerPlayerEntity teammate : PlayerLookup.world((ServerWorld) sender.getWorld())) {
                var senderName = sender.getEntityName();
                var teammateName = teammate.getEntityName();
                // play sound for all // TODO how to play for one
                teammate.getWorld().playSound(
                        null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                        teammate.getBlockPos(), // The position of where the sound will come from
                        DEFAULT_SOUND_EVENT,
                        SoundCategory.BLOCKS, // This determines which of the volume sliders affect this sound
                        1f, // Volume multiplier, 1 is normal, 0.5 is half volume, etc
                        1f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
                );
                // packet skip oneself
                if (Objects.equals(teammateName, senderName)){
                    continue;
                }
                var bufNew = PacketByteBufs.copy(buf.asByteBuf());
                ServerPlayNetworking.send(teammate, channelName, bufNew);
                LOGGER.info("%s send ping to %s".formatted(senderName, teammateName));
            }
        }
    }

    @NotNull
    static Vector3d Vec3dToVector3d(Vec3d cameraDir) {
        Vector3d ret = new Vector3d();
        ret.x = cameraDir.x;
        ret.y = cameraDir.y;
        ret.z = cameraDir.z;
        return ret;
    }

    public static Vector3f Vec3dToV3f(Vec3d v) {
        var ret = new Vector3f();
        ret.y = (float) v.y;
        ret.z = (float) v.z;
        ret.x = (float) v.x;
        return ret;
    }
}