package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.rewards.RewardsStateService;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

final class AmigoLvlGuiStatsActionSupport {
   private AmigoLvlGuiStatsActionSupport() {
   }

   static AmigoLvlGuiStatsResetResult resetStats(UUID ownerId, PlayerRef viewerPlayerRef, Map<String, Integer> pendingChanges) {
      boolean isAdmin = AmigoLvlGuiPermissionSupport.isAdminBestEffort(viewerPlayerRef, ownerId);
      int resetPoints = RewardsStateService.getShared().getResetPoints(ownerId);
      if (!isAdmin) {
         if (resetPoints <= 0) {
            return new AmigoLvlGuiStatsResetResult(true, AmigoText.text("ui.stats.reset.no_points"));
         }

         boolean consumed = RewardsStateService.getShared().consumeResetPoint(ownerId);
         if (!consumed) {
            return new AmigoLvlGuiStatsResetResult(true, AmigoText.text("ui.stats.reset.consume_failed"));
         }
      }

      pendingChanges.clear();
      NpcStatsService.getShared().resetAll(ownerId);

      try {
         AmigoNpcManager.getShared().requestRescale(ownerId);
      } catch (Throwable var6) {
      }

      return new AmigoLvlGuiStatsResetResult(false, null);
   }

   static void cancelChanges(Map<String, Integer> pendingChanges) {
      pendingChanges.clear();
   }

   static AmigoLvlGuiStatsSaveResult savePendingStats(UUID ownerId, Map<String, Integer> pendingChanges) {
      int npcLevel = AmigoNpcManager.getShared().getNpcLevel(ownerId);
      NpcStatsService svc = NpcStatsService.getShared();
      String errorMessage = null;

      for (Entry<String, Integer> e : new HashMap<>(pendingChanges).entrySet()) {
         String stat = e.getKey();
         int amt = e.getValue() == null ? 0 : e.getValue();
         if (amt > 0) {
            NpcAttribute attr = AmigoLvlGuiStatSupport.toAttr(stat);
            if (attr != null) {
               NpcStatsService.AddResult res = svc.add(ownerId, npcLevel, attr, amt);
               if (!res.ok()) {
                  errorMessage = AmigoText.format("ui.stats.save.failed_apply", stat, res.error());
               }
            }
         }
      }

      pendingChanges.clear();

      try {
         AmigoNpcManager.getShared().requestRescale(ownerId);
      } catch (Throwable var11) {
      }

      return new AmigoLvlGuiStatsSaveResult(errorMessage);
   }
}
