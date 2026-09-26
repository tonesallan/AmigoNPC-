package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgLevelingStage1Config;
import br.tones.amigonpc.core.rpgleveling.stage1.RpgStage1MobLevelCalculator;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AmigoPlayerKillNpcXpSystem extends DamageEventSystem {
   private static final RpgLevelingStage1Config STAGE1_CFG = new RpgLevelingStage1Config();
   private static final RpgStage1MobLevelCalculator STAGE1_MOB_CALC = new RpgStage1MobLevelCalculator(STAGE1_CFG);
   private static final ConcurrentHashMap<UUID, Double> STAGE1_REMAINDER = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Long> RECENT_KILLS = new ConcurrentHashMap<>();

   public SystemGroup<EntityStore> getGroup() {
      try {
         return DamageModule.get().getInspectDamageGroup();
      } catch (Throwable ignored) {
         return DamageModule.get().getFilterDamageGroup();
      }
   }

   public Query<EntityStore> getQuery() {
      return Query.any();
   }

   public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer, Damage damage) {
      if (damage != null && !damage.isCancelled()) {
         Ref<EntityStore> targetRef = chunk.getReferenceTo(entityIndex);
         if (targetRef != null) {
            if (!AmigoPlayerKillNpcXpSupport.isPlayerRef(store, targetRef)) {
               AmigoNpcManager manager = AmigoNpcManager.getShared();

               try {
                  if (manager.isAmigoRef(targetRef)) {
                     return;
                  }
               } catch (Throwable var18) {
               }

               if (AmigoPlayerKillNpcXpSupport.isDeadTarget(store, targetRef, damage)) {
                  Ref<EntityStore> attackerRef = AmigoPlayerKillNpcXpSupport.extractAttackerRef(damage.getSource());
                  if (attackerRef != null) {
                     if (AmigoPlayerKillNpcXpSupport.isPlayerRef(store, attackerRef)) {
                        UUID ownerId = AmigoPlayerKillNpcXpSupport.resolveOwnerId(store, attackerRef);
                        if (ownerId != null) {
                           if (AmigoPlayerKillNpcXpAwardSupport.isNpcActive(manager, ownerId)) {
                              AmigoPlayerKillNpcXpSupport.scheduleSecondScan(manager, store, ownerId, targetRef, attackerRef);
                              String mobId = AmigoPlayerKillNpcXpSupport.resolveMobId(store, targetRef);
                              if (mobId != null) {
                                 long now = System.currentTimeMillis();
                                 if (AmigoPlayerKillNpcXpSupport.shouldSkipRecentKill(RECENT_KILLS, mobId, now, 1500L, 5000)) {
                                    return;
                                 }
                              }

                              int npcLevel = AmigoPlayerKillNpcXpAwardSupport.resolveNpcLevel(manager, ownerId);
                              int mobLevel = AmigoPlayerKillNpcXpAwardSupport.resolveMobLevel(store, targetRef, STAGE1_MOB_CALC, STAGE1_CFG);
                              double xpD = AmigoPlayerKillNpcXpAwardSupport.resolveXpFromKill(npcLevel, mobLevel, STAGE1_CFG);
                              long xpGain = AmigoPlayerKillNpcXpSupport.coerceStage1XpGainToLong(STAGE1_REMAINDER, ownerId, xpD, 5000);
                              if (xpGain > 0L) {
                                 NpcXpContext ctx = AmigoPlayerKillNpcXpAwardSupport.buildKillContext(store, mobId, mobLevel);
                                 AmigoPlayerKillNpcXpAwardSupport.awardXp(ownerId, xpGain, ctx);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
