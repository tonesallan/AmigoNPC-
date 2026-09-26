package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.Source;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class AmigoPlayerKillNpcXpSupport {
   private AmigoPlayerKillNpcXpSupport() {
   }

   static boolean isDeadTarget(Store<EntityStore> store, Ref<EntityStore> targetRef, Damage damage) {
      if (store != null && targetRef != null && damage != null) {
         float hp = Float.NaN;
         boolean dead = false;

         try {
            DeathComponent dc = (DeathComponent)store.getComponent(targetRef, DeathComponent.getComponentType());
            if (dc != null) {
               dead = true;
            }
         } catch (Throwable var7) {
         }

         try {
            EntityStatMap stats = (EntityStatMap)store.getComponent(targetRef, EntityStatMap.getComponentType());
            if (stats != null) {
               hp = stats.get(DefaultEntityStatTypes.getHealth()).get();
               if (hp <= 0.0F) {
                  dead = true;
               }
            }
         } catch (Throwable var6) {
         }

         if (dead) {
            return true;
         }

         try {
            float amt = damage.getAmount();
            return !Float.isNaN(hp) && hp > 0.0F && amt > 0.0F && hp - amt <= 0.001F;
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   static Ref<EntityStore> extractAttackerRef(Source src) {
      return DamageSourceRefSupport.extractAttackerRef(src);
   }

   static boolean isPlayerRef(Store<EntityStore> store, Ref<EntityStore> ref) {
      if (store != null && ref != null) {
         try {
            return store.getComponent(ref, Player.getComponentType()) != null;
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   static UUID resolveOwnerId(Store<EntityStore> store, Ref<EntityStore> attackerRef) {
      if (store != null && attackerRef != null) {
         try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(attackerRef, UUIDComponent.getComponentType());
            return uuidComp == null ? null : uuidComp.getUuid();
         } catch (Throwable ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   static void scheduleSecondScan(AmigoNpcManager manager, Store<EntityStore> store, UUID ownerId, Ref<EntityStore> targetRef, Ref<EntityStore> attackerRef) {
      if (manager != null && store != null && ownerId != null) {
         try {
            Vector3d anchorPos = null;

            try {
               TransformComponent tc = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
               if (tc != null && tc.getPosition() != null) {
                  anchorPos = tc.getPosition();
               }
            } catch (Throwable var8) {
            }

            if (anchorPos == null) {
               try {
                  TransformComponent atc = (TransformComponent)store.getComponent(attackerRef, TransformComponent.getComponentType());
                  if (atc != null && atc.getPosition() != null) {
                     anchorPos = atc.getPosition();
                  }
               } catch (Throwable var7) {
               }
            }

            manager.scheduleAutoLootSecondScan(ownerId, anchorPos, targetRef);
         } catch (Throwable var9) {
         }
      }
   }

   static String resolveMobId(Store<EntityStore> store, Ref<EntityStore> targetRef) {
      if (store != null && targetRef != null) {
         try {
            UUIDComponent mobUuid = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
            if (mobUuid != null && mobUuid.getUuid() != null) {
               return mobUuid.getUuid().toString();
            }
         } catch (Throwable var3) {
         }

         return null;
      } else {
         return null;
      }
   }

   static boolean shouldSkipRecentKill(ConcurrentHashMap<String, Long> recentKills, String mobId, long now, long dedupeWindowMillis, int maxEntries) {
      if (recentKills != null && mobId != null) {
         Long last = recentKills.get(mobId);
         if (last != null && now - last < dedupeWindowMillis) {
            return true;
         }

         recentKills.put(mobId, now);
         if (recentKills.size() > maxEntries) {
            recentKills.clear();
         }

         return false;
      } else {
         return false;
      }
   }

   static long coerceStage1XpGainToLong(ConcurrentHashMap<UUID, Double> stage1Remainder, UUID ownerId, double xpGainDouble, int maxEntries) {
      if (stage1Remainder == null || ownerId == null) {
         return 0L;
      }

      if (xpGainDouble > 0.0 && !Double.isNaN(xpGainDouble) && !Double.isInfinite(xpGainDouble)) {
         double rem = stage1Remainder.getOrDefault(ownerId, 0.0);
         if (Double.isNaN(rem) || Double.isInfinite(rem) || rem < 0.0) {
            rem = 0.0;
         }

         double sum = xpGainDouble + rem;
         long whole = (long)Math.floor(sum);
         double newRem = sum - whole;
         if (!(newRem >= 0.0) || Double.isNaN(newRem) || Double.isInfinite(newRem)) {
            newRem = 0.0;
         }

         stage1Remainder.put(ownerId, newRem);
         if (stage1Remainder.size() > maxEntries) {
            stage1Remainder.clear();
         }

         return Math.max(0L, whole);
      } else {
         return 0L;
      }
   }
}
