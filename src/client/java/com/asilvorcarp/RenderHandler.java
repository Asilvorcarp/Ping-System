package com.asilvorcarp;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

public class RenderHandler implements IRenderer {
    public static final float ICON_RESIZER = 1.5f; // TODO be able to config this
    private static final RenderHandler INSTANCE = new RenderHandler();
    private final MinecraftClient mc;
    public int debug_count;
    public Map<String, PingPoint> pings;
    public boolean singlePingEach;
    private static final Identifier PING_BASIC = new Identifier(ApexMC.MOD_ID, "textures/ping/ping_basic.png");

    public RenderHandler() {
        this.mc = MinecraftClient.getInstance();
        this.debug_count = 0;
        this.pings = new HashMap<>();
        this.singlePingEach = true;
    }

    public static RenderHandler getInstance() {
        return INSTANCE;
    }

    public void setPing(PingPoint p) {
        if (singlePingEach) {
            pings.put(p.owner, p);
        } else {
            // TODO (later) more than one ping point for each player
        }
    }

    @Override
    public void onRenderWorldLast(MatrixStack matrixStack, Matrix4f projMatrix) {
        if (this.mc.world != null && this.mc.player != null && !this.mc.options.hudHidden) {
            this.renderOverlays(matrixStack, projMatrix, this.mc);
        }
    }

    public static Quaternionf getDegreesQuaternion(Vector3f vector, float degrees) {
        return new Quaternionf().fromAxisAngleDeg(vector, degrees);
    }

    private static Vec3d map(double anglePerPixel, Vec3d cameraDir, Vector3f horizontalRotationAxis,
                             Vector3f verticalRotationAxis, int x, int y, int width, int height) {
        float horizontalRotation = (float) ((x - width / 2f) * anglePerPixel);
        float verticalRotation = (float) ((y - height / 2f) * anglePerPixel);

        final Vector3f temp2 = Vec3dToV3f(cameraDir);
        Quaternionfc rot1 = getDegreesQuaternion(verticalRotationAxis, verticalRotation);
        Quaternionfc rot2 = getDegreesQuaternion(horizontalRotationAxis, horizontalRotation);
        temp2.rotate(rot1);
        temp2.rotate(rot2);
        return new Vec3d(temp2);
    }

    private static Vector2d mapBack(double anglePerPixel, Vec3d cameraDir, Vector3f horizontalRotationAxis,
                                    Vector3f verticalRotationAxis, Vec3d targetDir, int width, int height) {
        // TODO implement
        return new Vector2d(10, 10);
    }

    private static Vector3f Vec3dToV3f(Vec3d v) {
        var ret = new Vector3f();
        ret.x = (float) v.x;
        ret.y = (float) v.y;
        ret.z = (float) v.z;
        return ret;
    }

    @Override
    public void onRenderGameOverlayPost(DrawContext drawContext) {
        for (var ping : this.pings.values()) {
            // TODO get cx cy on camera

            MinecraftClient client = MinecraftClient.getInstance();
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();
            assert client.cameraEntity != null;
            Vec3d cameraPos = client.cameraEntity.getPos();
            Vec3d targetDir = ping.pos.subtract(cameraPos);
            Vec3d cameraDirection = client.cameraEntity.getRotationVec(1.0f);
            double fov = client.options.getFov().getValue();
            double angleSize = fov / height;

            Vector2d v2 = getIconCenter2(width, height, targetDir, cameraDirection, angleSize);
//            System.out.println(v2);

            renderIconHUD((int) v2.x, (int) v2.y, ping);
        }
    }

    @NotNull
    private Vector2d getIconCenter(int width, int height, Vec3d targetDir, Vec3d cameraDirection, double angleSize) {
        Vector3f verticalRotationAxis = Vec3dToV3f(cameraDirection);
        verticalRotationAxis.cross(new Vector3f(0, 1, 0));
        verticalRotationAxis.normalize();
        Vector3f horizontalRotationAxis = Vec3dToV3f(cameraDirection);
        horizontalRotationAxis.cross(verticalRotationAxis);
        horizontalRotationAxis.normalize();
        verticalRotationAxis = Vec3dToV3f(cameraDirection);
        verticalRotationAxis.cross(horizontalRotationAxis);

        var v2 = mapBack(angleSize, cameraDirection, horizontalRotationAxis, verticalRotationAxis,
                targetDir, width, height);
        return v2;
    }

