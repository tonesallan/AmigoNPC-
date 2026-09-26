package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.debug.ActionTraceService;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.UUID;

final class AmigoLvlGuiMainActionSupport {
   private AmigoLvlGuiMainActionSupport() {
   }

   static AmigoLvlGuiMainActionResult handleAction(
      UUID ownerId, PlayerRef viewerPlayerRef, Map<String, Integer> pendingChanges, AmigoLvlGuiEventData data, String action
   ) {
      if ("reset_stats".equals(action)) {
         trace(ownerId, "reset_stats");
         AmigoLvlGuiStatsResetResult resetResult = AmigoLvlGuiStatsActionSupport.resetStats(ownerId, viewerPlayerRef, pendingChanges);
         return resetResult.error
            ? new AmigoLvlGuiMainActionResult(true, true, false, false, resetResult.message)
            : new AmigoLvlGuiMainActionResult(true, true, false, true, null);
      }

      if ("cancel".equals(action)) {
         trace(ownerId, "cancel");
         AmigoLvlGuiStatsActionSupport.cancelChanges(pendingChanges);
         return new AmigoLvlGuiMainActionResult(true, true, false, true, null);
      }

      if ("save".equals(action)) {
         trace(ownerId, "save");
         AmigoLvlGuiStatsSaveResult saveResult = AmigoLvlGuiStatsActionSupport.savePendingStats(ownerId, pendingChanges);
         return new AmigoLvlGuiMainActionResult(true, true, false, false, saveResult.errorMessage);
      }

      if ("claim_all".equals(action)) {
         trace(ownerId, "claim_all");
         AmigoLvlGuiClaimSupport.claimAll(ownerId, viewerPlayerRef);
         return new AmigoLvlGuiMainActionResult(true, true, true, false, null);
      }

      if ("claim".equals(action)) {
         int rewardLevel = AmigoLvlGuiEventDataReadSupport.claimLevelOrDefault(data, 0);

         try {
            ActionTraceService.getShared().record(ownerId, "ui_action", "claim level=" + rewardLevel);
         } catch (Throwable var7) {
         }

         return AmigoLvlGuiClaimSupport.claimLevel(ownerId, viewerPlayerRef, rewardLevel)
            ? new AmigoLvlGuiMainActionResult(true, true, true, false, null)
            : new AmigoLvlGuiMainActionResult(true, false, false, false, null);
      } else {
         if (!"increment".equals(action) && !"decrement".equals(action)) {
            return new AmigoLvlGuiMainActionResult(false, false, false, false, null);
         }

         try {
            String statT = AmigoLvlGuiEventDataReadSupport.statNameOrEmpty(data);
            int amtT = AmigoLvlGuiEventDataReadSupport.amountOrDefault(data, 0);
            ActionTraceService.getShared().record(ownerId, "ui_action", action + " stat=" + statT + " amount=" + amtT);
         } catch (Throwable var8) {
         }

         AmigoLvlGuiPendingActionResult pendingResult = AmigoLvlGuiPendingEventSupport.handlePendingStatAction(
            ownerId, pendingChanges, data, action, "increment", "decrement"
         );
         return !pendingResult.accepted
            ? new AmigoLvlGuiMainActionResult(true, false, false, false, null)
            : new AmigoLvlGuiMainActionResult(true, true, false, pendingResult.changed, null);
      }
   }

   private static void trace(UUID ownerId, String detail) {
      try {
         ActionTraceService.getShared().record(ownerId, "ui_action", detail);
      } catch (Throwable var3) {
      }
   }
}
