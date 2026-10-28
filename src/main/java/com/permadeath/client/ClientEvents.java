package com.permadeath.client;

import com.permadeath.PermadeathMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PermadeathMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        AnimationOverlay.tick();
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN_BOOK.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new BookScreen());
            }
        }
    }

    /** Con el juego normal, la animación se dibuja encima del HUD. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) {
            AnimationOverlay.render(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    /** Si hay una pantalla abierta (pantalla de muerte, libro, inventario...), se dibuja encima de ella. */
    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        AnimationOverlay.render(event.getGuiGraphics(), event.getPartialTick());
    }

    private ClientEvents() {}
}
