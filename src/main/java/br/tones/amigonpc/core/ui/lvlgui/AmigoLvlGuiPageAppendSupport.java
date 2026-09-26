package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

final class AmigoLvlGuiPageAppendSupport {
   private AmigoLvlGuiPageAppendSupport() {
   }

   static void appendBasePages(UICommandBuilder cmd) {
      cmd.append("Pages/AmigoNPC_Leveling_Main.ui");
      cmd.append("#StatsPageContainer", "Pages/AmigoNPC_Leveling_StatsPage.ui");
      cmd.append("#RpgLvlLeaderboardPageContainer", "Pages/AmigoNPC_Leveling_LeaderboardPage.ui");
      cmd.append("#RewardsPageContainer", "Pages/AmigoNPC_Leveling_RewardsPage.ui");
      cmd.append("#StatsPageContainer #PlayerHeaderContainer", "Pages/AmigoNPC_Leveling_PlayerHeader.ui");
      cmd.append("#RewardsPageContainer #PlayerHeaderContainer", "Pages/AmigoNPC_Leveling_PlayerHeader.ui");
   }
}
