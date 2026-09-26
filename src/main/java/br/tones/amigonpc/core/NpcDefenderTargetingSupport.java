package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class NpcDefenderTargetingSupport {
   private NpcDefenderTargetingSupport() {
   }

   static NpcDefenderTargetingSupport.Decision resolve(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      UUID ownerId,
      Object ownerRefObj,
      Vector3d ownerPos,
      Vector3d npcPos,
      long now,
      double horizontal,
      double chaseMaxDistance,
      long assistGraceMillis,
      long combatWindowMillis,
      double rangedAirMaxDy,
      NpcDefenderTargetingSupport.AliveChecker aliveChecker,
      NpcDefenderTargetingSupport.AssistHousekeeper assistHousekeeper,
      NpcDefenderTargetingSupport.AutoTargetFinder autoTargetFinder,
      NpcDefenderTargetingSupport.RetargetFinder retargetFinder,
      NpcDefenderTargetingSupport.DebugLogger debugLogger,
      NpcDefenderTargetingSupport.AirborneChecker airborneChecker,
      NpcDefenderTargetingSupport.EntityHeightGetter entityHeightGetter
   ) {
      if (store != null && rec != null && ownerRefObj != null && ownerPos != null && npcPos != null) {
         if (rec.combatUntilMillis > 0L && now > rec.combatUntilMillis) {
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
         }

         if (rec.npcCombatUntilMillis > 0L && now > rec.npcCombatUntilMillis) {
            rec.npcCombatUntilMillis = 0L;
            rec.npcCombatTargetRefObj = null;
         }

         Object combatTarget = NpcCombatStateSupport.getActiveCombatTarget(rec, now);
         if (combatTarget != null && !aliveChecker.isAlive(store, combatTarget)) {
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
            combatTarget = null;
         }

         Object npcCombatTarget = NpcCombatStateSupport.getActiveNpcCombatTarget(rec, now);
         if (npcCombatTarget != null && !aliveChecker.isAlive(store, npcCombatTarget)) {
            rec.npcCombatUntilMillis = 0L;
            rec.npcCombatTargetRefObj = null;
            npcCombatTarget = null;
         }

         assistHousekeeper.tick(rec, store, now);
         Object assistTarget = NpcCombatStateSupport.getActiveAssistTarget(rec, now);
         if (assistTarget != null && !aliveChecker.isAlive(store, assistTarget)) {
            rec.assistTargetRefObj = null;
            rec.assistUntilMillis = 0L;
            assistTarget = null;
         }

         Object desiredTarget;
         if (rec.combatMode == CombatMode.WEAKEST_ENEMY) {
            Object weakestTarget = autoTargetFinder.find(store, rec, ownerRefObj, ownerPos);
            if (weakestTarget != null) {
               rec.assistTargetRefObj = weakestTarget;
               rec.assistUntilMillis = 0L;
               desiredTarget = weakestTarget;
               debugLogger.log(rec, ownerId, "weakestTarget: targetRef=" + weakestTarget);
            } else {
               desiredTarget = ownerRefObj;
            }
         } else {
            desiredTarget = combatTarget != null ? combatTarget : (assistTarget != null ? assistTarget : ownerRefObj);
         }

         boolean inCombatOrAssist = desiredTarget != null && !refEq(desiredTarget, ownerRefObj);
         if (inCombatOrAssist && horizontal > chaseMaxDistance) {
            rec.combatUntilMillis = 0L;
            rec.combatTargetRefObj = null;
            rec.npcCombatUntilMillis = 0L;
            rec.npcCombatTargetRefObj = null;
            rec.assistTargetRefObj = null;
            rec.chaseDisengaged = true;
            rec.assistUntilMillis = now + assistGraceMillis;
            desiredTarget = ownerRefObj;
            inCombatOrAssist = false;
            resetTargetTrackers(rec);
         }

         if (inCombatOrAssist && !aliveChecker.isAlive(store, desiredTarget)) {
            refreshAutoLootStick(rec, store, desiredTarget, npcPos, now);
            Object deadTarget = desiredTarget;
            if (combatTarget != null && refEq(deadTarget, combatTarget)) {
               rec.combatUntilMillis = 0L;
               rec.combatTargetRefObj = null;
               combatTarget = null;
            }

            if (npcCombatTarget != null && refEq(deadTarget, npcCombatTarget)) {
               rec.npcCombatUntilMillis = 0L;
               rec.npcCombatTargetRefObj = null;
               npcCombatTarget = null;
            }

            if (rec.assistTargetRefObj != null && refEq(deadTarget, rec.assistTargetRefObj)) {
               rec.assistTargetRefObj = null;
               rec.assistUntilMillis = now + assistGraceMillis;
            }

            Object rt = retargetFinder.find(store, rec, ownerRefObj, npcPos, deadTarget);
            if (rt != null) {
               rec.assistTargetRefObj = rt;
               rec.assistUntilMillis = 0L;
               desiredTarget = rt;
               inCombatOrAssist = true;
               resetTargetTrackers(rec);
               debugLogger.log(rec, ownerId, "retargetOnDeath: targetRef=" + rt);
            } else {
               desiredTarget = ownerRefObj;
               inCombatOrAssist = false;
            }
         }

         if (inCombatOrAssist) {
            try {
               if (desiredTarget instanceof Ref && rec.refObj instanceof Ref) {
                  Ref<EntityStore> tgtRef = (Ref<EntityStore>)desiredTarget;
                  Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
                  TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
                  TransformComponent tgtT = (TransformComponent)store.getComponent(tgtRef, TransformComponent.getComponentType());
                  if (npcT != null && tgtT != null && npcT.getPosition() != null && tgtT.getPosition() != null) {
                     Vector3d np2 = npcT.getPosition();
                     Vector3d tp2 = tgtT.getPosition();
                     double dx2 = tp2.x() - np2.x();
                     double dz2 = tp2.z() - np2.z();
                     double dy2 = tp2.y() - np2.y();
                     double h2 = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                     double ady2 = Math.abs(dy2);
                     boolean airborne = false;

                     try {
                        airborne = airborneChecker.isAirborne(store, tgtRef, np2);
                     } catch (Throwable var59) {
                     }

                     double targetHeight = entityHeightGetter.get(store, tgtRef);
                     boolean tallTarget = targetHeight >= 1.9;
                     double allowedDy = airborne ? rangedAirMaxDy : (tallTarget ? 3.0 : 2.0);
                     if (!airborne && tallTarget && h2 < 0.9 && now - rec.lastCombatNudgeMillis > 900L) {
                        try {
                           double len = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                           double nx = len > 0.001 ? -dx2 / len : 1.0;
                           double nz = len > 0.001 ? -dz2 / len : 0.0;
                           Vector3d npos = new Vector3d(np2.x() + nx * 0.8, np2.y(), np2.z() + nz * 0.8);
                           npcT.teleportPosition(npos);
                           rec.lastCombatNudgeMillis = now;
                        } catch (Throwable var58) {
                        }
                     }

                     boolean tooFar = h2 > chaseMaxDistance;
                     boolean tooHigh = ady2 > allowedDy;
                     boolean stuckTooLong = false;
                     if (airborne) {
                        rec.targetStuckSinceMillis = 0L;
                        rec.lastTargetHorizontal = h2;
                        rec.lastTargetSampleMillis = now;
                     } else {
                        if (now - rec.lastTargetSampleMillis >= 350L) {
                           boolean npcNotMoving = now - rec.lastNpcMovedMillis > 900L;
                           boolean farFromTarget = h2 > 3.2;
                           if (!npcNotMoving || !farFromTarget || !(rec.lastTargetHorizontal >= 0.0) || !(h2 >= rec.lastTargetHorizontal - 0.2)) {
                              rec.targetStuckSinceMillis = 0L;
                           } else if (rec.targetStuckSinceMillis == 0L) {
                              rec.targetStuckSinceMillis = now;
                           }

                           rec.lastTargetHorizontal = h2;
                           rec.lastTargetSampleMillis = now;
                        }

                        stuckTooLong = rec.targetStuckSinceMillis > 0L && now - rec.targetStuckSinceMillis > 2000L;
                     }

                     boolean lostCond = tooFar || tooHigh || stuckTooLong;
                     if (lostCond) {
                        if (rec.targetLostSinceMillis == 0L) {
                           rec.targetLostSinceMillis = now;
                        }
                     } else {
                        rec.targetLostSinceMillis = 0L;
                     }

                     if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                        boolean okToRenew = !lostCond && h2 <= chaseMaxDistance;
                        if (okToRenew) {
                           rec.combatUntilMillis = now + combatWindowMillis;
                        }
                     }

                     if (rec.targetLostSinceMillis > 0L && now - rec.targetLostSinceMillis > 2000L) {
                        if (combatTarget != null && refEq(desiredTarget, combatTarget)) {
                           rec.combatUntilMillis = 0L;
                           rec.combatTargetRefObj = null;
                           combatTarget = null;
                        } else if (npcCombatTarget != null && refEq(desiredTarget, npcCombatTarget)) {
                           rec.npcCombatUntilMillis = 0L;
                           rec.npcCombatTargetRefObj = null;
                           npcCombatTarget = null;
                        } else {
                           rec.assistTargetRefObj = null;
                           rec.assistUntilMillis = now + assistGraceMillis;
                        }

                        resetTargetTrackers(rec);
                        desiredTarget = ownerRefObj;
                        inCombatOrAssist = false;
                     }
                  }
               }
            } catch (Throwable var60) {
            }
         } else {
            resetTargetTrackers(rec);
         }

         return new NpcDefenderTargetingSupport.Decision(combatTarget, npcCombatTarget, desiredTarget, inCombatOrAssist);
      } else {
         return new NpcDefenderTargetingSupport.Decision(null, null, ownerRefObj, false);
      }
   }

   private static void refreshAutoLootStick(AmigoNpcManager.NpcRecord rec, Store<EntityStore> store, Object desiredTarget, Vector3d npcPos, long now) {
      if (rec != null && rec.autoLootEnabled) {
         boolean refresh = rec.autoLootStickUntilMillis <= 0L
            || now >= rec.autoLootStickUntilMillis
            || rec.autoLootStickDeadRefObj == null
            || !refEq(rec.autoLootStickDeadRefObj, desiredTarget);
         if (refresh) {
            rec.autoLootStickUntilMillis = now + 900L;
            rec.autoLootStickDeadRefObj = desiredTarget;

            try {
               if (desiredTarget instanceof Ref<?> rawDeadRef) {
                  @SuppressWarnings("unchecked")
                  Ref<EntityStore> deadRef = (Ref<EntityStore>)rawDeadRef;
                  TransformComponent deadT = (TransformComponent)store.getComponent(deadRef, TransformComponent.getComponentType());
                  if (deadT != null && deadT.getPosition() != null) {
                     rec.autoLootStickAnchorPos = deadT.getPosition();
                  }
               }

               if (rec.autoLootStickAnchorPos == null) {
                  rec.autoLootStickAnchorPos = npcPos;
               }
            } catch (Throwable var9) {
            }
         }
      }
   }

   private static void resetTargetTrackers(AmigoNpcManager.NpcRecord rec) {
      if (rec != null) {
         rec.targetLostSinceMillis = 0L;
         rec.targetStuckSinceMillis = 0L;
         rec.lastTargetHorizontal = -1.0;
         rec.lastTargetSampleMillis = 0L;
      }
   }

   private static boolean refEq(Object a, Object b) {
      if (a == b) {
         return true;
      }

      if (a != null && b != null) {
         try {
            return a.equals(b);
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   @FunctionalInterface
   interface AirborneChecker {
      boolean isAirborne(Store<EntityStore> var1, Ref<EntityStore> var2, Vector3d var3);
   }

   @FunctionalInterface
   interface AliveChecker {
      boolean isAlive(Store<EntityStore> var1, Object var2);
   }

   @FunctionalInterface
   interface AssistHousekeeper {
      void tick(AmigoNpcManager.NpcRecord var1, Object var2, long var3);
   }

   @FunctionalInterface
   interface AutoTargetFinder {
      Object find(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, Vector3d var4);
   }

   @FunctionalInterface
   interface DebugLogger {
      void log(AmigoNpcManager.NpcRecord var1, UUID var2, String var3);
   }

   static final class Decision {
      final Object combatTarget;
      final Object npcCombatTarget;
      final Object desiredTarget;
      final boolean inCombatOrAssist;

      Decision(Object combatTarget, Object npcCombatTarget, Object desiredTarget, boolean inCombatOrAssist) {
         this.combatTarget = combatTarget;
         this.npcCombatTarget = npcCombatTarget;
         this.desiredTarget = desiredTarget;
         this.inCombatOrAssist = inCombatOrAssist;
      }
   }

   @FunctionalInterface
   interface EntityHeightGetter {
      double get(Object var1, Object var2);
   }

   @FunctionalInterface
   interface RetargetFinder {
      Object find(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, Vector3d var4, Object var5);
   }
}
