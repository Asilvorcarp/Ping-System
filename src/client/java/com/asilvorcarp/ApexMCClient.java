package com.asilvorcarp;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

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
    }

    private static void checkKeyPress(MinecraftClient client) {
        while (pingKeyBinding.wasPressed()) {
            System.out.println("pressed ping key");

            Text mes = Text.literal("Mozambique here!");
            assert client.player != null;
            var player = client.player;
            player.sendMessage(mes, false);

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

            HitResult hit = raycast(client.cameraEntity, MAX_REACH, tickDelta, false, cameraDirection);
            // HitResult hit = client.crosshairTarget;

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
            System.out.println("Ping at");
            System.out.println(pingPos);

            if (pingPos != null) {
                RenderHandler.getInstance().setPing(new PingPoint(pingPos, player.getEntityName()));
            }

        }
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
}