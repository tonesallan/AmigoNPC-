package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.rewards.RewardsStateService;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.UUID;

final class AmigoLvlGuiStatsRenderSupport {
   private AmigoLvlGuiStatsRenderSupport() {
   }

   static void updateUIWithPending(UUID ownerId, PlayerRef viewerPlayerRef, Map<String, Integer> pendingChanges, String[] statNames, UICommandBuilder cmd) {
      NpcLevelDataView view = NpcLevelDataView.fromOwner(ownerId);
      int effectiveAvailable = Math.max(0, view.availablePoints - AmigoLvlGuiStatSupport.totalPending(pendingChanges));
      cmd.set("#StatsPageContainer #StatsPage #AvailablePointsLabel.Text", AmigoText.format("ui.stats.available_points", effectiveAvailable));
      AmigoLvlGuiRenderSupport.updateHeader(ownerId, cmd);
      int resetPoints = RewardsStateService.getShared().getResetPoints(ownerId);
      cmd.set("#StatsPageContainer #StatsPage #ResetPointsCount.Text", AmigoText.format("ui.stats.reset_points", resetPoints));
      boolean canReset = resetPoints > 0 || AmigoLvlGuiPermissionSupport.isAdminBestEffort(viewerPlayerRef, ownerId);
      cmd.set("#StatsPageContainer #StatsPage #ResetStatsButton.Disabled", !canReset);

      for (String stat : statNames) {
         boolean editable = AmigoLvlGuiStatSupport.isEditableStat(stat);
         int allocated = view.allocatedPoints.getOrDefault(stat, 0);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsNum.Text", String.valueOf(allocated));
         String valueText = view.statValueText.getOrDefault(stat, "0");
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValueNum.Text", valueText);
         int pending = pendingChanges.getOrDefault(stat, 0);
         boolean hasPending = pending > 0;
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsPending.Visible", hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValuePending.Visible", hasPending);
         if (hasPending) {
            String p = "+" + pending;
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsPending.Text", p);
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValuePending.Text", p);
         } else {
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "PointsPending.Text", "");
            cmd.set("#StatsPageContainer #StatsPage #" + stat + "ValuePending.Text", "");
         }

         boolean canInc = editable && effectiveAvailable > 0;
         boolean canInc5 = editable && effectiveAvailable >= 5;
         if (editable) {
            NpcAttribute attr = AmigoLvlGuiStatSupport.toAttr(stat);
            NpcStatsService svc = NpcStatsService.getShared();
            int cap = attr != null ? svc.maxPointsFor(attr) : 0;
            int already = allocated + pending;
            int room = cap > 0 ? Math.max(0, cap - already) : Integer.MAX_VALUE;
            if (room <= 0) {
               canInc = false;
               canInc5 = false;
            } else if (room < 5) {
               canInc5 = false;
            }
         } else {
            canInc = false;
            canInc5 = false;
         }

         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Increment1.Disabled", !canInc);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Increment5.Disabled", !canInc5);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement1.Disabled", !hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement5.Disabled", !hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement1.Visible", hasPending);
         cmd.set("#StatsPageContainer #StatsPage #" + stat + "Decrement5.Visible", hasPending);
      }

      boolean hasAnyPending = !pendingChanges.isEmpty();
      cmd.set("#StatsPageContainer #StatsPage #SaveButton.Visible", hasAnyPending);
      cmd.set("#StatsPageContainer #StatsPage #CancelButton.Visible", hasAnyPending);
   }
}
