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
      return rec == null ? null : rec.assistTargetRefObj;
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
                     rec.combatTargetRefObj = attackerRefObj;
                     rec.combatUntilMillis = System.currentTimeMillis() + combatWindowMillis;
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
      if (ownerId != null && attackerRefObj != null) {
         if (rec != null) {
            if (!rec.downed) {
               if (rec.refObj != null) {
                  if (attackerRefObj != rec.refObj) {
                     long now = System.currentTimeMillis();
                     if (getActiveCombatTarget(rec, now) == null) {
                        rec.npcCombatTargetRefObj = attackerRefObj;
                        rec.npcCombatUntilMillis = now + combatWindowMillis;

                        try {
                           ActionTraceService.getShared().record(ownerId, "npc_state", "combat_start source=npc_attacked");
                        } catch (Throwable var9) {
                        }

                        debugger.log(rec, ownerId, "startNpcCombat: agressorRef=" + attackerRefObj);
                     }
                  }
               }
            }
         }
      }
   }

   static void startAssist(AmigoNpcManager.NpcRecord rec, UUID ownerId, Object targetRefObj, NpcCombatStateSupport.CombatDebugger debugger) {
      if (ownerId != null && targetRefObj != null) {
         if (rec != null) {
            if (rec.defendeEnabled) {
               if (!rec.downed) {
                  if (rec.refObj != null) {
                     if (targetRefObj != rec.refObj) {
                        long now = System.currentTimeMillis();
                        if (getActiveCombatTarget(rec, now) == null) {
                           if (rec.assistTargetRefObj == null) {
                              rec.assistTargetRefObj = targetRefObj;
                              rec.assistUntilMillis = 0L;

                              try {
                                 ActionTraceService.getShared().record(ownerId, "npc_state", "combat_start source=assist");
                              } catch (Throwable var7) {
                              }

                              debugger.log(rec, ownerId, "startAssist: targetRef=" + targetRefObj);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @FunctionalInterface
   interface CombatDebugger {
      void log(AmigoNpcManager.NpcRecord var1, UUID var2, String var3);
   }
}