    private Vector2d getIconCenter2(int width, int height, Vec3d targetDir, Vec3d cameraDirection, double angleSize) {
        Vector3f verticalRotationAxis = Vec3dToV3f(cameraDirection);
        verticalRotationAxis.cross(new Vector3f(0, 1, 0));
        verticalRotationAxis.normalize();
        Vector3f horizontalRotationAxis = Vec3dToV3f(cameraDirection);
        horizontalRotationAxis.cross(verticalRotationAxis);
        horizontalRotationAxis.normalize();
        verticalRotationAxis = Vec3dToV3f(cameraDirection);
        verticalRotationAxis.cross(horizontalRotationAxis);
        cameraDirection.normalize();

        Vector2d ret = new Vector2d(10, 10);
        double minTheta = 90;

        // FIXME!!! need to abandon this bad idea
        outerLoop:
        for (int x = 0; x < width; x += 4) {
            for (int y = 0; y < height; y += 4) {
                Vec3d tarHat = map(angleSize, cameraDirection, horizontalRotationAxis, verticalRotationAxis, x, y, width, height).normalize();
                var theta = Math.acos(tarHat.dotProduct(targetDir) / (tarHat.length() * targetDir.length()));
                theta = Math.toDegrees(theta);
                if (theta < minTheta) {
                    minTheta = theta;
                    ret.x = x;
                    ret.y = y;
                }
                if (theta <= 1) {
                    break outerLoop;
                }
            }
        }

//        System.out.println(ret);

//        Vec3d tarHat1 = map(angleSize, cameraDirection, horizontalRotationAxis, verticalRotationAxis, 0, 0, width, height);
//        Vec3d tarHat2 = map(angleSize, cameraDirection, horizontalRotationAxis, verticalRotationAxis, width / 2, height / 2, width, height);
//        System.out.println(tarHat1);
//        System.out.println(tarHat2);

        return ret;
    }

    /**
     * cx, cy: center of the icon
     * ping: the PingPoint
     */
    private void renderIconHUD(int cx, int cy, PingPoint ping) {
        // TODO show distance and owner
        double zLevel = 0;
        var realResizer = ICON_RESIZER / 32;
        float u = 0, v = 0, width = 256 * realResizer, height = 256 * realResizer;
        float pixelWidth = 0.00390625F / realResizer;

        int x = (int) (cx - width / 2);
        int y = (int) (cy - height / 2);

        INSTANCE.debug_count++;
        if (INSTANCE.debug_count >= 160) {
            INSTANCE.debug_count = 0;
            System.out.println("fuck overlay post");
            System.out.println(x);
            System.out.println(y);
        }

        // TODO add background
        RenderUtils.bindTexture(PING_BASIC);

//            RenderUtils.drawTexturedRect(0, 0, 0, 0, 128, 128);

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.applyModelViewMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderUtils.setupBlend();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(x, y + height, zLevel).texture(u * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y + height, zLevel).texture((u + width) * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y, zLevel).texture((u + width) * pixelWidth, v * pixelWidth).next();
        buffer.vertex(x, y, zLevel).texture(u * pixelWidth, v * pixelWidth).next();

        tessellator.draw();
    }

    public void renderOverlays(MatrixStack matrixStack, Matrix4f projMatrix, MinecraftClient mc) {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null) {
            return;
        }

        for (var ping : this.pings.values()) {
            highlightPing(ping, mc);
        }
    }

    private static void highlightPing(PingPoint ping, MinecraftClient mc) {
        // TODO show owner name
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double x = ping.pos.x - cameraPos.x;
        double y = ping.pos.y - cameraPos.y;
        double z = ping.pos.z - cameraPos.z;

        assert mc.player != null;
        double size = 0.3;
        double minX = x - size / 2;
        double minY = y - size / 2;
        double minZ = z - size / 2;
        double maxX = x + size / 2;
        double maxY = y + size / 2;
        double maxZ = z + size / 2;
        Color4f color = ping.color;

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
        RenderUtils.setupBlend();
        RenderUtils.color(1f, 1f, 1f, 1f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.applyModelViewMatrix();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color, 0.3f), buffer);

        tessellator.draw();

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color, 1f), buffer);

        tessellator.draw();

        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public void updateData(MinecraftClient mc) {
        // do nothing?
    }
}
