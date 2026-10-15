package com.smeltlink.screen;

import java.util.Locale;

import com.smeltlink.cluster.FurnaceVariant;
import com.smeltlink.menu.SmeltLinkMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Client-side GUI screen for SmeltLink Share Mode with:
 * - Dynamic Share Mode visual button styling (bold gold/green + glowing outline
 * + LINKED badge)
 * - Preserved cursor coordinates on mode switch
 * - Fast scroll wheel adjustment (4 per notch, Shift=8, Ctrl=1) with tactile
 * click sound
 * - True >64 slot count rendering via ContainerData
 */
public class SmeltLinkScreen extends AbstractContainerScreen<SmeltLinkMenu> {

    private static final int TEX_WIDTH = 176;
    private static final int TEX_HEIGHT = 166;

    private final Identifier backgroundTexture;
    private final Identifier litProgressSprite;
    private final Identifier burnProgressSprite;
    private final FurnaceVariant variant;
    private Button returnButton;

    public SmeltLinkScreen(SmeltLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, TEX_WIDTH, TEX_HEIGHT);
        this.variant = menu.getVariant();
        this.backgroundTexture = variant.guiTexture();
        this.litProgressSprite = variant.litProgressSprite();
        this.burnProgressSprite = variant.burnProgressSprite();
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        int btnW = 38;
        int btnH = 14;
        int btnX = this.leftPos + this.imageWidth - btnW - 6;
        int btnY = this.topPos + 5;

        this.returnButton = Button.builder(
                buildShareButtonLabel(menu.getDataClusterSize(), menu.getDataMaxClusterSize()),
                btn -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        CursorStateTracker.captureBeforeSwitch();
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                })
                .bounds(btnX, btnY, btnW, btnH)
                .tooltip(buildShareButtonTooltip(menu.getDataClusterSize(), menu.getDataMaxClusterSize()))
                .build();

        this.addRenderableWidget(this.returnButton);

        // Restore cursor position if transitioning from Single Furnace mode
        CursorStateTracker.restoreIfPending(this);
    }

    private static Component buildShareButtonLabel(int active, int max) {
        ChatFormatting color = (active >= max) ? ChatFormatting.GREEN : ChatFormatting.GOLD;
        return Component.literal("⚡ x" + active).withStyle(color, ChatFormatting.BOLD);
    }

    private static Tooltip buildShareButtonTooltip(int active, int max) {
        return Tooltip.create(
                Component.literal("Mode: ")
                        .append(Component.literal("SHARE LINKED (" + active + " / " + max + ")")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                        .append(Component.literal("\nClick: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("Switch to Single Furnace").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("\nScroll: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("±4 Furnaces (Shift: ±8 | Ctrl: ±1)")
                                .withStyle(ChatFormatting.YELLOW)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        CursorStateTracker.restoreIfPending(this);
        if (this.returnButton != null) {
            int active = menu.getDataClusterSize();
            int max = menu.getDataMaxClusterSize();
            this.returnButton.setMessage(buildShareButtonLabel(active, max));
            this.returnButton.setTooltip(buildShareButtonTooltip(active, max));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            int current = menu.getDataClusterSize();
            int max = Math.min(64, menu.getDataMaxClusterSize());

            int step = 4;
            if (this.minecraft.hasControlDown()) {
                step = 1;
            } else if (this.minecraft.hasShiftDown()) {
                step = 8;
            }

            int direction = scrollY > 0 ? 1 : -1;
            int ticks = Math.max(1, (int) Math.round(Math.abs(scrollY)));
            int next = Mth.clamp(current + direction * step * ticks, 1, max);

            if (next != current) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, next);

                float pitch = 0.85F + 0.40F * ((float) next / (float) Math.max(1, max));
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ── Background & Animated Indicators ───────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        CursorStateTracker.restoreIfPending(this);
        super.extractBackground(graphics, mouseX, mouseY, a);

        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, this.backgroundTexture,
                xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.isLit()) {
            int litProgressHeight = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    this.litProgressSprite,
                    14,
                    14,
                    0,
                    14 - litProgressHeight,
                    xo + 56,
                    yo + 36 + 14 - litProgressHeight,
                    14,
                    litProgressHeight);
        }

        int burnProgressWidth = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        if (burnProgressWidth > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    this.burnProgressSprite,
                    24,
                    16,
                    0,
                    0,
                    xo + 79,
                    yo + 34,
                    burnProgressWidth,
                    16);
        }

        // Draw dynamic Share Mode glowing border around the top-right toggle button
        if (this.returnButton != null) {
            int active = menu.getDataClusterSize();
            int max = menu.getDataMaxClusterSize();
            int borderColor = (active >= max) ? 0xFF00E676 : 0xFFFFD54F;
            graphics.outline(
                    this.returnButton.getX() - 1,
                    this.returnButton.getY() - 1,
                    this.returnButton.getWidth() + 2,
                    this.returnButton.getHeight() + 2,
                    borderColor);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        // Draw a distinct "LINKED" pill badge in the top-left corner so Share Mode is
        // unmistakable
        String tag = "LINKED";
        int tagW = this.font.width(tag);
        int bgX = 6;
        int bgY = 5;
        graphics.fill(bgX, bgY, bgX + tagW + 6, bgY + 11, 0xCC102818);
        graphics.outline(bgX, bgY, tagW + 6, 11, 0xFF00E676);
        graphics.text(this.font, tag, bgX + 3, bgY + 2, 0xFF55FF88, false);
    }

    // ── Tooltips ───────────────────────────────────────────────────────

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        if (relX >= 56 && relX < 56 + 14 && relY >= 36 && relY < 36 + 14) {
            int burning = menu.getDataBurningCount();
            int total = menu.getDataClusterSize();
            graphics.setTooltipForNextFrame(Component.literal("Burning: " + burning + " / " + total), mouseX, mouseY);
        }
    }

    // ── Extended Slot Rendering (>64 Capacity via True Count) ──────────

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot,
            @Nullable String itemCount) {
        if (slot.index >= 0 && slot.index < 3 && !itemStack.isEmpty()) {
            int trueCount = menu.getSlotTrueCount(slot.index);
            if (trueCount <= 0) {
                trueCount = itemStack.getCount();
            }
            itemCount = trueCount > 1 ? formatCount(trueCount) : "";
        }
        super.renderSlotContents(graphics, itemStack, slot, itemCount);
    }

    public static String formatCount(int count) {
        if (count <= 1)
            return "";
        if (count <= 999)
            return String.valueOf(count);
        if (count < 10_000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        return String.format(Locale.ROOT, "%dk", count / 1000);
    }
}