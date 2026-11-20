package dev.zyon;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static dev.zyon.ClickLayout.*;

public final class ClickGui extends GuiScreen {
    private static final int TEXT = 0xFFE8F4FF, MUTED = 0xFF91A9C2, LINE = 0xFF203B56;
    private final Zyon client;
    private Feature.Category category = Feature.Category.RENDER;
    private Feature selected, hoveredHelp;
    private GuiTextField search, profile;
    private boolean profilesPage, binding;
    private int listScroll, settingsScroll, profileScroll, originX, originY;
    private float scale;
    private Setting dragging;
    private String status = "";

    public ClickGui(Zyon client) { this.client = client; selected = client.feature("crosshair"); }
    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true);
        String query = search == null ? "" : search.getText(), name = profile == null ? "principal" : profile.getText();
        scale = ClickLayout.scale(width, height);
        originX = (int)((width - WIDTH * scale) / 2); originY = (int)((height - HEIGHT * scale) / 2);
        search = new GuiTextField(1, fontRendererObj, 549, 33, 214, 18);
        search.setMaxStringLength(48); search.setEnableBackgroundDrawing(false); search.setTextColor(TEXT); search.setText(query);
        profile = new GuiTextField(2, fontRendererObj, 202, 137, 250, 18);
        profile.setMaxStringLength(32); profile.setEnableBackgroundDrawing(false); profile.setTextColor(TEXT); profile.setText(name);
    }
    private int mx(int x) { return (int)Math.floor((x - originX) / scale); }
    private int my(int y) { return (int)Math.floor((y - originY) / scale); }
    private void label(String text, int x, int y, int color) { fontRendererObj.drawString(text, x, y, color); }
    private void trim(String text, int x, int y, int maxWidth, int color) { label(fontRendererObj.trimStringToWidth(text, maxWidth), x, y, color); }
    private void card(int x, int y, int w, int h, int color) {
        drawRect(x + 3, y, x + w - 3, y + h, color);
        drawRect(x, y + 3, x + w, y + h - 3, color);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }
    private void panel(int x, int y, int w, int h, int fill, int border) { card(x, y, w, h, border); card(x + 1, y + 1, w - 2, h - 2, fill); }
    private void button(String text, int x, int y, int w, int h, int mouseX, int mouseY, boolean active) {
        boolean over = contains(mouseX, mouseY, x, y, w, h);
        panel(x, y, w, h, active ? client.accent() : over ? 0xFF183B58 : 0xFF11253A, active || over ? client.accent() : LINE);
        label(text, x + (w - fontRendererObj.getStringWidth(text)) / 2, y + (h - 8) / 2, active ? 0xFF031629 : TEXT);
    }
    private void helpButton(Feature feature, int left, int top, int x, int y, boolean visible) {
        boolean over = visible && contains(x, y, left, top, HELP_SIZE, HELP_SIZE);
        panel(left, top, HELP_SIZE, HELP_SIZE, over ? client.accent() : 0xFF102E48, over ? 0xFF92DAFF : 0xFF2B5274);
        label("?", left + 6, top + 5, over ? 0xFF031629 : 0xFF79C7FF);
        if (over) hoveredHelp = feature;
    }
    private List<Feature> visible() {
        List<Feature> result = new ArrayList<Feature>();
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        for (Feature feature : client.features)
            if (query.isEmpty() ? feature.category == category : (feature.name + " " + feature.description + " " + feature.category.label).toLowerCase(Locale.ROOT).contains(query)) result.add(feature);
        return result;
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partial) {
        if (mc.theWorld == null) drawGradientRect(0, 0, width, height, 0xFF07182B, 0xFF020A13);
        else drawRect(0, 0, width, height, 0xB0030912);
        int x = mx(mouseX), y = my(mouseY); hoveredHelp = null;
        if (dragging != null) updateSlider(x);
        GL11.glPushMatrix(); GL11.glTranslatef(originX, originY, 0); GL11.glScalef(scale, scale, 1);
        try {
            int opacity = client.feature("theme").setting("opacity").integer();
            panel(0, 0, WIDTH, HEIGHT, (opacity << 24) | 0x081421, 0xFF285175);
            drawRect(148, 1, WIDTH - 4, 3, client.accent());
            drawSidebar(x, y, opacity);
            label("PAINEL DO CLIENTE", 168, 18, MUTED);
            label("\u00a7l" + (profilesPage ? "Seus perfis" : search.getText().trim().isEmpty() ? category.label : "Resultados da busca"), 168, 35, TEXT);
            label(profilesPage ? "Guarde seus ajustes e alterne entre configuracoes." : "Escolha uma funcao e personalize ao seu lado.", 168, 53, MUTED);
            if (profilesPage) drawProfiles(x, y);
            else {
                panel(534, 22, 246, 32, 0xFF0D2033, search.isFocused() ? client.accent() : LINE);
                if (search.getText().isEmpty() && !search.isFocused()) label("Buscar funcoes...", 549, 34, MUTED);
                search.drawTextBox();
                label("FUNCOES", LIST_X, 81, MUTED);
                String count = visible().size() + " disponiveis"; label(count, 418 - fontRendererObj.getStringWidth(count), 81, MUTED);
                drawModules(x, y); drawInspector(x, y);
            }
            drawRect(168, 427, 780, 428, LINE);
            int enabled = 0; for (Feature feature : client.features) if (feature.enabled()) enabled++;
            trim(status.isEmpty() ? enabled + " ativos  |  Ajustes salvos no seu perfil" : status, 168, 443, 486, status.isEmpty() ? MUTED : client.accent());
            label("ESC: fechar", 708, 443, MUTED);
            if (hoveredHelp != null && dragging == null) drawHelp(hoveredHelp, x, y);
        } finally { GL11.glPopMatrix(); }
    }
    private void drawSidebar(int x, int y, int opacity) {
        card(1, 1, 146, HEIGHT - 2, (opacity << 24) | 0x0B1C2E);
        drawRect(147, 15, 148, HEIGHT - 15, LINE);
        Branding.image(Branding.LOGO, 18, 20, 32, 32);
        label("\u00a7lZYON", 59, 23, TEXT); label("NEON / 1.8.9", 59, 39, MUTED);
        drawRect(18, 69, 129, 70, LINE); label("CATEGORIAS", 21, 85, MUTED);
        String[] icons = {"V", "H", "I"};
        for (Feature.Category item : Feature.Category.values()) {
            int top = 105 + item.ordinal() * 40;
            boolean active = !profilesPage && item == category;
            if (active || contains(x, y, 12, top, 124, 32)) card(12, top, 124, 32, active ? 0xFF143B5B : 0xFF102A42);
            if (active) drawRect(12, top + 7, 14, top + 25, client.accent());
            card(23, top + 7, 20, 20, active ? client.accent() : 0xFF1B344D);
            label(icons[item.ordinal()], 30, top + 13, active ? 0xFF041629 : MUTED);
            label(item.label, 51, top + 13, active ? TEXT : MUTED);
        }
        drawRect(18, 312, 130, 313, LINE);
        button("Meus perfis", 18, 324, 112, 28, x, y, profilesPage);
        button("Editar HUD", 18, 359, 112, 28, x, y, false);
        button("Ocultar extras", 18, 394, 112, 28, x, y, false);
        label("END: ocultar", 25, 442, MUTED);
    }
    private void drawModules(int x, int y) {
        List<Feature> list = visible(); listScroll = clampScroll(listScroll, list.size(), ROW, ROW - CARD_H, LIST_H);
        int hovered = moduleIndex(x, y, listScroll, list.size());
        clip(LIST_X, LIST_Y, LIST_W, LIST_H);
        try {
            for (int i = 0; i < list.size(); i++) {
                int top = LIST_Y + i * ROW - listScroll;
                if (top + CARD_H <= LIST_Y || top >= LIST_Y + LIST_H) continue;
                Feature feature = list.get(i);
                panel(LIST_X, top, LIST_W, CARD_H, selected == feature ? 0xFF12334D : hovered == i ? 0xFF112D44 : 0xFF0E2336, selected == feature ? 0xFF2375AA : LINE);
                if (selected == feature) drawRect(LIST_X, top + 10, LIST_X + 2, top + CARD_H - 10, client.accent());
                trim(feature.name, 182, top + 10, 150, feature.enabled() ? client.accent() : TEXT);
                trim(feature.id.equals("theme") ? "Sempre aplicado" : feature.key == 0 ? "Sem atalho" : "Tecla: " + Keyboard.getKeyName(feature.key), 182, top + 30, 153, MUTED);
                helpButton(feature, HELP_X, top + 16, x, y, hovered == i);
                if (!feature.id.equals("theme")) {
                    card(TOGGLE_X, top + 17, 30, 16, feature.enabled() ? client.accent() : 0xFF3A4D62);
                    card(TOGGLE_X + (feature.enabled() ? 16 : 2), top + 19, 12, 12, 0xFFE8F4FF);
                }
            }
            if (list.isEmpty()) { label("Nada encontrado.", 184, 126, TEXT); label("Tente outro nome na busca.", 184, 144, MUTED); }
        } finally { unclip(); }
        scrollbar(423, LIST_Y, LIST_H, list.size() * ROW - (ROW - CARD_H), listScroll);
    }
    private void drawInspector(int x, int y) {
        panel(434, 78, 346, 332, 0xFF0D2032, LINE);
        if (selected == null) { label("Selecione uma funcao", 450, 102, TEXT); label("Os ajustes aparecerao aqui.", 450, 124, MUTED); return; }
        trim("\u00a7l" + selected.name, 450, 98, 224, TEXT);
        helpButton(selected, 684, 93, x, y, true);
        if (selected.id.equals("theme")) label("FIXO", 730, 98, client.accent());
        else button(selected.enabled() ? "ATIVO" : "ATIVAR", 712, 90, 52, 24, x, y, selected.enabled());
        List<String> desc = fontRendererObj.listFormattedStringToWidth(selected.description, 310);
        for (int i = 0; i < Math.min(2, desc.size()); i++) label(desc.get(i), 450, 126 + i * 12, MUTED);
        label("GERAL", 450, 163, MUTED); label("Atalho de teclado", 450, 190, TEXT);
        if (selected.id.equals("theme")) label("Sempre aplicado", 674, 190, MUTED);
        else button(binding ? "Pressione uma tecla" : selected.key == 0 ? "Definir atalho" : Keyboard.getKeyName(selected.key), 620, 181, 144, 26, x, y, binding);
        drawRect(450, 218, 764, 219, LINE); label("AJUSTES", 450, 226, MUTED);
        String count = selected.settings.size() + " opcoes"; label(count, 764 - fontRendererObj.getStringWidth(count), 226, MUTED);
        settingsScroll = clampScroll(settingsScroll, selected.settings.size(), SETTING_ROW, 10, SETTINGS_H);
        clip(SETTINGS_X, SETTINGS_Y, SETTINGS_W, SETTINGS_H);
        try {
            if (selected.settings.isEmpty()) {
                label("Sem ajustes adicionais.", 450, 258, TEXT);
                label("Use a ativacao e o atalho acima.", 450, 277, MUTED);
            }
            for (int i = 0; i < selected.settings.size(); i++) {
                Setting setting = selected.settings.get(i); int top = SETTINGS_Y + i * SETTING_ROW - settingsScroll;
                if (top + 42 <= SETTINGS_Y || top >= SETTINGS_Y + SETTINGS_H) continue;
                trim(setting.label, 450, top, setting.kind == Setting.Kind.NUMBER ? 238 : 300, TEXT);
                if (setting.kind == Setting.Kind.NUMBER) {
                    String value = setting.display(); label(value, 762 - fontRendererObj.getStringWidth(value), top, client.accent());
                    drawRect(452, top + 28, 750, top + 32, 0xFF28435E);
                    int thumb = 452 + (int)(298 * (setting.get() - setting.min) / (setting.max - setting.min));
                    drawRect(452, top + 28, thumb, top + 32, client.accent()); card(thumb - 3, top + 24, 7, 12, 0xFFE5F5FF);
                } else {
                    panel(450, top + 17, 314, 25, 0xFF132E46, LINE);
                    trim(setting.kind == Setting.Kind.BOOLEAN ? setting.on() ? "Ativado" : "Desativado" : setting.display(), 460, top + 26, 260, setting.kind == Setting.Kind.BOOLEAN && !setting.on() ? MUTED : client.accent());
                    label(setting.kind == Setting.Kind.BOOLEAN ? setting.on() ? "[x]" : "[ ]" : ">", 738, top + 26, client.accent());
                }
            }
        } finally { unclip(); }
        scrollbar(771, SETTINGS_Y, SETTINGS_H, selected.settings.size() * SETTING_ROW - 10, settingsScroll);
    }
    private void drawHelp(Feature feature, int x, int y) {
        int w = 300; List<String> lines = fontRendererObj.listFormattedStringToWidth(FeatureHelp.describe(feature), w - 24);
        int h = 40 + lines.size() * 12; int[] box = tooltip(x, y, w, h);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT); GL11.glDisable(GL11.GL_DEPTH_TEST);
        try {
            card(box[0] + 3, box[1] + 4, w, h, 0x99000000);
            panel(box[0], box[1], w, h, 0xFF061522, client.accent());
            label("\u00a7l" + feature.name, box[0] + 12, box[1] + 12, 0xFF87CEFF);
            for (int i = 0; i < lines.size(); i++) label(lines.get(i), box[0] + 12, box[1] + 31 + i * 12, TEXT);
        } finally { GL11.glPopAttrib(); }
    }
    private void scrollbar(int x, int y, int height, int content, int scroll) {
        if (content <= height) return;
        int size = Math.max(16, height * height / content), top = y + (height - size) * scroll / (content - height);
        drawRect(x, y, x + 2, y + height, 0xFF18334C); drawRect(x, top, x + 2, top + size, client.accent());
    }
    private void drawProfiles(int x, int y) {
        panel(168, 78, 612, 332, 0xFF0D2032, LINE);
        label("SALVAR OU CARREGAR", 190, 98, MUTED);
        panel(190, 126, 280, 32, 0xFF112B43, profile.isFocused() ? client.accent() : LINE); profile.drawTextBox();
        button("Salvar perfil", 482, 126, 130, 32, x, y, true); button("Carregar", 624, 126, 140, 32, x, y, false);
        label("PERFIS NESTE COMPUTADOR", 190, 180, MUTED);
        List<String> names = client.profiles.list(); profileScroll = clampScroll(profileScroll, names.size(), 34, 6, 196);
        clip(190, 202, 574, 196);
        try {
            if (names.isEmpty()) label("Crie seu primeiro perfil usando um nome acima.", 200, 218, MUTED);
            for (int i = 0; i < names.size(); i++) {
                int top = 202 + i * 34 - profileScroll;
                if (top + 28 <= 202 || top >= 398) continue;
                button(names.get(i), 190, top, 574, 28, x, y, names.get(i).equalsIgnoreCase(profile.getText()));
            }
        } finally { unclip(); }
        scrollbar(771, 202, 196, names.size() * 34 - 6, profileScroll);
    }
    private void select(Feature feature) { if (selected != feature) settingsScroll = 0; selected = feature; dragging = null; }
    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        int x = mx(mouseX), y = my(mouseY); binding = false;
        if (contains(x, y, 18, 394, 112, 28)) { client.panic(); status = "Recursos visuais ocultados."; return; }
        if (contains(x, y, 18, 359, 112, 28)) { mc.displayGuiScreen(new HudEditor(client, this)); return; }
        if (contains(x, y, 18, 324, 112, 28)) { profilesPage = true; search.setFocused(false); return; }
        for (Feature.Category item : Feature.Category.values()) if (contains(x, y, 12, 105 + item.ordinal() * 40, 124, 32)) {
            category = item; profilesPage = false; search.setText(""); search.setFocused(false); profile.setFocused(false); listScroll = settingsScroll = 0;
            for (Feature feature : client.features) if (feature.category == item) { select(feature); break; }
            return;
        }
        if (profilesPage) { profileClick(x, y); return; }
        search.mouseClicked(x, y, button);
        List<Feature> list = visible(); int index = moduleIndex(x, y, listScroll, list.size());
        if (index >= 0) {
            Feature feature = list.get(index); select(feature);
            int top = LIST_Y + index * ROW - listScroll;
            if (helpHit(x, y, top)) return;
            if (!feature.id.equals("theme") && toggleHit(x, y, top)) { feature.toggle(); client.profiles.saveAutomatic(); }
            return;
        }
        if (selected == null) return;
        if (contains(x, y, 684, 93, HELP_SIZE, HELP_SIZE)) return;
        if (!selected.id.equals("theme") && contains(x, y, 712, 90, 52, 24)) { selected.toggle(); client.profiles.saveAutomatic(); return; }
        if (!selected.id.equals("theme") && contains(x, y, 620, 181, 144, 26)) {
            binding = true; search.setFocused(false); status = "Pressione uma tecla. DELETE: remover; ESC: cancelar."; return;
        }
        if (contains(x, y, SETTINGS_X, SETTINGS_Y, SETTINGS_W, SETTINGS_H)) {
            int offset = y - SETTINGS_Y + settingsScroll, settingIndex = offset / SETTING_ROW;
            if (settingIndex < selected.settings.size() && offset % SETTING_ROW >= 17 && offset % SETTING_ROW < 42) {
                Setting setting = selected.settings.get(settingIndex);
                if (setting.kind == Setting.Kind.NUMBER) { dragging = setting; updateSlider(x); }
                else { setting.cycle(); client.profiles.saveAutomatic(); }
            }
        }
    }
    private void profileClick(int x, int y) {
        profile.mouseClicked(x, y, 0);
        try {
            if (contains(x, y, 482, 126, 130, 32)) { client.profiles.save(profile.getText()); status = "Perfil salvo: " + profile.getText(); }
            else if (contains(x, y, 624, 126, 140, 32)) { client.profiles.load(profile.getText()); client.profiles.saveAutomatic(); status = "Perfil carregado: " + profile.getText(); }
            else if (contains(x, y, 190, 202, 574, 196)) {
                int offset = y - 202 + profileScroll, index = offset / 34; List<String> names = client.profiles.list();
                if (index < names.size() && offset % 34 < 28) { profile.setText(names.get(index)); profile.setFocused(false); }
            }
        } catch (Exception error) { status = error.getMessage() == null ? "Erro no perfil; veja latest.log." : error.getMessage(); }
    }
    private void updateSlider(int x) { double fraction = Math.max(0, Math.min(1, (x - 452) / 298.0)); dragging.set(dragging.min + fraction * (dragging.max - dragging.min)); }
    @Override protected void mouseReleased(int mouseX, int mouseY, int button) { if (dragging != null) { dragging = null; client.profiles.saveAutomatic(); } }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int wheel = Mouse.getEventDWheel(); if (wheel == 0) return;
        int x = mx(Mouse.getEventX() * width / mc.displayWidth), y = my(height - Mouse.getEventY() * height / mc.displayHeight - 1), delta = wheel > 0 ? -28 : 28;
        if (profilesPage && contains(x, y, 190, 202, 574, 196)) profileScroll = clampScroll(profileScroll + delta, client.profiles.list().size(), 34, 6, 196);
        else if (!profilesPage && contains(x, y, LIST_X, LIST_Y, LIST_W, LIST_H)) listScroll = clampScroll(listScroll + delta, visible().size(), ROW, ROW - CARD_H, LIST_H);
        else if (!profilesPage && selected != null && contains(x, y, SETTINGS_X, SETTINGS_Y, SETTINGS_W, SETTINGS_H)) settingsScroll = clampScroll(settingsScroll + delta, selected.settings.size(), SETTING_ROW, 10, SETTINGS_H);
    }
    @Override protected void keyTyped(char typed, int key) throws IOException {
        if (!binding && key == client.panicKey.getKeyCode()) { client.panic(); status = "Recursos visuais ocultados."; return; }
        if (binding) {
            if (key == Keyboard.KEY_ESCAPE) { binding = false; status = "Atalho mantido."; return; }
            if (key == client.menuKey.getKeyCode() || key == client.panicKey.getKeyCode() || key == client.editorKey.getKeyCode() || key == client.zoomKey.getKeyCode()) { status = "Tecla reservada."; return; }
            selected.key = key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? 0 : key; binding = false; status = "Atalho atualizado: " + selected.name; client.profiles.saveAutomatic(); return;
        }
        if (key == Keyboard.KEY_ESCAPE || key == client.menuKey.getKeyCode()) { mc.displayGuiScreen(null); return; }
        if (profilesPage) profile.textboxKeyTyped(typed, key);
        else if (search.textboxKeyTyped(typed, key)) { listScroll = 0; List<Feature> list = visible(); if (!list.contains(selected)) select(list.isEmpty() ? null : list.get(0)); }
    }
    @Override public void updateScreen() { search.updateCursorCounter(); profile.updateCursorCounter(); }
    @Override public void onGuiClosed() { dragging = null; Keyboard.enableRepeatEvents(false); client.profiles.saveAutomatic(); }
    @Override public boolean doesGuiPauseGame() { return false; }
    private void clip(int x, int y, int w, int h) {
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT); GL11.glEnable(GL11.GL_SCISSOR_TEST);
        double factor = mc.displayWidth / (double)width;
        int left = (int)Math.floor((originX + x * scale) * factor), right = (int)Math.ceil((originX + (x + w) * scale) * factor);
        int bottom = mc.displayHeight - (int)Math.ceil((originY + (y + h) * scale) * factor), top = mc.displayHeight - (int)Math.floor((originY + y * scale) * factor);
        GL11.glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
    }
    private void unclip() { GL11.glPopAttrib(); }
}
