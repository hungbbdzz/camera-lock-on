package com.velorise.cameralockon;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CombatAwareness {
    private static final Map<Integer, ThreatEntry> THREATS = new LinkedHashMap<>();

    private static ClientLevel trackedLevel;
    private static int previousHurtTime;
    private static float previousCombinedHealth = -1.0F;
    private static long attackerLockProtectedUntil;

    private CombatAwareness() {
    }

    public static void tick(LocalPlayer player, ClientLevel level) {
        if (trackedLevel != level) {
            clear();
            trackedLevel = level;
            previousCombinedHealth = player.getHealth() + player.getAbsorptionAmount();
        }

        long now = System.currentTimeMillis();
        removeExpiredThreats(level, now);

        float combinedHealth = player.getHealth() + player.getAbsorptionAmount();
        if (player.hurtTime > previousHurtTime) {
            float estimatedDamage = previousCombinedHealth < 0.0F
                    ? 1.0F
                    : Math.max(0.0F, previousCombinedHealth - combinedHealth);
            registerLatestAttacker(player, level, Math.max(1.0F, estimatedDamage), now);
        }

        previousHurtTime = player.hurtTime;
        previousCombinedHealth = combinedHealth;
    }

    private static void registerLatestAttacker(
            LocalPlayer player,
            ClientLevel level,
            float damage,
            long now
    ) {
        DamageSource source = player.getLastDamageSource();
        LivingEntity attacker = resolveAttacker(source);
        if (attacker == null
                || attacker == player
                || attacker.level() != level
                || TargetingRules.isBlacklisted(attacker.getType())) {
            return;
        }

        long hitWindowMillis = Math.max(
                1L,
                Math.round(CameraLockOnConfig.ATTACKER_HIT_WINDOW.get() * 1000.0D)
        );

        ThreatEntry entry = THREATS.computeIfAbsent(
                attacker.getId(),
                ignored -> new ThreatEntry(attacker)
        );
        entry.entity = attacker;

        if (now - entry.lastHitTime > hitWindowMillis) {
            entry.hitCount = 0;
            entry.accumulatedDamage = 0.0F;
        }

        entry.hitCount++;
        entry.accumulatedDamage += damage;
        entry.lastHitTime = now;

        tryAutomaticAttackerLock(player, now);
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        if (source == null) {
            return null;
        }

        Entity causingEntity = source.getEntity();
        if (causingEntity instanceof LivingEntity living) {
            return living;
        }

        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof LivingEntity living) {
            return living;
        }

        if (directEntity instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }

        return null;
    }

    private static void tryAutomaticAttackerLock(LocalPlayer player, long now) {
        CameraLockOnConfig.AttackerResponse response =
                CameraLockOnConfig.AttackerResponse.fromConfig(
                        CameraLockOnConfig.ATTACKER_RESPONSE.get()
                );

        if (response == CameraLockOnConfig.AttackerResponse.OFF
                || response == CameraLockOnConfig.AttackerResponse.INDICATOR_ONLY
                || now < attackerLockProtectedUntil) {
            return;
        }

        if (LockOnController.isActive()
                && !CameraLockOnConfig.ATTACKER_REPLACE_TARGET.get()) {
            return;
        }

        int requiredHits = response == CameraLockOnConfig.AttackerResponse.IMMEDIATE
                ? 1
                : CameraLockOnConfig.ATTACKER_REQUIRED_HITS.get();

        ThreatEntry best = null;
        double bestScore = -Double.MAX_VALUE;
        double maximumRange = CameraLockOnConfig.ATTACKER_LOCK_RANGE.get();

        for (ThreatEntry entry : THREATS.values()) {
            LivingEntity attacker = entry.entity;
            if (entry.hitCount < requiredHits
                    || attacker == null
                    || !TargetingRules.isEligible(
                            player,
                            attacker,
                            true,
                            LockOnController.getTemporaryPinnedType(),
                            maximumRange
                    )) {
                continue;
            }

            double score = calculateThreatScore(player, entry, now);
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }

        if (best != null && LockOnController.requestLock(
                best.entity,
                LockOnController.LockReason.ATTACKER,
                CameraLockOnConfig.ATTACKER_REPLACE_TARGET.get()
        )) {
            long protectionMillis = Math.max(
                    0L,
                    Math.round(CameraLockOnConfig.ATTACKER_LOCK_PROTECTION.get() * 1000.0D)
            );
            attackerLockProtectedUntil = now + protectionMillis;
        }
    }

    private static void removeExpiredThreats(ClientLevel level, long now) {
        long lifetimeMillis = Math.max(
                Math.round(CameraLockOnConfig.ATTACKER_INDICATOR_LIFETIME.get() * 1000.0D),
                Math.round(CameraLockOnConfig.ATTACKER_HIT_WINDOW.get() * 1000.0D)
        );

        THREATS.values().removeIf(entry -> entry.entity == null
                || entry.entity.isRemoved()
                || !entry.entity.isAlive()
                || entry.entity.level() != level
                || now - entry.lastHitTime > lifetimeMillis);
    }

    private static double calculateThreatScore(
            LocalPlayer player,
            ThreatEntry entry,
            long now
    ) {
        double ageSeconds = Math.max(0.0D, (now - entry.lastHitTime) / 1000.0D);
        double recencyBonus = Math.max(0.0D, 30.0D - ageSeconds * 8.0D);
        double distancePenalty = player.distanceTo(entry.entity) * 1.5D;
        double lineOfSightBonus = player.hasLineOfSight(entry.entity) ? 12.0D : 0.0D;
        return entry.hitCount * 40.0D
                + entry.accumulatedDamage * 5.0D
                + recencyBonus
                + lineOfSightBonus
                - distancePenalty;
    }

    public static List<ThreatSnapshot> getThreatSnapshots(LocalPlayer player) {
        long now = System.currentTimeMillis();
        long visibleLifetimeMillis = Math.max(
                1L,
                Math.round(CameraLockOnConfig.ATTACKER_INDICATOR_LIFETIME.get() * 1000.0D)
        );
        List<ThreatSnapshot> result = new ArrayList<>();

        for (ThreatEntry entry : THREATS.values()) {
            if (entry.entity == null || now - entry.lastHitTime > visibleLifetimeMillis) {
                continue;
            }
            result.add(new ThreatSnapshot(
                    entry.entity,
                    entry.hitCount,
                    entry.accumulatedDamage,
                    entry.lastHitTime,
                    calculateThreatScore(player, entry, now)
            ));
        }

        result.sort(Comparator.comparingDouble(ThreatSnapshot::score).reversed());
        return result;
    }

    public static void clear() {
        THREATS.clear();
        trackedLevel = null;
        previousHurtTime = 0;
        previousCombinedHealth = -1.0F;
        attackerLockProtectedUntil = 0L;
    }

    private static final class ThreatEntry {
        private LivingEntity entity;
        private int hitCount;
        private float accumulatedDamage;
        private long lastHitTime;

        private ThreatEntry(LivingEntity entity) {
            this.entity = entity;
        }
    }

    public record ThreatSnapshot(
            LivingEntity entity,
            int hitCount,
            float accumulatedDamage,
            long lastHitTime,
            double score
    ) {
    }
}
