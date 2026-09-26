package br.tones.amigonpc.core;

import br.tones.amigonpc.core.debug.ActionTraceService;
import java.util.UUID;

final class NpcCombatStateSupport {
   private NpcCombatStateSupport() {
   }

   static Object getActiveCombatTarget(AmigoNpcManager.NpcRecord rec, long now) {
      if (rec == null) {
         return null;
      } else if (rec.combatTargetRefObj == null) {
         return null;
      } else if (rec.combatUntilMillis <= 0L) {
         return null;
      } else {
         return now > rec.combatUntilMillis ? null : rec.combatTargetRefObj;
      }
   }

   static Object getActiveNpcCombatTarget(AmigoNpcManager.NpcRecord rec, long now) {
      if (rec == null) {
         return null;
      } else if (rec.npcCombatTargetRefObj == null) {
         return null;
      } else if (rec.npcCombatUntilMillis <= 0L) {
         return null;
      } else {
         return now > rec.npcCombatUntilMillis ? null : rec.npcCombatTargetRefObj;
      }
   }

   static Object getActiveAssistTarget(AmigoNpcManager.NpcRecord rec, long now) {
      if (rec == null || rec.assistTargetRefObj == null) {
         return null;
      }

      // Weakest-enemy mode locks the selected target until it is defeated or
      // explicitly discarded by targeting/chase validation. It may not acquire
      // a new target without an active owner combat context.
      if (rec.combatMode == CombatMode.WEAKEST_ENEMY) {
         return rec.assistTargetRefObj;
      }

      if (rec.ownerCombatContextUntilMillis <= 0L || now > rec.ownerCombatContextUntilMillis) {
         rec.assistTargetRefObj = null;
         rec.assistUntilMillis = 0L;
         return null;
      }

      return rec.assistTargetRefObj;
   }

   static void clearAssist(AmigoNpcManager.NpcRecord rec) {
      if (rec != null) {
         rec.assistTargetRefObj = null;
         rec.assistUntilMillis = 0L;
      }
   }

   static void startCombat(
      AmigoNpcManager.NpcRecord rec, UUID ownerId, Object attackerRefObj, long combatWindowMillis, NpcCombatStateSupport.CombatDebugger debugger
   ) {
      if (ownerId != null && attackerRefObj != null) {
         if (rec != null) {
            if (!rec.downed) {
               if (rec.refObj != null) {
                  if (attackerRefObj != rec.refObj) {
                     long now = System.currentTimeMillis();
                     rec.combatTargetRefObj = attackerRefObj;
                     rec.combatUntilMillis = now + combatWindowMillis;
                     rec.ownerCombatContextUntilMillis = now + combatWindowMillis;
                     rec.npcCombatTargetRefObj = null;
                     rec.npcCombatUntilMillis = 0L;
                     rec.assistTargetRefObj = null;
                     rec.assistUntilMillis = 0L;
                     debugger.log(rec, ownerId, "startCombat: agressorRef=" + attackerRefObj + " (defender=" + (rec.defendeEnabled ? "ON" : "OFF") + ")");
                  }
               }
            }
         }
      }
   }

   static void startNpcCombat(
      AmigoNpcManager.NpcRecord rec, UUID ownerId, Object attackerRefObj, long combatWindowMillis, NpcCombatStateSupport.CombatDebugger debugger
   ) {
      if (rec == null || ownerId == null || attackerRefObj == null || rec.downed) {
         return;
      }

      // AmigoNPC never starts an independent fight because only the companion was hit.
      // Owner-driven combat (startCombat/startAssist) is the only combat context.
      rec.npcCombatTargetRefObj = null;
      rec.npcCombatUntilMillis = 0L;
      debugger.log(rec, ownerId, "npc_attacked: no solo combat");
   }

   static void startAssist(AmigoNpcManager.NpcRecord rec, UUID ownerId, Object targetRefObj, NpcCombatStateSupport.CombatDebugger debugger) {
      if (ownerId != null && targetRefObj != null) {
         if (rec != null && !rec.downed && rec.refObj != null && targetRefObj != rec.refObj) {
            long now = System.currentTimeMillis();
            rec.ownerCombatContextUntilMillis = now + 3000L;

            if (rec.combatMode == CombatMode.WEAKEST_ENEMY) {
               // The owner's attacked entity is an explicit combat opponent, but
               // the companion still chooses the lowest-health valid enemy nearby.
               rec.combatTargetRefObj = targetRefObj;
               rec.combatUntilMillis = now + 3000L;
               rec.assistTargetRefObj = null;
               rec.assistUntilMillis = 0L;
            } else {
               rec.assistTargetRefObj = targetRefObj;
               rec.assistUntilMillis = now + 3000L;
            }

            try {
               ActionTraceService.getShared().record(ownerId, "npc_state", "combat_start source=owner_attack");
            } catch (Throwable ignored) {
            }

            debugger.log(rec, ownerId, "startAssist: targetRef=" + targetRefObj);
         }
      }
   }

   @FunctionalInterface
   interface CombatDebugger {
      void log(AmigoNpcManager.NpcRecord var1, UUID var2, String var3);
   }
}
