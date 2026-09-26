package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import java.util.List;

final class AmigoLvlGuiRewardRowState {
   final int level;
   final boolean achieved;
   final boolean prevAchieved;
   final boolean isFirstRow;
   final boolean isLastRow;
   final boolean canClaim;
   final String claimText;
   final int resetPoints;
   final String resetPointsText;
   final String commandDisplay;
   final List<AmigoLevelRewardsConfig.ItemEntry> items;

   AmigoLvlGuiRewardRowState(
      int level,
      boolean achieved,
      boolean prevAchieved,
      boolean isFirstRow,
      boolean isLastRow,
      boolean canClaim,
      String claimText,
      int resetPoints,
      String resetPointsText,
      String commandDisplay,
      List<AmigoLevelRewardsConfig.ItemEntry> items
   ) {
      this.level = level;
      this.achieved = achieved;
      this.prevAchieved = prevAchieved;
      this.isFirstRow = isFirstRow;
      this.isLastRow = isLastRow;
      this.canClaim = canClaim;
      this.claimText = claimText;
      this.resetPoints = resetPoints;
      this.resetPointsText = resetPointsText;
      this.commandDisplay = commandDisplay;
      this.items = items;
   }
}
