package com.asilvorcarp;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class RenderHandler implements IRenderer {
    private static final RenderHandler INSTANCE = new RenderHandler();
    private final MinecraftClient mc;
    public int debug_count;

    public RenderHandler() {
        this.mc = MinecraftClient.getInstance();
        this.debug_count = 0;
    }

    public static RenderHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void onRenderWorldLast(MatrixStack matrixStack, Matrix4f projMatrix) {
        if (this.mc.world != null && this.mc.player != null && !this.mc.options.hudHidden) {
            this.renderOverlays(matrixStack, projMatrix, this.mc);
        }
    }

    public void renderOverlays(MatrixStack matrixStack, Matrix4f projMatrix, MinecraftClient mc) {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null) {
            return;
        }

        mc.getProfiler().push(() -> "BeaconRangeHeldItem");
        renderBeaconBoxForPlayer(entity, mc);
        mc.getProfiler().pop();

        this.render(entity, matrixStack, projMatrix, mc);
    }

    public void render(Entity entity, MatrixStack matrixStack, Matrix4f projMatrix, MinecraftClient mc) {
//        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        this.update(cameraPos, entity, mc);
//        this.draw(cameraPos, matrixStack, projMatrix, mc);

//        debug_count++;
//        if(debug_count > 165){
//            System.out.println("{{ Render!");
//            debug_count=0;
//        }

        renderBeaconBoxForPlayer(entity, mc);
    }

    private static void renderBeaconBoxForPlayer(Entity entity, MinecraftClient mc) {
        // 83 63 -81
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        var enX = entity.getX();
//        var enY = entity.getY();
//        var enZ = entity.getZ();
        double enX = 83.0, enY = 63.0, enZ = -81.0;
        double x = Math.floor(enX) - cameraPos.x;
        double y = Math.floor(enY) - cameraPos.y;
        double z = Math.floor(enZ) - cameraPos.z;
        // Use the slot number as the level if sneaking
        assert mc.player != null;
//        int level = mc.player.isSneaking() ? Math.min(4, mc.player.getInventory().selectedSlot + 1) : 4;
//        double range = level * 10 + 10;
        double range = 0;
        double minX = x - range;
        double minY = y - range;
        double minZ = z - range;
        double maxX = x + range + 1;
        double maxY = y + range + 1;
        double maxZ = z + range + 1;
//        Color4f color = OverlayRendererBeaconRange.getColorForLevel(level);
        Color4f color = new Color4f(247, 175, 53);

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
