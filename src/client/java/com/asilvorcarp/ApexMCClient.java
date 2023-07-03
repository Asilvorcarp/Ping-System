package com.asilvorcarp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static net.minecraft.util.math.RotationAxis.POSITIVE_Y;

public class ApexMCClient implements ClientModInitializer {
    public static final double EXTENDED_REACH = 20.0D;
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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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

                HitResult hit = raycast(client.cameraEntity, EXTENDED_REACH, tickDelta, false, cameraDirection);
                // HitResult hit = client.crosshairTarget;
//                HitResult hit = raycastInDirection(client, tickDelta, cameraDirection);

                switch(Objects.requireNonNull(hit).getType()) {
                    case MISS:
                        //nothing near enough
                        player.sendMessage(Text.literal("Too far"), true);
                        break;
                    case BLOCK:
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos blockPos = blockHit.getBlockPos();
                        BlockState blockState = client.world.getBlockState(blockPos);
                        Block block = blockState.getBlock();
                        final Text blockMes = block.getName();
                        player.sendMessage(blockMes, true);
                        break;
                    case ENTITY:
                        EntityHitResult entityHit = (EntityHitResult) hit;
                        Entity entity = entityHit.getEntity();
                        final Text entityMes = entity.getName();
                        player.sendMessage(entityMes, true);
                        break;
                }


            }

        });
    }

    private static HitResult raycastInDirection(MinecraftClient client, float tickDelta, Vec3d direction) {
        Entity entity = client.getCameraEntity();
        if (entity == null || client.world == null) {
            return null;
        }

        assert client.interactionManager != null;
        double reachDistance = client.interactionManager.getReachDistance(); // Change this to extend the reach
        HitResult target = raycast(entity, reachDistance, tickDelta, false, direction);
        boolean tooFar = false;
        double extendedReach = reachDistance;
        if (client.interactionManager.hasExtendedReach()) {
            extendedReach = EXTENDED_REACH;
            reachDistance = extendedReach;
        } else {
            if (reachDistance > 3.0D) {
                tooFar = true;
            }
        }

        Vec3d cameraPos = entity.getCameraPosVec(tickDelta);

        extendedReach = extendedReach * extendedReach;
        if (target != null) {
            extendedReach = target.getPos().squaredDistanceTo(cameraPos);
        }

        Vec3d vec3d3 = cameraPos.add(direction.multiply(reachDistance));
        Box box = entity
                .getBoundingBox()
                .stretch(entity.getRotationVec(1.0F).multiply(reachDistance))
                .expand(1.0D, 1.0D, 1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
                entity,
                cameraPos,
                vec3d3,
                box,
                (entityx) -> !entityx.isSpectator() && entityx.isCollidable(),
                extendedReach
        );

        if (entityHitResult == null) {
            return target;
        }

        Entity entity2 = entityHitResult.getEntity();
        Vec3d vec3d4 = entityHitResult.getPos();
        double g = cameraPos.squaredDistanceTo(vec3d4);
        if (tooFar && g > 9.0D) {
            return null;
        } else if (g < extendedReach || target == null) {
            target = entityHitResult;
            if (entity2 instanceof LivingEntity || entity2 instanceof ItemFrameEntity) {
                client.targetedEntity = entity2;
            }
        }

        return target;
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