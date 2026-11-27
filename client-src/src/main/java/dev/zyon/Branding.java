// Java / Branding.java / Java 8 / Minecraft 1.8.9
package dev.zyon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import dev.zyon.platform.event.GuiOpenEvent;
import dev.zyon.platform.event.SubscribeEvent;
import org.lwjgl.opengl.Display;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class Branding {
    public static final ResourceLocation LOGO = new ResourceLocation("zyon", "textures/brand.png");
    private static final ResourceLocation BACKGROUND = new ResourceLocation("zyon", "textures/menu.jpg");
    private boolean menuReported;
    @SubscribeEvent public void open(GuiOpenEvent event) {
        if (Display.isCreated()) Display.setTitle("Zyon Client");
        if (event.gui instanceof GuiMainMenu && !menuReported) {
            menuReported = true; System.out.println("ZYON_MENU_READY");
        }
        if (event.gui instanceof GuiMainMenu && Zyon.instance.feature("theme").flag("menu")) event.gui = new MainMenu();
    }
    public static void image(ResourceLocation resource, int x, int y, int width, int height) {
        // External rendering can leave the texture-state cache out of sync.
        GlStateManager.disableTexture2D();
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
        GlStateManager.color(1, 1, 1, 1); GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
    }
    public static void applyWindowIcons() {
        if (!Display.isCreated()) return;
        Display.setTitle("Zyon Client");
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
        private int menuTop() { return Math.max(92, Math.min(height / 2 - 15, height - 146)); }
        @Override public void initGui() {
            int left = Math.max(18, width / 12), top = menuTop(), w = Math.min(210, width - left - 20);
            buttonList.clear();
            buttonList.add(new MenuButton(1, left, top, w, "Um jogador"));
            buttonList.add(new MenuButton(2, left, top + 29, w, "Multijogador"));
            buttonList.add(new MenuButton(3, left, top + 58, w / 2 - 3, "Opcoes"));
            buttonList.add(new MenuButton(4, left + w / 2 + 3, top + 58, w / 2 - 3, "Menu Zyon"));
            buttonList.add(new MenuButton(5, left, top + 87, w, "Sair"));
        }
        @Override public void drawScreen(int mouseX, int mouseY, float partial) {
            double cover = Math.max(width / 1672.0, height / 941.0);
            int bw = (int)Math.ceil(1672 * cover), bh = (int)Math.ceil(941 * cover);
            image(BACKGROUND, (width - bw) / 2, (height - bh) / 2, bw, bh);
            drawRect(0, 0, width, height, 0x44050D1A);
            int left = Math.max(18, width / 12), logoY = Math.max(15, menuTop() - 77);
            image(LOGO, left, logoY, 43, 43);
            GlStateManager.pushMatrix(); GlStateManager.translate(left + 52, logoY + 8, 0); GlStateManager.scale(2, 2, 1);
            fontRendererObj.drawStringWithShadow("ZYON CLIENT", 0, 0, 0xFFE7F7FF); GlStateManager.popMatrix();
            fontRendererObj.drawStringWithShadow("MINECRAFT JAVA 1.8.9", left + 53, logoY + 31, 0xFFA8D8F0);
            fontRendererObj.drawStringWithShadow("Seu mundo. Seu cliente.", left, logoY + 57, 0xFFBBD6E8);
            super.drawScreen(mouseX, mouseY, partial);
            fontRendererObj.drawStringWithShadow("Cliente independente para Minecraft Java", 14, height - 28, 0xFF91A8BC);
            fontRendererObj.drawStringWithShadow("Zyon Client 1.8.9", 14, height - 16, 0xFFBBD6E8);
        }
        @Override protected void actionPerformed(GuiButton button)  {
            if (button.id == 1) mc.displayGuiScreen(new GuiSelectWorld(this));
            if (button.id == 2) mc.displayGuiScreen(new GuiMultiplayer(this));
            if (button.id == 3) mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
            if (button.id == 4) mc.displayGuiScreen(new ClickGui(Zyon.instance));
            if (button.id == 5) mc.shutdown();
        }
        @Override protected void keyTyped(char typed, int key) { }
        @Override public boolean doesGuiPauseGame() { return false; }
    }
    private static final class MenuButton extends GuiButton {
        MenuButton(int id, int x, int y, int width, String name) { super(id, x, y, width, 23, name); }
        @Override public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, hovered ? 0xED123F60 : 0xDB091A2E);
            drawRect(xPosition, yPosition, xPosition + 2, yPosition + height, hovered ? 0xFF75DEFF : 0xFF249BFF);
            drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + 8, enabled ? 0xFFE8F7FF : 0xFF627D92);
        }
    }
}
