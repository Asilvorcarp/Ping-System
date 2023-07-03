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
import org.joml.Matrix4f;

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

    @Override
    public void onRenderGameOverlayPost(DrawContext drawContext) {
        int winWidth = mc.getWindow().getScaledWidth();
        int winHeight = mc.getWindow().getScaledHeight();
        int x = winWidth / 4;
        int y = winHeight / 4;

        INSTANCE.debug_count++;
        if (INSTANCE.debug_count >= 160) {
            INSTANCE.debug_count = 0;
            System.out.println("fuck overlay post");
            System.out.println(x);
            System.out.println(y);
        }

        for (var ping : this.pings.values()) {
            RenderUtils.bindTexture(PING_BASIC);

//            RenderUtils.drawTexturedRect(0, 0, 0, 0, 128, 128);
            double zLevel = 0;
            var realResizer = ICON_RESIZER / 32;
            float u = 0, v = 0, width = 256 * realResizer, height = 256 * realResizer;
            float pixelWidth = 0.00390625F / realResizer;
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
