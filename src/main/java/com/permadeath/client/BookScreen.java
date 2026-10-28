package com.permadeath.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.permadeath.Change;
import com.permadeath.PermadeathMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Libro viejo con los cambios de dificultad activos. Se abre con G. */
public class BookScreen extends Screen {
    private static final ResourceLocation BOOK =
            new ResourceLocation(PermadeathMod.MODID, "textures/gui/book.png");

    private static final int BOOK_W = 256;
    private static final int BOOK_H = 168;

    private static final float TEXT_SCALE = 0.8F;
    private static final int PAGE_W = 96;
    private static final int PAGE_H = 128;
    private static final int LEFT_X = 18;
    private static final int RIGHT_X = 139;
    private static final int PAGE_Y = 16;

    private static final int INK = 0x3B2410;
    private static final int INK_TITLE = 0x6B1010;

    private static final class Entry {
        final Component title;
        final List<FormattedCharSequence> lines;
        final int height;

        Entry(Component title, List<FormattedCharSequence> lines) {
            this.title = title;
            this.lines = lines;
            this.height = 11 + lines.size() * 9 + 6;
        }
    }

    private final List<List<Entry>> pages = new ArrayList<>();
    private int spread = 0;
    private float scale = 1.0F;
    private int left;
    private int top;
    private Button prevButton;
    private Button nextButton;

    public BookScreen() {
        super(Component.literal("Permadeath"));
    }

    @Override
    protected void init() {
        scale = Math.min(width * 0.92F / BOOK_W, height * 0.80F / BOOK_H);
        left = Math.round((width - BOOK_W * scale) / 2.0F);
        top = Math.round((height - BOOK_H * scale) / 2.0F) - 8;
        buildPages();
        spread = Math.min(spread, maxSpread());

        int buttonY = top + Math.round(BOOK_H * scale) + 6;
        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> flip(-1))
                .bounds(width / 2 - 62, buttonY, 40, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> flip(1))
                .bounds(width / 2 + 22, buttonY, 40, 20).build());
        updateButtons();
    }

    private void buildPages() {
        pages.clear();
        int wrapWidth = Math.round(PAGE_W / TEXT_SCALE);
        int capacity = Math.round(PAGE_H / TEXT_SCALE) - 14;

        List<Change> active = ClientState.active;
        List<Entry> all = new ArrayList<>();
        all.add(new Entry(Component.literal("Cambios activos: " + active.size()).withStyle(ChatFormatting.BOLD),
                font.split(Component.literal("Cada día de Minecraft se suma uno nuevo."), wrapWidth)));
        if (active.isEmpty()) {
            all.add(new Entry(Component.literal("Nada por ahora").withStyle(ChatFormatting.BOLD),
                    font.split(Component.literal("Todavía no hay cambios de dificultad activos."), wrapWidth)));
        }
        for (Change c : active) {
            all.add(new Entry(Component.literal(c.title).withStyle(ChatFormatting.BOLD),
                    font.split(Component.literal(c.description), wrapWidth)));
        }

        List<Entry> page = new ArrayList<>();
        int used = 0;
        for (Entry e : all) {
            if (!page.isEmpty() && used + e.height > capacity) {
                pages.add(page);
                page = new ArrayList<>();
                used = 0;
            }
            page.add(e);
            used += e.height;
        }
        if (!page.isEmpty()) pages.add(page);
    }

    private int maxSpread() {
        return Math.max(0, (pages.size() - 1) / 2);
    }

    private void flip(int dir) {
        spread = Math.max(0, Math.min(maxSpread(), spread + dir));
        updateButtons();
    }

    private void updateButtons() {
        if (prevButton != null) prevButton.active = spread > 0;
        if (nextButton != null) nextButton.active = spread < maxSpread();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(left, top, 0.0F);
        pose.scale(scale, scale, 1.0F);
        g.blit(BOOK, 0, 0, BOOK_W, BOOK_H, 0.0F, 0.0F, BOOK_W, BOOK_H, BOOK_W, BOOK_H);

        int leftIndex = spread * 2;
        drawPage(g, leftIndex, LEFT_X);
        drawPage(g, leftIndex + 1, RIGHT_X);
        pose.popPose();

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPage(GuiGraphics g, int pageIndex, int x) {
        if (pageIndex >= pages.size()) return;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, PAGE_Y, 0.0F);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        int y = 0;
        for (Entry e : pages.get(pageIndex)) {
            g.drawString(font, e.title, 0, y, INK_TITLE, false);
            y += 11;
            for (FormattedCharSequence line : e.lines) {
                g.drawString(font, line, 0, y, INK, false);
                y += 9;
            }
            y += 6;
        }
        String number = "- " + (pageIndex + 1) + " -";
        int pageBottom = Math.round(PAGE_H / TEXT_SCALE) - 4;
        g.drawString(font, number, Math.round(PAGE_W / TEXT_SCALE / 2.0F) - font.width(number) / 2,
                pageBottom, INK, false);
        pose.popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientModEvents.OPEN_BOOK.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            flip(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            flip(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
