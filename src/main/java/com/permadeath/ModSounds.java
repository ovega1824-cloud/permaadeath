package com.permadeath;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PermadeathMod.MODID);

    public static final RegistryObject<SoundEvent> CHANGE = register("change");
    public static final RegistryObject<SoundEvent> DEATH = register("death");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(PermadeathMod.MODID, name)));
    }

    private ModSounds() {}
}
