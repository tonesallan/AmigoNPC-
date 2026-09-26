package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.UUID;

final class NpcFollowCombatSupport {
   private NpcFollowCombatSupport() {
   }

   static void tick(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d ownerPos,
      Vector3d npcPos,
      long now,
      double horizontal,
      boolean ownerUnderground,
      boolean lootStickActiveNow,
      String lockedTargetCloseSlot,
      double chaseMaxDistance,
      long assistGraceMillis,
      long combatWindowMillis,
      double rangedAirMaxDy,
      NpcFollowCombatSupport.ComponentGetter componentGetter,
      NpcFollowCombatSupport.MarkedTargetSetter markedTargetSetter,
      NpcFollowCombatSupport.LockedTargetSetter lockedTargetSetter,
      NpcFollowCombatSupport.FlockStateSetter flockStateSetter,
      NpcFollowCombatSupport.AliveChecker aliveChecker,
      NpcFollowCombatSupport.AssistHousekeeper assistHousekeeper,
      NpcFollowCombatSupport.DefenderTargetFinder defenderTargetFinder,
      NpcFollowCombatSupport.RetargetFinder retargetFinder,
      NpcFollowCombatSupport.DebugLogger debugLogger,
      NpcFollowCombatSupport.AirborneChecker airborneChecker,
      NpcFollowCombatSupport.EntityHeightGetter entityHeightGetter,
      NpcFollowCombatSupport.TargetMobLevelUpdater targetMobLevelUpdater,
      NpcFollowCombatSupport.BowEquipper bowEquipper,
      NpcFollowCombatSupport.RangedAttacker rangedAttacker,
      NpcFollowCombatSupport.SwordApplier swordApplier,
      NpcFollowCombatSupport.MeleeAttacker meleeAttacker
   ) {
      if (store != null && rec != null) {
         if (lootStickActiveNow) {
            holdLootStick(store, rec, lockedTargetCloseSlot, componentGetter, markedTargetSetter, lockedTargetSetter);
         } else if (!rec.defendeEnabled) {
            runNonDefender(
               store,
               rec,
               ownerRefObj,
               now,
               horizontal,
               ownerUnderground,
               lockedTargetCloseSlot,
               chaseMaxDistance,
               aliveChecker,
               componentGetter,
               markedTargetSetter,
               lockedTargetSetter,
               flockStateSetter,
               targetMobLevelUpdater,
               airborneChecker,
               bowEquipper,
               rangedAttacker,
               swordApplier,
               meleeAttacker
            );
         } else {
            runDefender(
               store,
               ownerId,
               rec,
               ownerRefObj,
               ownerPos,
               npcPos,
               now,
               horizontal,
               ownerUnderground,
               lockedTargetCloseSlot,
               chaseMaxDistance,
               assistGraceMillis,
               combatWindowMillis,
               rangedAirMaxDy,
               componentGetter,
               markedTargetSetter,
               lockedTargetSetter,
               flockStateSetter,
               aliveChecker,
               assistHousekeeper,
               defenderTargetFinder,
               retargetFinder,
               debugLogger,
               airborneChecker,
               entityHeightGetter,
               targetMobLevelUpdater,
               bowEquipper,
               rangedAttacker,
               swordApplier,
               meleeAttacker
            );
         }
      }
   }

   private static void holdLootStick(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      String lockedTargetCloseSlot,
      NpcFollowCombatSupport.ComponentGetter componentGetter,
      NpcFollowCombatSupport.MarkedTargetSetter markedTargetSetter,
      NpcFollowCombatSupport.LockedTargetSetter lockedTargetSetter
   ) {
      Object npcEntityObj = componentGetter.get(store, rec.refObj, NPCEntity.getComponentType());
      if (npcEntityObj != null) {
         markedTargetSetter.set(npcEntityObj, lockedTargetCloseSlot, null);
         lockedTargetSetter.set(npcEntityObj, rec.refObj);
         markedTargetSetter.set(npcEntityObj, "CombatTarget", null);
      }
   }

