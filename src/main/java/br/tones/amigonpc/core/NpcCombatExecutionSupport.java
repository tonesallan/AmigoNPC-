package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class NpcCombatExecutionSupport {
   private NpcCombatExecutionSupport() {
   }

   static void applyTargets(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object npcEntityObj,
      Object ownerRef,
      Object desiredTarget,
      boolean inCombat,
      boolean ownerUnderground,
      boolean lootStickHold,
      String closeFollowSlot,
      boolean applyFlockState,
      NpcCombatExecutionSupport.MarkedTargetSetter markedTargetSetter,
      NpcCombatExecutionSupport.LockedTargetSetter lockedTargetSetter,
      NpcCombatExecutionSupport.FlockStateSetter flockStateSetter
   ) {
      if (store != null && rec != null && npcEntityObj != null) {
         boolean closeFollow = ownerUnderground && !inCombat;
         markedTargetSetter.set(npcEntityObj, closeFollowSlot, closeFollow ? ownerRef : null);
         Object lockedTarget = lootStickHold ? rec.refObj : (inCombat ? desiredTarget : ownerRef);
         lockedTargetSetter.set(npcEntityObj, lockedTarget);
         markedTargetSetter.set(npcEntityObj, "CombatTarget", inCombat ? desiredTarget : null);
         if (applyFlockState) {
            flockStateSetter.set(store, rec.refObj, inCombat ? "Run" : "Walk", "");
         }
      }
   }

   static void executeCombatOrIdle(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRef,
      Object desiredTarget,
      long now,
      boolean inCombat,
      boolean resetTrackingOnIdle,
      NpcCombatExecutionSupport.TargetMobLevelUpdater targetMobLevelUpdater,
      NpcCombatExecutionSupport.AirborneChecker airborneChecker,
      NpcCombatExecutionSupport.BowEquipper bowEquipper,
      NpcCombatExecutionSupport.RangedAttacker rangedAttacker,
      NpcCombatExecutionSupport.SwordApplier swordApplier,
      NpcCombatExecutionSupport.MeleeAttacker meleeAttacker
   ) {
      if (store != null && rec != null) {
         if (inCombat) {
            targetMobLevelUpdater.update(store, rec, desiredTarget);
            boolean wantsRanged = shouldUseRangedAgainstTarget(store, rec, desiredTarget, airborneChecker);
            if (wantsRanged) {
               bowEquipper.equip(store, rec, now);
               rangedAttacker.attack(store, rec, ownerRef, desiredTarget, now);
            } else {
               rec.rangedBowReadyAtMillis = 0L;
               rec.rangedBowItemId = null;
               applySwordIfPossible(store, rec, swordApplier);
               meleeAttacker.attack(store, rec, ownerRef, desiredTarget, now);
            }
         } else {
            rec.rangedBowReadyAtMillis = 0L;
            rec.rangedBowItemId = null;
            applySwordIfPossible(store, rec, swordApplier);
            rec.currentTargetMobLevel = 0;
            rec.currentTargetMobUuid = null;
            if (resetTrackingOnIdle) {
               rec.chaseDisengaged = false;
               rec.targetLostSinceMillis = 0L;
               rec.targetStuckSinceMillis = 0L;
               rec.lastTargetHorizontal = -1.0;
               rec.lastTargetSampleMillis = 0L;
            }
         }
      }
   }

   private static boolean shouldUseRangedAgainstTarget(
      Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, Object desiredTarget, NpcCombatExecutionSupport.AirborneChecker airborneChecker
   ) {
      if (store != null && rec != null && desiredTarget != null) {
         try {
            if (desiredTarget instanceof Ref && rec.refObj instanceof Ref) {
               Ref<EntityStore> tgtRef = (Ref<EntityStore>)desiredTarget;
               Ref<EntityStore> npcRef = (Ref<EntityStore>)rec.refObj;
               TransformComponent npcT = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
               if (npcT != null && npcT.getPosition() != null) {
                  return airborneChecker.isAirborne(store, tgtRef, npcT.getPosition());
               }
            }
         } catch (Throwable var7) {
         }

         return false;
      } else {
         return false;
      }
   }

   private static void applySwordIfPossible(Store<EntityStore> store, AmigoNpcManager.NpcRecord rec, NpcCombatExecutionSupport.SwordApplier swordApplier) {
      if (store != null && rec != null && swordApplier != null) {
         try {
            if (rec.refObj instanceof Ref<EntityStore> npcRef) {
               swordApplier.apply(store, npcRef, rec.ownerId, rec, false);
            }
         } catch (Throwable var4) {
         }
      }
   }

   @FunctionalInterface
   interface AirborneChecker {
      boolean isAirborne(Store<EntityStore> var1, Ref<EntityStore> var2, Vector3d var3);
   }

   @FunctionalInterface
   interface BowEquipper {
      void equip(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, long var3);
   }

   @FunctionalInterface
   interface FlockStateSetter {
      void set(Store<EntityStore> var1, Object var2, String var3, String var4);
   }

   @FunctionalInterface
   interface LockedTargetSetter {
      void set(Object var1, Object var2);
   }

   @FunctionalInterface
   interface MarkedTargetSetter {
      void set(Object var1, String var2, Object var3);
   }

   @FunctionalInterface
   interface MeleeAttacker {
      void attack(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, Object var4, long var5);
   }

   @FunctionalInterface
   interface RangedAttacker {
      void attack(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, Object var4, long var5);
   }

   @FunctionalInterface
   interface SwordApplier {
      void apply(Store<EntityStore> var1, Ref<EntityStore> var2, UUID var3, AmigoNpcManager.NpcRecord var4, boolean var5);
   }

   @FunctionalInterface
   interface TargetMobLevelUpdater {
      void update(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3);
   }
}
