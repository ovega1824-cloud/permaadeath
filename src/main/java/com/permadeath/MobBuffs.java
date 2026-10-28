package com.permadeath;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Cambios que modifican a los mobs hostiles (atributos, equipo, efectos). */
public final class MobBuffs {

    public static boolean isHostile(Mob mob) {
        return mob instanceof Enemy && !mob.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static void addMod(Mob mob, Attribute attribute, String key, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) return;
        UUID id = UUID.nameUUIDFromBytes(("permadeath:" + key).getBytes(StandardCharsets.UTF_8));
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null) {
            if (existing.getAmount() == amount) return;
            inst.removeModifier(id);
        }
        inst.addTransientModifier(new AttributeModifier(id, "permadeath_" + key, amount, op));
    }

    private static void setIfEmpty(Mob mob, EquipmentSlot slot, Item item) {
        if (mob.getItemBySlot(slot).isEmpty()) {
            mob.setItemSlot(slot, new ItemStack(item));
            mob.setDropChance(slot, 0.0F);
        }
    }

    /**
     * @param fresh true solo cuando el mob acaba de aparecer (para cosas de una sola vez: equipo, bebes, etc.)
     */
    public static void apply(Mob mob, PermadeathData d, boolean fresh, long day) {
        if (!isHostile(mob)) return;

        // --- Atributos por tipo ---
        if (mob instanceof Zombie) {
            if (d.has(Change.ZOMBIE_SPEED)) addMod(mob, Attributes.MOVEMENT_SPEED, "zombie_speed", 0.35, AttributeModifier.Operation.MULTIPLY_BASE);
            if (d.has(Change.ZOMBIE_REINFORCE)) addMod(mob, Attributes.SPAWN_REINFORCEMENTS_CHANCE, "zombie_reinforce", 0.6, AttributeModifier.Operation.ADDITION);
        }
        if (mob instanceof AbstractSkeleton && d.has(Change.SKELETON_HEALTH)) {
            addMod(mob, Attributes.MAX_HEALTH, "skeleton_health", 12.0, AttributeModifier.Operation.ADDITION);
        }
        if (mob instanceof Spider && d.has(Change.SPIDER_SPEED)) {
            addMod(mob, Attributes.MOVEMENT_SPEED, "spider_speed", 0.40, AttributeModifier.Operation.MULTIPLY_BASE);
        }
        if (mob instanceof Creeper && d.has(Change.CREEPER_SPEED)) {
            addMod(mob, Attributes.MOVEMENT_SPEED, "creeper_speed", 0.40, AttributeModifier.Operation.MULTIPLY_BASE);
        }

        // --- Atributos para todos los hostiles ---
        if (d.has(Change.MOB_DAMAGE)) addMod(mob, Attributes.ATTACK_DAMAGE, "mob_damage", 0.5, AttributeModifier.Operation.MULTIPLY_BASE);
        if (d.has(Change.MOB_HEALTH)) addMod(mob, Attributes.MAX_HEALTH, "mob_health", 0.5, AttributeModifier.Operation.MULTIPLY_BASE);
        if (d.has(Change.MOB_SPEED)) addMod(mob, Attributes.MOVEMENT_SPEED, "mob_speed", 0.15, AttributeModifier.Operation.MULTIPLY_BASE);
        if (d.has(Change.MOB_FOLLOW)) addMod(mob, Attributes.FOLLOW_RANGE, "mob_follow", 32.0, AttributeModifier.Operation.ADDITION);
        if (d.has(Change.MOB_ARMOR)) addMod(mob, Attributes.ARMOR, "mob_armor", 6.0, AttributeModifier.Operation.ADDITION);
        if (d.has(Change.DAILY_SCALING) && day > 0) {
            double bonus = Math.min(day, 60L) * 0.03;
            addMod(mob, Attributes.MAX_HEALTH, "daily_scaling", bonus, AttributeModifier.Operation.MULTIPLY_BASE);
        }

        // --- Efectos permanentes ---
        if (d.has(Change.SUN_IMMUNE) && (mob instanceof Zombie || mob instanceof AbstractSkeleton)
                && !mob.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1_000_000, 0, false, false));
        }
        if (d.has(Change.MOB_REGEN) && !mob.hasEffect(MobEffects.REGENERATION)) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1_000_000, 0, false, false));
        }

        // --- Cosas de una sola vez, solo si el mob acaba de aparecer ---
        if (!fresh) return;

        if (d.has(Change.MOB_PERSIST)) mob.setPersistenceRequired();

        if (mob instanceof Creeper creeper) {
            boolean blast = d.has(Change.CREEPER_BLAST);
            boolean fuse = d.has(Change.CREEPER_FUSE);
            boolean charged = d.has(Change.CREEPER_CHARGED) && mob.getRandom().nextFloat() < 0.35F;
            if (blast || fuse || charged) {
                CompoundTag tag = new CompoundTag();
                creeper.saveWithoutId(tag);
                if (blast) tag.putByte("ExplosionRadius", (byte) 6);
                if (fuse) tag.putShort("Fuse", (short) 15);
                if (charged) tag.putBoolean("powered", true);
                creeper.load(tag);
            }
        }

        if (mob instanceof Zombie zombie) {
            if (d.has(Change.ZOMBIE_ARMOR)) {
                setIfEmpty(zombie, EquipmentSlot.HEAD, Items.IRON_HELMET);
                setIfEmpty(zombie, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
            }
            if (d.has(Change.ZOMBIE_SWORD) && zombie.getMainHandItem().isEmpty() && zombie.getRandom().nextFloat() < 0.4F) {
                setIfEmpty(zombie, EquipmentSlot.MAINHAND,
                        zombie.getRandom().nextBoolean() ? Items.IRON_SWORD : Items.DIAMOND_SWORD);
            }
            if (d.has(Change.ZOMBIE_BABY) && zombie.getRandom().nextFloat() < 0.35F) {
                zombie.setBaby(true);
            }
        }

        if (mob instanceof AbstractSkeleton && d.has(Change.SKELETON_HELMET)) {
            setIfEmpty(mob, EquipmentSlot.HEAD, Items.IRON_HELMET);
        }

        if (mob instanceof Phantom phantom && d.has(Change.PHANTOM_GIANT) && phantom.getPhantomSize() < 3) {
            phantom.setPhantomSize(3);
        }

        // Vida llena despues de aplicar los bonos de vida.
        mob.setHealth(mob.getMaxHealth());
    }

    private MobBuffs() {}
}
