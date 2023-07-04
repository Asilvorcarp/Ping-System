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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.Objects;

import static com.asilvorcarp.ApexMC.LOGGER;
import static com.asilvorcarp.NetworkingConstants.PING_PACKET;
import static com.asilvorcarp.RenderHandler.XY2Vec3d;

public class ApexMCClient implements ClientModInitializer {
    public static final double MAX_REACH = 256.0D;
    private static KeyBinding pingKeyBinding;

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        pingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.apex_mc.ping", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_I, // The keycode of the key
                "category.apex_mc.apex" // The translation key of the keybinding's category.
        ));

        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());

        ClientTickEvents.END_CLIENT_TICK.register(ApexMCClient::checkKeyPress);

        ClientPlayNetworking.registerGlobalReceiver(PING_PACKET, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                // Everything in this lambda is run on the render thread
                pingReceiver(buf);
            });
        });
    }

    private static void checkKeyPress(MinecraftClient client) {
        while (pingKeyBinding.wasPressed()) {
            System.out.println("pressed ping key");

            assert client.player != null;
            var player = client.player;
//            Text mes = Text.literal("Mozambique here!");
//            player.sendMessage(mes, true);

            // FIXME: play sound
            var world = client.world;
            assert world != null;
            BlockPos soundPos = player.getBlockPos();
            System.out.println(soundPos);
            world.playSound(
                    null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                    soundPos, // The position of where the sound will come from
                    SoundEvents.BLOCK_ANVIL_BREAK, // The sound that will play, in this case, the sound the anvil plays when it lands.
                    SoundCategory.BLOCKS, // This determines which of the volume sliders affect this sound
                    1f, // Volume multiplier, 1 is normal, 0.5 is half volume, etc
                    1f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
            );

            // get the targeted block

            float tickDelta = 1.0f; // TODO test this
            assert client.cameraEntity != null;
            Vec3d cameraDirection = client.cameraEntity.getRotationVec(tickDelta);

            pingDirection(client, player, tickDelta, cameraDirection);

//            // DEBUG: a big curved surface
//            int width = client.getWindow().getScaledWidth();
//            int height = client.getWindow().getScaledHeight();
//            for (int x = 0; x < width; x+=6) {
//                for (int y = 0; y < height; y+=6) {
//                    Vec3d dir = XY2Vec3d(new Vector2i(x, y));
//                    Objects.requireNonNull(dir).normalize();
//                    double dist = 25;
//                    pingDirDistance(client, player, tickDelta, dir, dist);
//                }
//            }
        }
    }

    private static void pingDirDistance(MinecraftClient client, ClientPlayerEntity player, float tickDelta, Vec3d dir, double dist) {
        assert client.cameraEntity != null;
        Vec3d cameraPos = client.cameraEntity.getPos();
        Vec3d pingPos = cameraPos.add(dir.multiply(dist));
        PingPoint p = new PingPoint(pingPos, player.getEntityName());
        addPointToRenderer(p);
    }

    private static void pingDirection(MinecraftClient client, ClientPlayerEntity player, float tickDelta, Vec3d dir) {
        assert client.cameraEntity != null;
        HitResult hit = raycast(client.cameraEntity, MAX_REACH, tickDelta, false, dir);

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
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                final Text entityMes = entity.getName();
                player.sendMessage(entityMes, true);
                pingPos = hit.getPos();
            }
        }

        if (pingPos != null) {
            System.out.println("Ping at");
            System.out.println(pingPos);
            PingPoint p = new PingPoint(pingPos, player.getEntityName());
            addPointToRenderer(p);
            sendPingToServer(p);
        }
    }

    private static void addPointToRenderer(PingPoint p) {
        RenderHandler.getInstance().addPing(p);
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

    public static void sendPingToServer(PingPoint p){
        try {
            PacketByteBuf buf = p.toPacketByteBuf();
            ClientPlayNetworking.send(PING_PACKET, buf);
        } catch (IOException e) {
            LOGGER.info("Fail to send ping packet to server", e);
            return;
        }
    }

    public void pingReceiver(PacketByteBuf buf) {
        try{
            var p = PingPoint.fromPacketByteBuf(buf);
            addPointToRenderer(p);
        } catch (Exception e){
            LOGGER.info("Fail to deserialize the ping packet received", e);
            return;
        }
    }
}