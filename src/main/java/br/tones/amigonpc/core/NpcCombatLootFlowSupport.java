package br.tones.amigonpc.core;

import br.tones.amigonpc.core.debug.ActionTraceService;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.UUID;

final class NpcCombatLootFlowSupport {
   private NpcCombatLootFlowSupport() {
   }

   static void endCombatTaggedLooting(AmigoNpcManager.NpcRecord rec) {
      if (rec != null) {
         rec.lootingActive = false;
         rec.lootTargetRefObj = null;
         rec.lootTargetSinceMillis = 0L;
         if (rec.pendingLootRefObjs != null) {
            rec.pendingLootRefObjs.clear();
         }

         if (rec.lootProcessedUntil != null) {
            rec.lootProcessedUntil.clear();
         }

         if (rec.combatTags != null) {
            rec.combatTags.clear();
         }

         rec.lastBattleCenter = null;
         rec.lastCombatEndMillis = 0L;
         rec.lootPausedInventoryFull = false;
         rec.nextAutoLootMillis = 0L;
         rec.autoLootStickUntilMillis = 0L;
         rec.autoLootStickDeadRefObj = null;
         rec.autoLootStickAnchorPos = null;
      }
   }

   static NpcCombatLootFlowSupport.PreLootAction prepareCombatTaggedLooting(
      AmigoNpcManager.NpcRecord rec,
      UUID ownerId,
      long now,
      Vector3d ownerPos,
      boolean inCombatOrAssistNow,
      long lootPostCombatStickMillis,
      long combatTagClearMillis,
      double combatTagClearDistance,
      NpcCombatLootFlowSupport.EnemyNearChecker enemyNearChecker
   ) {
      if (rec == null) {
         return NpcCombatLootFlowSupport.PreLootAction.RETURN;
      }

      if (inCombatOrAssistNow) {
         if (!rec.wasInCombat) {
            try {
               ActionTraceService.getShared().record(ownerId, "npc_state", "combat_active");
            } catch (Throwable var17) {
            }
         }

         rec.wasInCombat = true;
         rec.lootingActive = false;
         rec.lootTargetRefObj = null;
         rec.lootTargetSinceMillis = 0L;
         rec.lootStickUntilMillis = 0L;
         rec.pendingLootRefObjs.clear();
         return NpcCombatLootFlowSupport.PreLootAction.RETURN;
      } else {
         if (rec.wasInCombat) {
            rec.wasInCombat = false;
            rec.lastCombatEndMillis = now;

            try {
               ActionTraceService.getShared().record(ownerId, "npc_state", "combat_end");
            } catch (Throwable var21) {
            }

            boolean hasTags;
            synchronized (rec.combatTags) {
               hasTags = !rec.combatTags.isEmpty();
            }

            rec.lootingActive = hasTags;
            rec.lootStickUntilMillis = hasTags ? now + lootPostCombatStickMillis : 0L;
            if (hasTags) {
               try {
                  ActionTraceService.getShared().record(ownerId, "npc_state", "looting_start");
               } catch (Throwable var19) {
               }
            }

            rec.lootTargetRefObj = null;
            rec.lootTargetSinceMillis = 0L;
            rec.pendingLootRefObjs.clear();
         }

         if (!rec.lootingActive) {
            return NpcCombatLootFlowSupport.PreLootAction.RETURN;
         }

         if (rec.lastCombatEndMillis > 0L) {
            boolean timeUp = now - rec.lastCombatEndMillis >= combatTagClearMillis;
            boolean distUp = false;
            if (rec.lastBattleCenter != null && ownerPos != null) {
               double clearDistanceSq = combatTagClearDistance * combatTagClearDistance;
               distUp = dist2(rec.lastBattleCenter, ownerPos) >= clearDistanceSq;
            }

            if (timeUp || distUp) {
               return NpcCombatLootFlowSupport.PreLootAction.CLEAR_AND_RETURN;
            }
         }

         if (enemyNearChecker != null && enemyNearChecker.isEnemyNear()) {
            rec.lootingActive = false;
            rec.lootTargetRefObj = null;
            rec.lootTargetSinceMillis = 0L;
            rec.pendingLootRefObjs.clear();

            try {
               ActionTraceService.getShared().record(ownerId, "npc_state", "looting_stop reason=enemy_near");
            } catch (Throwable var18) {
            }

            return NpcCombatLootFlowSupport.PreLootAction.RETURN;
         } else {
            return NpcCombatLootFlowSupport.PreLootAction.CONTINUE;
         }
      }
   }

   private static double dist2(Vector3d a, Vector3d b) {
      if (a != null && b != null) {
         double dx = a.getX() - b.getX();
         double dy = a.getY() - b.getY();
         double dz = a.getZ() - b.getZ();
         return dx * dx + dy * dy + dz * dz;
      } else {
         return Double.POSITIVE_INFINITY;
      }
   }

   @FunctionalInterface
   interface EnemyNearChecker {
      boolean isEnemyNear();
   }

   enum PreLootAction {
      CONTINUE,
      RETURN,
      CLEAR_AND_RETURN;
   }
}
