package io.github.moosasharwaan.appliedquartermaster.client;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.me.common.MEStorageMenu;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.network.ReturnToTabletPayload;
import io.github.moosasharwaan.appliedquartermaster.tablet.TabletModuleLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Puts an AE2 terminal opened from the ME Tablet inside the tablet's casing: a dark frame with the purple
 * status bar on top, and a "Tablet" button in the status bar that goes back to the tablet's home screen
 * (Esc does the same). The terminal itself is AE2's own screen, untouched.
 */
@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID, value = Dist.CLIENT)
public final class TerminalFrame {

    private static final int CASE_OUTLINE = 0xFF2B2B2B;
    private static final int FRAME = 0xFF4D4D67;
    private static final int FRAME_LIGHT = 0xFF64647F;
    private static final int PURPLE = 0xFF5A479E;
    private static final int PURPLE_LIGHT = 0xFF915DCD;
    private static final int CHIP = 0xFF7A63C2;
    private static final int TEXT = 0xFFF2F2F2;

    /** Room left of the terminal for AE2's toolbar buttons, and on the right for the upgrade panel. */
    private static final int LEFT = 30;
    private static final int RIGHT = 36;
    private static final int STATUS_H = 12;
    private static final int PAD = 5;

    private TerminalFrame() {
    }

    static boolean isTabletTerminal(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> s
                && s.getMenu() instanceof MEStorageMenu menu
                && menu.getTarget() instanceof ItemMenuHost<?> host
                && host.getLocator() instanceof TabletModuleLocator;
    }

    private record Box(int x0, int y0, int x1, int y1, boolean status) {
    }

    private static Box box(AbstractContainerScreen<?> s) {
        int x0 = Math.max(0, s.getLeftPos() - LEFT);
        int x1 = Math.min(s.width, s.getLeftPos() + s.getImageWidth() + RIGHT);
        boolean status = s.getTopPos() >= STATUS_H + PAD + 2;
        int y0 = Math.max(0, s.getTopPos() - (status ? STATUS_H + PAD + 2 : PAD));
        int y1 = Math.min(s.height, s.getTopPos() + s.getImageHeight() + 18);
        return new Box(x0, y0, x1, y1, status);
    }

    /** The "Tablet" button in the status bar, or null when there is no room for the bar. */
    private static int[] chip(AbstractContainerScreen<?> s, Box b) {
        if (!b.status()) {
            return null;
        }
        var font = Minecraft.getInstance().font;
        int w = font.width(label()) + 14;
        int x = b.x0() + 5;
        int y = b.y0() + 3;
        return new int[]{x, y, x + w, y + STATUS_H - 2};
    }

    private static Component label() {
        return Component.translatable("gui.appliedquartermaster.terminal.back");
    }

    @SubscribeEvent
    public static void onBackground(ScreenEvent.Render.Background event) {
        if (!isTabletTerminal(event.getScreen())) {
            return;
        }
        var s = (AbstractContainerScreen<?>) event.getScreen();
        var b = box(s);
        GuiGraphicsExtractor g = event.getGuiGraphics();
        // Casing: dark outline, frame, slightly lighter inner edge
        g.fill(b.x0() + 2, b.y0(), b.x1() - 2, b.y1(), CASE_OUTLINE);
        g.fill(b.x0(), b.y0() + 2, b.x1(), b.y1() - 2, CASE_OUTLINE);
        g.fill(b.x0() + 2, b.y0() + 1, b.x1() - 2, b.y1() - 1, FRAME);
        g.fill(b.x0() + 1, b.y0() + 2, b.x1() - 1, b.y1() - 2, FRAME);
        g.fill(b.x0() + 2, b.y0() + 1, b.x1() - 2, b.y0() + 2, FRAME_LIGHT);
        if (b.status()) {
            int sy = b.y0() + 2;
            g.fill(b.x0() + 3, sy, b.x1() - 3, sy + STATUS_H, PURPLE);
            g.fill(b.x0() + 3, sy, b.x1() - 3, sy + 1, PURPLE_LIGHT);
            var font = Minecraft.getInstance().font;
            var c = chip(s, b);
            boolean hover = c != null && event.getMouseX() >= c[0] && event.getMouseX() < c[2]
                    && event.getMouseY() >= c[1] && event.getMouseY() < c[3];
            if (c != null) {
                g.fill(c[0], c[1], c[2], c[3], hover ? PURPLE_LIGHT : CHIP);
                // A small "back" arrow
                int ay = c[1] + (c[3] - c[1]) / 2;
                g.fill(c[0] + 3, ay, c[0] + 4, ay + 1, TEXT);
                g.fill(c[0] + 4, ay - 1, c[0] + 5, ay + 2, TEXT);
                g.fill(c[0] + 5, ay - 2, c[0] + 6, ay + 3, TEXT);
                g.text(font, label(), c[0] + 9, c[1] + 1, TEXT, false);
            }
            String title = s.getTitle().getString();
            int tw = font.width(title);
            int tx = (b.x0() + b.x1()) / 2 - tw / 2;
            if (c == null || tx > c[2] + 6) {
                g.text(font, title, tx, sy + 2, TEXT, false);
            }
            // Signal bars on the right
            int bx = b.x1() - 14;
            for (int i = 0; i < 4; i++) {
                g.fill(bx + i * 2, sy + 9 - (i + 1) * 2, bx + i * 2 + 1, sy + 9, TEXT);
            }
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !isTabletTerminal(event.getScreen())) {
            return;
        }
        var s = (AbstractContainerScreen<?>) event.getScreen();
        var c = chip(s, box(s));
        if (c != null && event.getMouseX() >= c[0] && event.getMouseX() < c[2]
                && event.getMouseY() >= c[1] && event.getMouseY() < c[3]) {
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                    .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            ClientPacketDistributor.sendToServer(new ReturnToTabletPayload());
            event.setCanceled(true);
        }
    }
}
