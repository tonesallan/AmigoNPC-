package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class NpcRecoveryEffectSupport {
   private NpcRecoveryEffectSupport() {
   }

   static void tickSpawnFollowFx(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      long now,
      long spawnFxFollowIntervalMillis,
      NpcRecoveryEffectSupport.ParticleSpawner particleSpawner
   ) {
      if (store != null && rec != null) {
         if (!rec.downed) {
            if (rec.refObj instanceof Ref) {
               long until = rec.spawnFxUntilMillis;
               if (until > 0L) {
                  if (now >= until) {
                     rec.spawnFxUntilMillis = 0L;
                     rec.spawnFxNextMillis = 0L;
                  } else {
                     long next = rec.spawnFxNextMillis;
                     if (next <= 0L || now >= next) {
                        rec.spawnFxNextMillis = now + spawnFxFollowIntervalMillis;

                        try {
                           Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                           TransformComponent tc = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
                           if (tc == null || tc.getPosition() == null) {
                              return;
                           }

                           particleSpawner.spawn(store, ownerRefObj, tc.getPosition(), "PlayerSpawn_Spawn");
                        } catch (Throwable var14) {
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static void tickAutoRegen(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      long now,
      float regenRatePerSecond,
      NpcRecoveryEffectSupport.ParticleSpawner particleSpawner
   ) {
      if (store != null && rec != null) {
         if (!rec.godMode) {
            if (!rec.downed) {
               if (rec.refObj instanceof Ref) {
                  if (isInCombatNow(rec, now)) {
                     stopRegen(rec);
                  } else if (rec.regenStartAtMillis > 0L) {
                     if (now >= rec.regenStartAtMillis) {
                        long last = rec.regenLastApplyMillis;
                        long dt = last <= 0L ? 200L : now - last;
                        if (dt <= 0L) {
                           dt = 1L;
                        }

                        if (dt > 2000L) {
                           dt = 2000L;
                        }

                        rec.regenLastApplyMillis = now;

                        try {
                           Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                           EntityStatMap stats = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
                           if (stats == null) {
                              return;
                           }

                           int healthIdx = DefaultEntityStatTypes.getHealth();
                           float cur = stats.get(healthIdx).get();
                           float max = stats.get(healthIdx).getMax();
                           if (cur >= max) {
                              stopRegen(rec);
                              return;
                           }

                           float add = max * regenRatePerSecond * ((float)dt / 1000.0F);
                           if (add < 1.0F) {
                              add = 1.0F;
                           }

                           float next = cur + add;
                           if (next >= max) {
                              next = max;
                              stopRegen(rec);
                           }

                           stats.setStatValue(healthIdx, next);
                           store.putComponent(npcRef, EntityStatMap.getComponentType(), stats);

                           try {
                              TransformComponent tc = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
                              if (tc != null && tc.getPosition() != null) {
                                 particleSpawner.spawn(store, ownerRefObj, tc.getPosition(), "BEAM_HEAL_GREEN_OLD");
                              }
                           } catch (Throwable var19) {
                           }
                        } catch (Throwable var20) {
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isInCombatNow(AmigoNpcManager.NpcRecord rec, long now) {
      return rec == null
         ? false
         : now < rec.combatUntilMillis
            || now < rec.assistUntilMillis
            || now < rec.npcCombatUntilMillis
            || rec.combatTargetRefObj != null
            || rec.assistTargetRefObj != null
            || rec.npcCombatTargetRefObj != null;
   }

   private static void stopRegen(AmigoNpcManager.NpcRecord rec) {
      if (rec != null) {
         rec.regenStartAtMillis = 0L;
         rec.regenLastApplyMillis = 0L;
      }
   }

   @FunctionalInterface
   interface ParticleSpawner {
      void spawn(Store<EntityStore> var1, Object var2, Vector3d var3, String var4);
   }
}
