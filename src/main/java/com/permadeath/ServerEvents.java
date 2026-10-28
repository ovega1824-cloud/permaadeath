package com.permadeath;

import com.permadeath.net.AnimationPacket;
import com.permadeath.net.Net;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = PermadeathMod.MODID)
public final class ServerEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PermadeathCommand.register(event.getDispatcher());
    }

    /** Cada segundo: revisa si paso un dia de Minecraft y corre los cambios periodicos. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 20 != 0) return;

        PermadeathData data = PermadeathData.get(server);
        if (!data.started) return;

        long day = Manager.currentDay(server);
        if (day < data.lastDay) {
            data.lastDay = day;
            data.setDirty();
        } else if (day > data.lastDay) {
            data.lastDay = day;
            data.setDirty();
            Manager.activateRandom(server, data);
        }

        Spawner.tick(server, data, server.getTickCount() / 20L);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            Manager.syncTo(player, PermadeathData.get(player.getServer()));
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();

        boolean fresh = false;
        if (entity instanceof Mob) {
            CompoundTag persistent = entity.getPersistentData();
            fresh = !persistent.getBoolean("pd_seen");
            persistent.putBoolean("pd_seen", true);
        }

        MinecraftServer server = event.getLevel().getServer();
        if (server == null) return;
        PermadeathData data = PermadeathData.get(server);
        if (data.activeCount() == 0) return;

        if (entity instanceof Mob mob) {
            MobBuffs.apply(mob, data, fresh, Manager.currentDay(server));
        } else if (entity instanceof AbstractArrow arrow && arrow.getOwner() instanceof AbstractSkeleton) {
            CompoundTag persistent = arrow.getPersistentData();
            if (persistent.getBoolean("pd_arrow")) return;
            persistent.putBoolean("pd_arrow", true);
            if (data.has(Change.FIRE_ARROWS)) arrow.setSecondsOnFire(100);
            if (data.has(Change.HEAVY_ARROWS)) arrow.setBaseDamage(arrow.getBaseDamage() + 2.0D);
        }
    }

    /** Efectos de los golpes de los mobs hostiles contra los jugadores. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !MobBuffs.isHostile(attacker)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        PermadeathData data = PermadeathData.get(server);
        if (data.activeCount() == 0) return;

        if (data.has(Change.DEADLY_NIGHTS) && isNight(player.level())) {
            event.setAmount(event.getAmount() * 1.5F);
        }

        boolean melee = event.getSource().getDirectEntity() == attacker;
        if (!melee) return;
        if (data.has(Change.BURNING_HITS)) player.setSecondsOnFire(4);
        if (data.has(Change.SPIDER_POISON) && attacker instanceof Spider) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
        if (data.has(Change.VAMPIRISM)) attacker.heal(event.getAmount() * 0.5F);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypes.FALL) || !MobBuffs.isHostile(mob)) return;
        MinecraftServer server = mob.getServer();
        if (server == null) return;
        if (PermadeathData.get(server).has(Change.NO_FALL)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) return;

        // Animacion de muerte para todos los jugadores.
        if (dead instanceof ServerPlayer player) {
            Net.sendToAll(new AnimationPacket(AnimationPacket.DEATH, player.getName().getString()));
            return;
        }

        if (!(dead instanceof Mob mob) || !MobBuffs.isHostile(mob)) return;
        MinecraftServer server = mob.getServer();
        if (server == null || !(mob.level() instanceof ServerLevel level)) return;
        PermadeathData data = PermadeathData.get(server);
        if (data.activeCount() == 0) return;

        if (data.has(Change.EXPLOSIVE_DEATHS) && !(mob instanceof Creeper)
                && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && mob.getRandom().nextFloat() < 0.2F) {
            level.explode(null, mob.getX(), mob.getY(0.5D), mob.getZ(), 1.8F, Level.ExplosionInteraction.NONE);
        }

        if (data.has(Change.ZOMBIE_REVIVE) && mob instanceof Zombie
                && !mob.getPersistentData().getBoolean("pd_revived")
                && mob.getRandom().nextFloat() < 0.3F) {
            Zombie risen = EntityType.ZOMBIE.create(level);
            if (risen != null) {
                risen.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0.0F);
                risen.getPersistentData().putBoolean("pd_revived", true);
                risen.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                        MobSpawnType.EVENT, null, null);
                level.addFreshEntity(risen);
            }
        }
    }

    private static boolean isNight(Level level) {
        long t = level.getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    private ServerEvents() {}
}
