package com.asilvorcarp;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.Objects;

import static com.asilvorcarp.ApexMC.LOGGER;
import static com.asilvorcarp.NetworkingConstants.PING_PACKET;
import static com.asilvorcarp.NetworkingConstants.REMOVE_PING_PACKET;

public class ApexMCClient implements ClientModInitializer {
    public static final double MAX_REACH = 512.0D;
    private static KeyBinding pingKeyBinding;

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        pingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.apex_mc.ping", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_Z, // The keycode of the key
                "category.apex_mc.apex" // The translation key of the keybinding's category.
        ));

        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());

        ClientTickEvents.END_CLIENT_TICK.register(ApexMCClient::checkKeyPress);

        ClientPlayNetworking.registerGlobalReceiver(PING_PACKET, (client, handler, buf, responseSender) -> {
            // Everything in this lambda is run on the render thread
            pingReceiver(buf);
        });

        ClientPlayNetworking.registerGlobalReceiver(REMOVE_PING_PACKET, (client, handler, buf, responseSender) -> {
            removePingReceiver(buf);
        });
    }

    private static void checkKeyPress(MinecraftClient client) {
        while (pingKeyBinding.wasPressed()) {
            assert client.player != null;
            var player = client.player;

            float tickDelta = 1.0f; // TODO test this
            assert client.cameraEntity != null;
            Vec3d cameraDirection = client.cameraEntity.getRotationVec(tickDelta);

            // TODO add config for include fluids
            pingDirection(client, player, tickDelta, cameraDirection, false);
        }
    }

    private static void pingDirDistance(MinecraftClient client, ClientPlayerEntity player, float tickDelta, Vec3d dir, double dist) {
        assert client.cameraEntity != null;
        Vec3d cameraPos = client.cameraEntity.getPos();
        Vec3d pingPos = cameraPos.add(dir.multiply(dist));
        PingPoint p = new PingPoint(pingPos, player.getEntityName());
        addPointToRenderer(p);
        sendPingToServer(p);
    }

    // also show some info about the thing pinging on

    private static Vec3d pingDirection(MinecraftClient client, ClientPlayerEntity player, float tickDelta,
                                       Vec3d dir, boolean includeFluids) {
        assert client.world != null;
        assert client.cameraEntity != null;
        HitResult hit = raycast(client.cameraEntity, MAX_REACH, tickDelta, includeFluids, dir);

        Vec3d pingPos = null;
        switch (Objects.requireNonNull(hit).getType()) {
            case MISS -> player.sendMessage(Text.literal("Too far"), true);
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = client.world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                final Text blockMes = block.getName();
                player.sendMessage(blockMes, true);
                pingPos = hit.getPos();
            }
            // TODO (hard) follow entity
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                final Text entityMes = entity.getName();
                player.sendMessage(entityMes, false);
                pingPos = hit.getPos();
            }
        }

        if (pingPos != null) {
            pingPosition(player, pingPos);
        }

        return pingPos;
    }
    private static void pingPosition(ClientPlayerEntity player, Vec3d pingPos) {
        LOGGER.debug("Ping at " + pingPos);
        PingPoint p = new PingPoint(pingPos, player.getEntityName());
        RenderHandler renderer = RenderHandler.getInstance();
        if(renderer.isOnPing()){
           renderer.removeOnPing();
           sendRemovePingToServer(renderer.getOnPing());
        } else {
            addPointToRenderer(p);
            sendPingToServer(p);
        }
    }

    private static void addPointToRenderer(PingPoint p) {
        RenderHandler.getInstance().addPing(p);
    }

    private void removePointAtRenderer(PingPoint p) {
        RenderHandler.getInstance().removePing(p);
    }

    private static HitResult raycast(
            Entity entity,
            double maxDistance,
            float tickDelta,
            boolean includeFluids,
            Vec3d direction
    ) {
        Vec3d end = entity.getCameraPosVec(tickDelta).add(direction.multiply(maxDistance));
        return entity.getWorld().raycast(new RaycastContext(
                entity.getCameraPosVec(tickDelta),
                end,
                RaycastContext.ShapeType.OUTLINE,
                includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                entity
        ));
    }

    public static void sendPingToServer(PingPoint p) {
        try {
            PacketByteBuf buf = p.toPacketByteBuf();
            ClientPlayNetworking.send(PING_PACKET, buf);
        } catch (IOException e) {
            LOGGER.error("Fail to send ping packet to server", e);
        }
    }

    private static void sendRemovePingToServer(PingPoint p){
        try {
            PacketByteBuf buf = p.toPacketByteBuf();
            ClientPlayNetworking.send(REMOVE_PING_PACKET, buf);
        } catch (IOException e) {
            LOGGER.error("Fail to send remove ping packet to server", e);
        }
    }

    public void pingReceiver(PacketByteBuf buf) {
        try {
            var p = PingPoint.fromPacketByteBuf(buf);
            addPointToRenderer(p);
            LOGGER.debug("Received ping at " + p.pos.toString());
        } catch (Exception e) {
            LOGGER.error("Fail to deserialize the ping packet received", e);
        }
    }

    private void removePingReceiver(PacketByteBuf buf) {
        try {
            var p = PingPoint.fromPacketByteBuf(buf);
            removePointAtRenderer(p);
            LOGGER.debug("Received remove ping at " + p.pos.toString());
        } catch (Exception e) {
            LOGGER.error("Fail to deserialize the remove ping packet received", e);
        }
    }
}