// Java / Branding.java / Java 8 / Minecraft Forge 1.8.9
package dev.zyon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.GuiModList;
import org.lwjgl.opengl.Display;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class Branding {
    public static final ResourceLocation LOGO = new ResourceLocation("zyon", "textures/brand.png");
    private static final ResourceLocation BACKGROUND = new ResourceLocation("zyon", "textures/menu.png");
    @SubscribeEvent public void open(GuiOpenEvent event) {
        if (event.gui instanceof GuiMainMenu && Zyon.instance.feature("theme").flag("menu")) event.gui = new MainMenu();
    }
    public static void image(ResourceLocation resource, int x, int y, int width, int height) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
        GlStateManager.color(1, 1, 1, 1); GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
    }
    public static void applyWindowIcons() {
        if (!Display.isCreated()) return;
        try {
            ByteBuffer[] buffers = new ByteBuffer[2]; int[] sizes = {16, 32};
            for (int i = 0; i < sizes.length; i++) {
                try (InputStream in = Branding.class.getResourceAsStream("/assets/zyon/textures/icon" + sizes[i] + ".png")) {
                    BufferedImage image = ImageIO.read(in); ByteBuffer data = ByteBuffer.allocateDirect(sizes[i] * sizes[i] * 4);
                    for (int y = 0; y < sizes[i]; y++) for (int x = 0; x < sizes[i]; x++) {
                        int argb = image.getRGB(x, y); data.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >> 24));
                    }
                    data.flip(); buffers[i] = data;
                }
            }
            Display.setIcon(buffers);
        } catch (Exception e) { org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Icone de janela indisponivel", e); }
    }
    private static final class MainMenu extends GuiScreen {
        @Override public void initGui() {
            int w = Math.max(150, Math.min(240, (int)(width * .28))), left = (width - w) / 2;
            int titleBottom = (int)Math.ceil(215 * Math.max(width / 1672.0, height / 941.0)) + 8;
            int top = Math.max(64, Math.min(height - 116, Math.max(titleBottom, height / 3))), third = (w - 8) / 3;
            buttonList.clear();
            buttonList.add(new GuiButton(1, left, top, w, 20, "Um jogador"));
            buttonList.add(new GuiButton(2, left, top + 24, w, 20, "Multijogador"));
            buttonList.add(new GuiButton(4, left, top + 48, w, 20, "Menu Zyon"));
            buttonList.add(new GuiButton(3, left, top + 84, third, 20, "Opcoes"));
            buttonList.add(new GuiButton(6, left + third + 4, top + 84, third, 20, "Mods"));
            buttonList.add(new GuiButton(5, left + 2 * (third + 4), top + 84, w - 2 * (third + 4), 20, "Sair"));
        }
        @Override public void drawScreen(int mouseX, int mouseY, float partial) {
            // Cover the viewport without stretching. Anchor the crop to the top to preserve the ZYON title on ultrawide displays.
            double cover = Math.max(width / 1672.0, height / 941.0);
            int backgroundWidth = (int)Math.ceil(1672 * cover), backgroundHeight = (int)Math.ceil(941 * cover);
            image(BACKGROUND, (width - backgroundWidth) / 2, 0, backgroundWidth, backgroundHeight);
            super.drawScreen(mouseX, mouseY, partial);
            fontRendererObj.drawStringWithShadow("Minecraft 1.8.9 / Zyon 1.2.1", 4, height - 12, 0xFFE0DCEB);
        }
        @Override protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 1) mc.displayGuiScreen(new GuiSelectWorld(this));
            if (button.id == 2) mc.displayGuiScreen(new GuiMultiplayer(this));
            if (button.id == 3) mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
            if (button.id == 4) mc.displayGuiScreen(new ClickGui(Zyon.instance));
            if (button.id == 5) mc.shutdown();
            if (button.id == 6) mc.displayGuiScreen(new GuiModList(this));
        }
        @Override protected void keyTyped(char typed, int key) { }
        @Override public boolean doesGuiPauseGame() { return false; }
    }
}