   private static void runNonDefender(
      Store<EntityStore> store,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      long now,
      double horizontal,
      boolean ownerUnderground,
      String lockedTargetCloseSlot,
      double chaseMaxDistance,
      NpcFollowCombatSupport.AliveChecker aliveChecker,
      NpcFollowCombatSupport.ComponentGetter componentGetter,
      NpcFollowCombatSupport.MarkedTargetSetter markedTargetSetter,
      NpcFollowCombatSupport.LockedTargetSetter lockedTargetSetter,
      NpcFollowCombatSupport.FlockStateSetter flockStateSetter,
      NpcFollowCombatSupport.TargetMobLevelUpdater targetMobLevelUpdater,
      NpcFollowCombatSupport.AirborneChecker airborneChecker,
      NpcFollowCombatSupport.BowEquipper bowEquipper,
      NpcFollowCombatSupport.RangedAttacker rangedAttacker,
      NpcFollowCombatSupport.SwordApplier swordApplier,
      NpcFollowCombatSupport.MeleeAttacker meleeAttacker
   ) {
      NpcNonDefenderTargetingSupport.Decision decision = NpcNonDefenderTargetingSupport.resolve(
         store, rec, ownerRefObj, now, horizontal, chaseMaxDistance, aliveChecker::isAlive
      );
      Object desiredTarget = decision.desiredTarget;
      boolean inCombat = decision.inCombat;
      Object npcEntityObj = componentGetter.get(store, rec.refObj, NPCEntity.getComponentType());
      if (npcEntityObj != null) {
         boolean lootStickHold = rec.autoLootEnabled && rec.autoLootStickUntilMillis > now && !inCombat;
         NpcCombatExecutionSupport.applyTargets(
            store,
            rec,
            npcEntityObj,
            ownerRefObj,
            desiredTarget,
            inCombat,
            ownerUnderground,
            lootStickHold,
            lockedTargetCloseSlot,
            false,
            markedTargetSetter::set,
            lockedTargetSetter::set,
            flockStateSetter::set
         );
      }

      NpcCombatExecutionSupport.executeCombatOrIdle(
         store,
         rec,
         ownerRefObj,
         desiredTarget,
         now,
         inCombat,
         true,
         targetMobLevelUpdater::update,
         airborneChecker::isAirborne,
         bowEquipper::equip,
         rangedAttacker::attack,
         swordApplier::apply,
         meleeAttacker::attack
      );
   }

   private static void runDefender(
      Store<EntityStore> store,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      Object ownerRefObj,
      Vector3d ownerPos,
      Vector3d npcPos,
      long now,
      double horizontal,
      boolean ownerUnderground,
      String lockedTargetCloseSlot,
      double chaseMaxDistance,
      long assistGraceMillis,
      long combatWindowMillis,
      double rangedAirMaxDy,
      NpcFollowCombatSupport.ComponentGetter componentGetter,
      NpcFollowCombatSupport.MarkedTargetSetter markedTargetSetter,
      NpcFollowCombatSupport.LockedTargetSetter lockedTargetSetter,
      NpcFollowCombatSupport.FlockStateSetter flockStateSetter,
      NpcFollowCombatSupport.AliveChecker aliveChecker,
      NpcFollowCombatSupport.AssistHousekeeper assistHousekeeper,
      NpcFollowCombatSupport.DefenderTargetFinder defenderTargetFinder,
      NpcFollowCombatSupport.RetargetFinder retargetFinder,
      NpcFollowCombatSupport.DebugLogger debugLogger,
      NpcFollowCombatSupport.AirborneChecker airborneChecker,
      NpcFollowCombatSupport.EntityHeightGetter entityHeightGetter,
      NpcFollowCombatSupport.TargetMobLevelUpdater targetMobLevelUpdater,
      NpcFollowCombatSupport.BowEquipper bowEquipper,
      NpcFollowCombatSupport.RangedAttacker rangedAttacker,
      NpcFollowCombatSupport.SwordApplier swordApplier,
      NpcFollowCombatSupport.MeleeAttacker meleeAttacker
   ) {
      NpcDefenderTargetingSupport.Decision decision = NpcDefenderTargetingSupport.resolve(
         store,
         rec,
         ownerId,
         ownerRefObj,
         ownerPos,
         npcPos,
         now,
         horizontal,
         chaseMaxDistance,
         assistGraceMillis,
         combatWindowMillis,
         rangedAirMaxDy,
         aliveChecker::isAlive,
         assistHousekeeper::tick,
         defenderTargetFinder::find,
         retargetFinder::find,
         debugLogger::log,
         airborneChecker::isAirborne,
         entityHeightGetter::get
      );
      Object desiredTarget = decision.desiredTarget;
      boolean inCombatOrAssist = decision.inCombatOrAssist;
      Object npcEntityObj = componentGetter.get(store, rec.refObj, NPCEntity.getComponentType());
      if (npcEntityObj != null) {
         NpcCombatExecutionSupport.applyTargets(
            store,
            rec,
            npcEntityObj,
            ownerRefObj,
            desiredTarget,
            inCombatOrAssist,
            ownerUnderground,
            false,
            lockedTargetCloseSlot,
            true,
            markedTargetSetter::set,
            lockedTargetSetter::set,
            flockStateSetter::set
         );
         NpcCombatExecutionSupport.executeCombatOrIdle(
            store,
            rec,
            ownerRefObj,
            desiredTarget,
            now,
            inCombatOrAssist,
            false,
            targetMobLevelUpdater::update,
            airborneChecker::isAirborne,
            bowEquipper::equip,
            rangedAttacker::attack,
            swordApplier::apply,
            meleeAttacker::attack
         );
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
   interface BowEquipper {
      void equip(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, long var3);
   }

   @FunctionalInterface
   interface ComponentGetter {
      Object get(Object var1, Object var2, Object var3);
   }

   @FunctionalInterface
   interface DebugLogger {
      void log(AmigoNpcManager.NpcRecord var1, UUID var2, String var3);
   }

   @FunctionalInterface
   interface DefenderTargetFinder {
      Object find(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, Vector3d var4);
   }

   @FunctionalInterface
   interface EntityHeightGetter {
      double get(Object var1, Object var2);
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
   interface RetargetFinder {
      Object find(Store<EntityStore> var1, AmigoNpcManager.NpcRecord var2, Object var3, Vector3d var4, Object var5);
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
