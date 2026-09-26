package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class AmigoLvlGuiBuildSupport {
   private AmigoLvlGuiBuildSupport() {
   }

   static void appendBasePages(UICommandBuilder cmd) {
      AmigoLvlGuiPageAppendSupport.appendBasePages(cmd);
   }

   static void bindStatButtons(UIEventBuilder events) {
      AmigoLvlGuiStatButtonBuildSupport.bindStatButtons(events, AmigoLvlGuiIds.STAT_NAMES, "increment", "decrement");
   }

   static void bindTabButtons(UIEventBuilder events) {
      AmigoLvlGuiEventBindingSupport.addNavBinding(events, "#NavStatsButton", "stats");
      AmigoLvlGuiEventBindingSupport.addNavBinding(events, "#NavLeaderboardButton", "leaderboard");
      AmigoLvlGuiEventBindingSupport.addNavBinding(events, "#NavRewardsButton", "rewards");
      AmigoLvlGuiEventBindingSupport.addNavBinding(events, "#NavRewardsButtonSelected", "rewards");
   }

   static void bindActionButtons(UIEventBuilder events) {
      AmigoLvlGuiEventBindingSupport.addActionBindingWithDefaults(events, "#ClaimAllButton", "claim_all");
      AmigoLvlGuiEventBindingSupport.addActionBindingWithDefaults(events, "#CancelButton", "cancel");
      AmigoLvlGuiEventBindingSupport.addActionBindingWithDefaults(events, "#SaveButton", "save");
      AmigoLvlGuiEventBindingSupport.addActionBindingWithDefaults(events, "#ResetStatsButton", "reset_stats");
   }

   static void bindSettingsButtons(UIEventBuilder events) {
      AmigoLvlGuiSettingsBuildSupport.bindSettingsButtons(events);
   }
}
