package mitra88.mobesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class RenderESP {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Frustum frustum = new Frustum();

    // Toggleable ESP
    private boolean espEnabled = true;
    private final KeyBinding toggleKey = new KeyBinding("Toggle ESP", Keyboard.KEY_O, "MobESP");

    public RenderESP() {
        ClientRegistry.registerKeyBinding(toggleKey);
    }

    // Handle key toggle properly
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (toggleKey.isPressed()) {
            espEnabled = !espEnabled;
            mc.thePlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText("ESP " + (espEnabled ? "Enabled" : "Disabled"))
            );
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!espEnabled || mc.theWorld == null || mc.thePlayer == null) return;

        // Interpolated player position for smooth rendering
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        double renderX = view.lastTickPosX + (view.posX - view.lastTickPosX) * event.partialTicks;
        double renderY = view.lastTickPosY + (view.posY - view.lastTickPosY) * event.partialTicks;
        double renderZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * event.partialTicks;

        // Update frustum once per frame
        frustum.setPosition(renderX, renderY, renderZ);

        // Setup OpenGL state
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0F);

        // Draw all mobs in a single Tessellator call
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!isTarget(e)) continue;
            if (e.isDead || e.isInvisible()) continue;
            if (!frustum.isBoundingBoxInFrustum(e.getEntityBoundingBox().expand(0.2, 0.2, 0.2))) continue;

            drawESPBoxLines(e, renderX, renderY, renderZ, event.partialTicks, wr);
        }

        tess.draw();

        // Restore OpenGL state
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private boolean isTarget(Entity e) {
        return e instanceof EntityZombie || e instanceof EntitySkeleton || e instanceof EntityEnderman;
    }

    // Add vertices for a single mob (no tess.draw yet)
    private void drawESPBoxLines(Entity e, double renderX, double renderY, double renderZ, float partialTicks, WorldRenderer wr) {
        // Interpolated entity position for smooth movement
        double x = (e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks) - renderX;
        double y = (e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks) - renderY;
        double z = (e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks) - renderZ;

        float r, g, b;
        if (e instanceof EntityZombie) { r = 0f; g = 1f; b = 0f; }
        else if (e instanceof EntitySkeleton) { r = 1f; g = 1f; b = 1f; }
        else { r = 0.6f; g = 0f; b = 1f; }

        double w = e.width / 2.0;
        double h = e.height;

        // Bottom square
        line(wr, x - w, y, z - w, x + w, y, z - w, r, g, b);
        line(wr, x + w, y, z - w, x + w, y, z + w, r, g, b);
        line(wr, x + w, y, z + w, x - w, y, z + w, r, g, b);
        line(wr, x - w, y, z + w, x - w, y, z - w, r, g, b);

        // Top square
        line(wr, x - w, y + h, z - w, x + w, y + h, z - w, r, g, b);
        line(wr, x + w, y + h, z - w, x + w, y + h, z + w, r, g, b);
        line(wr, x + w, y + h, z + w, x - w, y + h, z + w, r, g, b);
        line(wr, x - w, y + h, z + w, x - w, y + h, z - w, r, g, b);

        // Vertical edges
        line(wr, x - w, y, z - w, x - w, y + h, z - w, r, g, b);
        line(wr, x + w, y, z - w, x + w, y + h, z - w, r, g, b);
        line(wr, x + w, y, z + w, x + w, y + h, z + w, r, g, b);
        line(wr, x - w, y, z + w, x - w, y + h, z + w, r, g, b);
    }

    private void line(WorldRenderer wr,
                      double x1, double y1, double z1,
                      double x2, double y2, double z2,
                      float r, float g, float b) {
        wr.pos(x1, y1, z1).color(r, g, b, 1f).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, 1f).endVertex();
    }
}
