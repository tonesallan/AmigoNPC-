package br.tones.amigonpc.core;

import org.joml.Vector3d;

final class NpcFollowMaintenanceSupport {
   private NpcFollowMaintenanceSupport() {
   }

   static boolean isInCombatOrAssistNow(AmigoNpcManager.NpcRecord rec, long now) {
      return rec != null
         && (NpcCombatStateSupport.getActiveCombatTarget(rec, now) != null
            || NpcCombatStateSupport.getActiveAssistTarget(rec, now) != null);
   }

   static void updateRegenAndAutoLootStick(
      AmigoNpcManager.NpcRecord rec, long now, long regenDelayMillis, Vector3d ownerPos, Vector3d npcPos, boolean inCombatOrAssistNow
   ) {
      if (rec != null) {
         if (inCombatOrAssistNow) {
            rec.regenWasInCombat = true;
            rec.regenStartAtMillis = 0L;
            rec.regenLastApplyMillis = 0L;
         } else {
            if (rec.regenWasInCombat) {
               rec.regenWasInCombat = false;
               if (!rec.godMode && !rec.downed) {
                  rec.regenStartAtMillis = now + regenDelayMillis;
                  rec.regenLastApplyMillis = 0L;
               }

               try {
                  if (rec.autoLootEnabled && rec.state == AmigoNpcManager.State.ACTIVE && !rec.downed) {
                     Vector3d anchorPos = ownerPos != null ? ownerPos : npcPos;
                     if (rec.autoLootStickAnchorPos == null && anchorPos != null) {
                        rec.autoLootStickAnchorPos = anchorPos;
                     }

                     rec.autoLootStickUntilMillis = Math.max(rec.autoLootStickUntilMillis, now + 900L);
                     rec.autoLootStickDeadRefObj = null;
                  }
               } catch (Throwable var9) {
               }
            }
         }
      }
   }

   static boolean shouldRescue(
      AmigoNpcManager.NpcRecord rec,
      long now,
      double horizontal,
      double dy,
      boolean inCombatOrAssist,
      boolean ownerUnderground,
      double undergroundTeleportDistance
   ) {
      if (rec == null) {
         return false;
      }

      boolean stalled = now - rec.lastNpcMovedMillis > 6000L;
      boolean stalledShort = now - rec.lastNpcMovedMillis > 3500L;
      boolean followSoft = horizontal > 25.0 || Math.abs(dy) > 12.0;
      boolean followHard = horizontal > 35.0 || Math.abs(dy) > 18.0;
      boolean farHard = horizontal > 120.0 || Math.abs(dy) > 30.0;
      boolean farSoft = horizontal > 70.0 || Math.abs(dy) > 20.0;
      boolean shouldRescue = false;
      if (rec.farSinceMillis > 0L) {
         long limit = inCombatOrAssist ? 1500L : 2500L;
         if (now - rec.farSinceMillis > limit) {
            shouldRescue = true;
         }
      }

      if (!inCombatOrAssist) {
         shouldRescue = shouldRescue || followHard || followSoft && stalledShort;
      }

      if (!shouldRescue) {
         shouldRescue = farHard || farSoft && stalled;
      }

      double forcedTeleportDist = ownerUnderground ? undergroundTeleportDistance : 35.0;
      return shouldRescue || horizontal > forcedTeleportDist;
   }

   static boolean beginRescue(AmigoNpcManager.NpcRecord rec, long now, long assistGraceMillis, boolean inCombatOrAssist) {
      if (rec == null) {
         return false;
      }

      if (now - rec.lastTeleportMillis <= 1000L) {
         return false;
      }

      rec.lastTeleportMillis = now;
      rec.targetLostSinceMillis = 0L;
      rec.targetStuckSinceMillis = 0L;
      rec.lastTargetHorizontal = -1.0;
      rec.lastTargetSampleMillis = 0L;

      if (inCombatOrAssist) {
         rec.chaseDisengaged = false;
         return true;
      }

      rec.combatUntilMillis = 0L;
      rec.combatTargetRefObj = null;
      rec.npcCombatUntilMillis = 0L;
      rec.npcCombatTargetRefObj = null;
      rec.assistUntilMillis = now + assistGraceMillis;
      rec.assistTargetRefObj = null;
      rec.ownerCombatContextUntilMillis = 0L;
      rec.chaseDisengaged = true;
      rec.currentTargetMobLevel = 0;
      rec.currentTargetMobUuid = null;
      return true;
   }
}
