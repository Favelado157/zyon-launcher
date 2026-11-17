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
    private static final ResourceLocation BACKGROUND = new ResourceLocation("zyon", "textures/menu.jpg");
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
            int left = Math.max(18, width / 12), top = Math.max(95, height / 2 - 15), w = Math.min(210, width - left - 20);
            buttonList.clear();
            buttonList.add(new MenuButton(1, left, top, w, "Um jogador"));
            buttonList.add(new MenuButton(2, left, top + 29, w, "Multijogador"));
            buttonList.add(new MenuButton(3, left, top + 58, w / 2 - 3, "Opcoes"));
            buttonList.add(new MenuButton(4, left + w / 2 + 3, top + 58, w / 2 - 3, "Menu Zyon"));
            buttonList.add(new MenuButton(6, left, top + 87, w / 2 - 3, "Mods"));
            buttonList.add(new MenuButton(5, left + w / 2 + 3, top + 87, w / 2 - 3, "Sair"));
        }
        @Override public void drawScreen(int mouseX, int mouseY, float partial) {
            image(BACKGROUND, 0, 0, width, height);
            drawRect(0, 0, width, height, 0x55100B23);
            int left = Math.max(18, width / 12), logoY = Math.max(15, Math.max(95, height / 2 - 15) - 77);
            image(LOGO, left, logoY, 43, 43);
            GlStateManager.pushMatrix(); GlStateManager.translate(left + 52, logoY + 8, 0); GlStateManager.scale(2, 2, 1);
            fontRendererObj.drawStringWithShadow("ZYON", 0, 0, 0xFFE3D6FF); GlStateManager.popMatrix();
            fontRendererObj.drawStringWithShadow("MINECRAFT JAVA 1.8.9", left + 53, logoY + 31, 0xFFC5B7E0);
            fontRendererObj.drawStringWithShadow("Seu mundo. Seu cliente.", left, logoY + 57, 0xFFD0C5E5);
            super.drawScreen(mouseX, mouseY, partial);
            fontRendererObj.drawStringWithShadow("Zyon Client 1.1.0  /  Forge", 14, height - 16, 0xFFCEBFDE);
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
    private static final class MenuButton extends GuiButton {
        MenuButton(int id, int x, int y, int width, String name) { super(id, x, y, width, 23, name); }
        @Override public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, hovered ? 0xED5C477D : 0xD0241B37);
            drawRect(xPosition, yPosition, xPosition + 2, yPosition + height, hovered ? Zyon.instance.accent() : 0xFF77608E);
            drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + 8, enabled ? 0xFFF3EBFF : 0xFF81758D);
        }
    }
}
