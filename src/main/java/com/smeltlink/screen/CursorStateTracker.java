package com.smeltlink.screen;

import java.lang.reflect.Field;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;

/**
 * Preserves the exact raw mouse cursor position when transitioning between
 * the individual furnace GUI and the SmeltLink Share Mode GUI.
 * <p>
 * Tracks the origin Screen instance where the switch button was clicked so
 * pending coordinates are NEVER consumed or cleared while still rendering
 * the old screen waiting for the server's OpenScreen packet.
 */
public final class CursorStateTracker {

    private static double savedRawX = -1;
    private static double savedRawY = -1;
    private static long captureTimeMs = 0;
    private static Screen originScreen = null;
    private static int remainingRestoreFrames = 0;

    private static Field XPOS_FIELD;
    private static Field YPOS_FIELD;

    static {
        try {
            XPOS_FIELD = MouseHandler.class.getDeclaredField("xpos");
            XPOS_FIELD.setAccessible(true);
            YPOS_FIELD = MouseHandler.class.getDeclaredField("ypos");
            YPOS_FIELD.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    private CursorStateTracker() { }

    public static void captureBeforeSwitch() {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = (mc != null && mc.gui != null) ? mc.gui.screen() : null;
        captureBeforeSwitch(currentScreen);
    }

    public static void captureBeforeSwitch(Screen currentScreen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.mouseHandler != null) {
            savedRawX = mc.mouseHandler.xpos();
            savedRawY = mc.mouseHandler.ypos();
            captureTimeMs = System.currentTimeMillis();
            originScreen = currentScreen != null ? currentScreen : ((mc.gui != null) ? mc.gui.screen() : null);
            remainingRestoreFrames = 3;
        }
    }

    public static void restoreIfPending(Screen activeScreen) {
        if (savedRawX < 0 || savedRawY < 0 || remainingRestoreFrames <= 0) return;

        long elapsed = System.currentTimeMillis() - captureTimeMs;
        if (elapsed > 2000) {
            clear();
            return;
        }

        // Do NOT consume or clear while we are still on the old screen waiting for the server packet!
        if (activeScreen == null || activeScreen == originScreen) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null && mc.mouseHandler != null) {
            InputConstants.releaseMouse(mc.getWindow(), savedRawX, savedRawY);
            try {
                if (XPOS_FIELD != null && YPOS_FIELD != null) {
                    XPOS_FIELD.setDouble(mc.mouseHandler, savedRawX);
                    YPOS_FIELD.setDouble(mc.mouseHandler, savedRawY);
                }
            } catch (Throwable ignored) {
            }

            remainingRestoreFrames--;
            if (remainingRestoreFrames <= 0) {
                clear();
            }
        }
    }

    public static void clear() {
        savedRawX = -1;
        savedRawY = -1;
        captureTimeMs = 0;
        originScreen = null;
        remainingRestoreFrames = 0;
    }
}
