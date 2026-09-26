package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import java.util.List;
import java.util.Set;

final class AmigoLvlGuiRewardRowStateSupport {
   private AmigoLvlGuiRewardRowStateSupport() {
   }

   static AmigoLvlGuiRewardRowState build(
      AmigoLevelRewardsConfig.RewardEntry reward, int npcLevel, Set<Integer> claimedRewardLevels, int prevLevel, int firstLevel, int lastLevel
   ) {
      int level = reward.Level;
      boolean achieved = level <= npcLevel;
      boolean alreadyClaimed = claimedRewardLevels != null && claimedRewardLevels.contains(level);
      boolean canClaim = achieved && !alreadyClaimed;
      boolean prevAchieved = prevLevel > 0 && prevLevel <= npcLevel || prevLevel == 0 && achieved;
      boolean isFirstRow = level == firstLevel;
      boolean isLastRow = level == lastLevel;
      String claimText = alreadyClaimed
         ? AmigoText.text("ui.rewards.row.claimed")
         : (canClaim ? AmigoText.text("ui.rewards.row.unclaim") : AmigoText.text("ui.rewards.row.unknown"));
      int resetPoints = reward.ResetPoints;
      String resetPointsText = resetPoints > 0 ? AmigoText.format("ui.rewards.row.reset_points", resetPoints) : "";
      String commandDisplay = AmigoLvlGuiRewardMarkupSupport.formatCommandDisplay(reward.Command, reward.CommandTitle);
      List<AmigoLevelRewardsConfig.ItemEntry> items = reward.Items != null ? reward.Items : List.of();
      return new AmigoLvlGuiRewardRowState(
         level, achieved, prevAchieved, isFirstRow, isLastRow, canClaim, claimText, resetPoints, resetPointsText, commandDisplay, items
      );
   }
}
