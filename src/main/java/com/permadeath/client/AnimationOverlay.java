package com.permadeath.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.permadeath.ModSounds;
import com.permadeath.PermadeathMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Animacion en pantalla: calavera con cuernos + PERMADEATH.
 * Cambio de dificultad: calavera arriba y PERMADEATH debajo, con el nombre del cambio en rojo.
 * Muerte: calavera arriba y abajo "NOMBRE  PERMADEATH".
 */
public final class AnimationOverlay {
    private static final ResourceLocation SKULL =
            new ResourceLocation(PermadeathMod.MODID, "textures/gui/skull.png");
    private static final ResourceLocation LOGO =
            new ResourceLocation(PermadeathMod.MODID, "textures/gui/logo_text.png");

    private static final int SKULL_W = 177, SKULL_H = 235;
    private static final int LOGO_W = 475, LOGO_H = 112;
    private static final int GAP = 14;

    private static final class Anim {
        final boolean death;
        final String text;
        final int duration;

        Anim(boolean death, String text) {
            this.death = death;
            this.text = text;
            this.duration = death ? 120 : 90;
        }
    }

    private static final Deque<Anim> QUEUE = new ArrayDeque<>();
    private static Anim current = null;
    private static int age = 0;

    public static void start(boolean death, String text) {
        QUEUE.add(new Anim(death, text));
    }

    public static boolean isActive() {
        return current != null;
    }

