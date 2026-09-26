package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.UUID;

final class AmigoLvlGuiRenderSupport {
   private AmigoLvlGuiRenderSupport() {
   }

   static void applyNavBarTab(UICommandBuilder cmd, String tab) {
      boolean isStats = "stats".equals(tab);
      boolean isLeaderboard = "leaderboard".equals(tab);
      boolean isRewards = "rewards".equals(tab);
      cmd.set("#StatsPageContainer #StatsPage.Visible", isStats);
      cmd.set("#RpgLvlLeaderboardPageContainer #RpgLvlLeaderboardPage.Visible", isLeaderboard);
      cmd.set("#RewardsPageContainer #RewardsPage.Visible", isRewards);
      cmd.set("#NavStatsButtonSelected.Visible", isStats);
      cmd.set("#NavStatsButton.Visible", !isStats);
      cmd.set("#NavLeaderboardButtonSelected.Visible", isLeaderboard);
      cmd.set("#NavLeaderboardButton.Visible", !isLeaderboard);
      cmd.set("#NavRewardsButtonSelected.Visible", isRewards);
      cmd.set("#NavRewardsButton.Visible", !isRewards);
   }

   static void updateHeader(UUID ownerId, UICommandBuilder cmd) {
      br.tones.amigonpc.core.AmigoNpcManager mgr = br.tones.amigonpc.core.AmigoNpcManager.getShared();
      String displayName = mgr.getNpcDisplayName(ownerId);
      String activity = activityText(mgr.getActivityStateName(ownerId));
      String healthText = AmigoText.format(
         "ui.header.health",
         Math.round(mgr.getCachedHealth(ownerId)),
         Math.round(mgr.getCachedMaxHealth(ownerId))
      );
      String nameText = AmigoText.format("ui.header.npc_name", displayName);
      String statusText = AmigoText.format("ui.header.status", activity);
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderNameLabel.Text", nameText);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderNameLabel.Text", nameText);
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderStatusLabel.Text", statusText);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderStatusLabel.Text", statusText);
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderHealthLabel.Text", healthText);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderHealthLabel.Text", healthText);

      NpcLevelDataView view = NpcLevelDataView.fromOwner(ownerId);
      String levelText = AmigoText.format("ui.header.level", view.level);
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderLevelLabel.Text", levelText);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderLevelLabel.Text", levelText);
      float prog = view.progress01();
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderProgressBar.Value", prog);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderProgressBar.Value", prog);
      int pct = (int)Math.floor(prog * 100.0F);
      String xpText = AmigoText.format("ui.header.xp_progress", view.xp, view.xpNeeded, view.xpNeeded <= 0L ? 0 : pct);
      cmd.set("#StatsPageContainer #StatsPage #PlayerHeaderXPProgressLabel.Text", xpText);
      cmd.set("#RewardsPageContainer #RewardsPage #PlayerHeaderXPProgressLabel.Text", xpText);
   }
   private static String activityText(String state) {
      if (state == null) {
         return AmigoText.text("ui.status.idle");
      }

      return switch (state) {
         case "INACTIVE" -> AmigoText.text("ui.status.inactive");
         case "DOWNED" -> AmigoText.text("ui.status.downed");
         case "COMBAT" -> AmigoText.text("ui.status.combat");
         case "GATHERING" -> AmigoText.text("ui.status.gathering");
         case "FOLLOWING" -> AmigoText.text("ui.status.following");
         default -> AmigoText.text("ui.status.idle");
      };
   }

}
