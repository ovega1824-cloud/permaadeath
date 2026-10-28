package com.permadeath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Cambios que ocurren cada cierto tiempo: apariciones extra, clima, rayos, TNT y fuego. */
public final class Spawner {

    private record Spec(Change change, EntityType<? extends Mob> type, int intervalSeconds, int count,
                        int cap, boolean dark, boolean sky) {}

    private static final List<Spec> SPECS = List.of(
            new Spec(Change.ZOMBIE_WAVES, EntityType.ZOMBIE, 25, 2, 12, true, false),
            new Spec(Change.SKELETON_AMBUSH, EntityType.SKELETON, 30, 2, 10, true, false),
            new Spec(Change.SPIDER_STALK, EntityType.SPIDER, 35, 2, 8, true, false),
            new Spec(Change.CREEPER_STALK, EntityType.CREEPER, 40, 1, 6, true, false),
            new Spec(Change.SILVERFISH, EntityType.SILVERFISH, 40, 3, 12, true, false),
            new Spec(Change.PHANTOM_SKY, EntityType.PHANTOM, 45, 2, 6, false, true),
            new Spec(Change.PILLAGER_ROAM, EntityType.PILLAGER, 60, 2, 6, false, false),
            new Spec(Change.WITCH_ROAM, EntityType.WITCH, 60, 1, 4, false, false),
            new Spec(Change.ENDERMAN_ROAM, EntityType.ENDERMAN, 60, 1, 5, false, false),
            new Spec(Change.VINDICATORS, EntityType.VINDICATOR, 75, 1, 4, false, false),
            new Spec(Change.WITHER_SKELETONS, EntityType.WITHER_SKELETON, 90, 1, 4, false, false),
            new Spec(Change.EVOKERS, EntityType.EVOKER, 120, 1, 2, false, false),
            new Spec(Change.SKY_GHASTS, EntityType.GHAST, 120, 1, 2, false, true),
            new Spec(Change.RAVAGERS, EntityType.RAVAGER, 180, 1, 2, false, false)
    );

    /** Se llama una vez por segundo. */
    public static void tick(MinecraftServer server, PermadeathData d, long second) {
        ServerLevel overworld = server.overworld();

        if (d.has(Change.ETERNAL_STORM) && !overworld.getLevelData().isThundering()) {
            overworld.setWeatherParameters(0, 36000, true, true);
        }

        // Horda al caer la noche (una vez por dia).
        long time = overworld.getDayTime();
        long day = time / 24000L;
        long timeOfDay = time % 24000L;
        if (d.has(Change.NIGHT_HORDE) && timeOfDay >= 13000L && timeOfDay < 13100L && d.lastHordeDay != day) {
            d.lastHordeDay = day;
            d.setDirty();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!canTarget(player)) continue;
                ServerLevel level = player.serverLevel();
                for (int i = 0; i < 8; i++) spawnNear(level, player, EntityType.ZOMBIE, false, false, 14, 26);
                for (int i = 0; i < 4; i++) spawnNear(level, player, EntityType.SKELETON, false, false, 14, 26);
                for (int i = 0; i < 2; i++) spawnNear(level, player, EntityType.SPIDER, false, false, 14, 26);
            }
        }

        for (Spec spec : SPECS) {
            if (!d.has(spec.change())) continue;
            if (second % spec.intervalSeconds() != spec.change().ordinal() % spec.intervalSeconds()) continue;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!canTarget(player)) continue;
                ServerLevel level = player.serverLevel();
                AABB box = player.getBoundingBox().inflate(48.0);
                int near = level.getEntitiesOfClass(Mob.class, box, m -> m.getType() == spec.type()).size();
                if (near >= spec.cap()) continue;
                for (int i = 0; i < spec.count(); i++) {
                    spawnNear(level, player, spec.type(), spec.dark(), spec.sky(), 18, 32);
                }
            }
        }

        if (d.has(Change.LIGHTNING) && second % 30 == Change.LIGHTNING.ordinal() % 30) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (canTarget(player)) strikeLightning(player);
            }
        }
        if (d.has(Change.TNT_RAIN) && second % 30 == Change.TNT_RAIN.ordinal() % 30) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (canTarget(player)) dropTnt(player);
            }
        }
        if (d.has(Change.WILDFIRES) && second % 20 == Change.WILDFIRES.ordinal() % 20) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (canTarget(player)) startFire(player);
            }
        }
    }

    private static boolean canTarget(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator()
                && player.serverLevel().getDifficulty() != Difficulty.PEACEFUL;
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        return level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static void spawnNear(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type,
                                  boolean dark, boolean sky, int minDist, int maxDist) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = minDist + random.nextDouble() * (maxDist - minDist);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            BlockPos pos = null;

            if (sky) {
                int y = player.blockPosition().getY() + 10 + random.nextInt(8);
                BlockPos candidate = new BlockPos(x, y, z);
                if (level.hasChunkAt(candidate) && level.isEmptyBlock(candidate)
                        && level.isEmptyBlock(candidate.above()) && level.isEmptyBlock(candidate.below())) {
                    pos = candidate;
                }
            } else {
                int baseY = player.blockPosition().getY();
                for (int dy = 6; dy >= -6; dy--) {
                    BlockPos candidate = new BlockPos(x, baseY + dy, z);
                    if (!level.hasChunkAt(candidate)) break;
                    if (isFree(level, candidate)) {
                        pos = candidate;
                        break;
                    }
                }
                if (pos != null && dark && level.getMaxLocalRawBrightness(pos) > 7) pos = null;
            }
            if (pos == null) continue;

            Mob mob = type.create(level);
            if (mob == null) return;
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob)) continue;
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (mob instanceof Phantom phantom) phantom.setPhantomSize(random.nextInt(3));
            level.addFreshEntity(mob);
            return;
        }
    }

    private static void strikeLightning(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.random;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = 8 + random.nextDouble() * 16;
        int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
        BlockPos probe = new BlockPos(x, 0, z);
        if (!level.hasChunkAt(probe)) return;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(Vec3.atBottomCenterOf(top));
        level.addFreshEntity(bolt);
    }

    private static void dropTnt(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.random;
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 10 + random.nextDouble() * 15;
            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;
            double y = player.getY() + 20 + random.nextInt(10);
            if (!level.hasChunkAt(new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z)))) continue;
            PrimedTnt tnt = new PrimedTnt(level, x, y, z, null);
            tnt.setFuse(80);
            level.addFreshEntity(tnt);
        }
    }

    private static void startFire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 3; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 4 + random.nextDouble() * 12;
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            BlockPos probe = new BlockPos(x, 0, z);
            if (!level.hasChunkAt(probe)) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, probe);
            if (Math.abs(pos.getY() - player.getY()) > 8) continue;
            if (!level.isEmptyBlock(pos)) continue;
            if (Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                return;
            }
        }
    }

    private Spawner() {}
}