    /** Una vez por tick de cliente. */
    public static void tick() {
        if (current == null) {
            Anim next = QUEUE.poll();
            if (next == null) return;
            current = next;
            age = 0;
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(
                    (next.death ? ModSounds.DEATH : ModSounds.CHANGE).get(), 1.0F, 1.0F));
            return;
        }
        age++;
        if (age >= current.duration) current = null;
    }

    public static void render(GuiGraphics g, float partialTick) {
        Anim anim = current;
        if (anim == null) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = g.guiWidth();
        int sh = g.guiHeight();

        float t = age + partialTick;
        float fadeIn = 12.0F;
        float fadeOut = 20.0F;
        float alpha = 1.0F;
        if (t < fadeIn) alpha = t / fadeIn;
        else if (t > anim.duration - fadeOut) alpha = (anim.duration - t) / fadeOut;
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        // Intensidad del glitch: fuerte al entrar y al salir, con ráfagas en medio.
        float glitch = 0.0F;
        if (t < fadeIn) glitch = 1.0F - t / fadeIn;
        else if (t > anim.duration - fadeOut) glitch = (t - (anim.duration - fadeOut)) / fadeOut;
        int burst = age % 26;
        if (burst < 2) glitch = Math.max(glitch, 0.55F);
        Random rnd = new Random((age / 2) * 7919L + 17L);

        // Oscurecer la pantalla con un tinte rojo muy oscuro.
        int dim = (int) (alpha * 140.0F);
        g.fill(0, 0, sw, sh, (dim << 24) | 0x140005);
        if (t < 5.0F) {
            int flash = (int) ((1.0F - t / 5.0F) * 110.0F);
            g.fill(0, 0, sw, sh, (flash << 24) | 0xB00000);
        }

        // Medidas del bloque en unidades lógicas.
        float nameScale = 8.5F;
        float nameW = 0.0F;
        if (anim.death) nameW = font.width(anim.text) * nameScale;
        float blockW = anim.death ? nameW + 36.0F + LOGO_W : LOGO_W;
        blockW = Math.max(blockW, SKULL_W);
        float blockH = SKULL_H + GAP + LOGO_H;

        float fit = Math.min(sh * 0.70F / blockH, sw * 0.88F / blockW);
        fit = Math.min(fit, 1.6F);

        float pop = 1.0F;
        if (t < 14.0F) {
            float k = 1.0F - t / 14.0F;
            pop = 1.0F + 0.35F * k * k * k;
        }
        float breathe = 1.0F + 0.012F * Mth.sin(t * 0.15F);
        float scale = fit * pop * breathe;

        float shakeX = (rnd.nextFloat() - 0.5F) * 10.0F * glitch;
        float shakeY = (float) Math.sin(t * 0.9F) * 0.8F;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(sw / 2.0F + shakeX, sh / 2.0F - 6.0F + shakeY, 0.0F);
        pose.scale(scale, scale, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float top = -blockH / 2.0F;
        int skullX = Math.round(-SKULL_W / 2.0F);
        int skullY = Math.round(top);
        drawImage(g, SKULL, skullX, skullY, SKULL_W, SKULL_H, alpha, glitch, rnd);

        float rowTop = top + SKULL_H + GAP;
        if (!anim.death) {
            int logoX = Math.round(-LOGO_W / 2.0F);
            drawImage(g, LOGO, logoX, Math.round(rowTop), LOGO_W, LOGO_H, alpha, glitch, rnd);
        } else {
            float rowLeft = -blockW / 2.0F;
            // Nombre del jugador, grande y plateado, con sombra roja.
            pose.pushPose();
            pose.translate(rowLeft, rowTop + (LOGO_H - 9.0F * nameScale) / 2.0F, 0.0F);
            pose.scale(nameScale, nameScale, 1.0F);
            int a = Mth.clamp((int) (alpha * 255.0F), 4, 255);
            g.drawString(font, anim.text, 1, 1, (a << 24) | 0x4A0A0A, false);
            g.drawString(font, anim.text, 0, 0, (a << 24) | 0xDADADA, false);
            pose.popPose();
            int logoX = Math.round(rowLeft + nameW + 36.0F);
            drawImage(g, LOGO, logoX, Math.round(rowTop), LOGO_W, LOGO_H, alpha, glitch, rnd);
        }

        pose.popPose();

        // Nombre del cambio, en rojo, debajo del bloque.
        if (!anim.death) {
            float textScale = Math.max(1.0F, fit * 3.2F);
            pose.pushPose();
            float centerX = sw / 2.0F;
            float y = sh / 2.0F - 6.0F + blockH * fit / 2.0F + 10.0F;
            pose.translate(centerX, y, 0.0F);
            pose.scale(textScale, textScale, 1.0F);
            int a = Mth.clamp((int) (alpha * 255.0F), 4, 255);
            String title = anim.text.toUpperCase();
            g.drawString(font, title, -font.width(title) / 2, 0, (a << 24) | 0xFF3030, true);
            pose.popPose();
        }

        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** Dibuja una imagen; con glitch la parte en franjas desplazadas y con canales de color separados. */
    private static void drawImage(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h,
                                  float alpha, float glitch, Random rnd) {
        if (glitch > 0.05F) {
            int split = Math.round(10.0F * glitch);
            g.setColor(1.0F, 0.1F, 0.1F, alpha * 0.7F);
            blit(g, tex, x - split, y, w, h);
            g.setColor(0.1F, 1.0F, 1.0F, alpha * 0.5F);
            blit(g, tex, x + split, y, w, h);
        }
        g.setColor(1.0F, 1.0F, 1.0F, alpha);
        if (glitch > 0.05F) {
            int slice = Math.max(8, h / 14);
            for (int sy = 0; sy < h; sy += slice) {
                int hh = Math.min(slice, h - sy);
                int dx = rnd.nextFloat() < 0.4F ? Math.round((rnd.nextFloat() - 0.5F) * 60.0F * glitch) : 0;
                g.blit(tex, x + dx, y + sy, w, hh, 0.0F, (float) sy, w, hh, w, h);
            }
        } else {
            blit(g, tex, x, y, w, h);
        }
    }

    private static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h) {
        g.blit(tex, x, y, w, h, 0.0F, 0.0F, w, h, w, h);
    }

    private AnimationOverlay() {}
}
