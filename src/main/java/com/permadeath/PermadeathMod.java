package com.permadeath;

import com.permadeath.net.Net;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PermadeathMod.MODID)
public class PermadeathMod {
    public static final String MODID = "permadeath";

    public PermadeathMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.SOUNDS.register(modBus);
        Net.register();
    }
}
