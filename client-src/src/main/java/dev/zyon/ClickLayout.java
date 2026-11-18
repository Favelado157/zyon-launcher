package dev.zyon;

/** Shared virtual coordinates keep drawing, clipping and hit testing consistent. */
public final class ClickLayout {
    public static final int WIDTH = 800, HEIGHT = 464;
    public static final int LIST_X = 168, LIST_Y = 102, LIST_W = 250, LIST_H = 308;
    public static final int ROW = 58, CARD_H = 50, HELP_X = 344, HELP_SIZE = 18, TOGGLE_X = 376;
    public static final int SETTINGS_X = 450, SETTINGS_Y = 240, SETTINGS_W = 314, SETTINGS_H = 158, SETTING_ROW = 52;
    private ClickLayout() {}
    public static boolean contains(int x, int y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }
    public static float scale(int width, int height) {
        return Math.max(.01f, Math.min(1.5f, Math.min((width - 16f) / WIDTH, (height - 16f) / HEIGHT)));
    }
    public static int maxScroll(int count, int row, int gap, int height) { return Math.max(0, count * row - gap - height); }
    public static int clampScroll(int value, int count, int row, int gap, int height) { return Math.max(0, Math.min(value, maxScroll(count, row, gap, height))); }
    public static int moduleIndex(int x, int y, int scroll, int count) {
        if (!contains(x, y, LIST_X, LIST_Y, LIST_W, LIST_H)) return -1;
        int offset = y - LIST_Y + scroll, index = offset / ROW;
        return index >= 0 && index < count && offset % ROW < CARD_H ? index : -1;
    }
    public static boolean helpHit(int x, int y, int rowTop) { return contains(x, y, HELP_X, rowTop + 16, HELP_SIZE, HELP_SIZE); }
    public static boolean toggleHit(int x, int y, int rowTop) { return contains(x, y, TOGGLE_X, rowTop + 17, 30, 16); }
    public static int[] tooltip(int x, int y, int width, int height) {
        int left = Math.max(10, Math.min(x + 14, WIDTH - width - 10));
        int top = y + 18;
        if (top + height > HEIGHT - 10) top = y - height - 12;
        return new int[] {left, Math.max(10, Math.min(top, HEIGHT - height - 10))};
    }
}
