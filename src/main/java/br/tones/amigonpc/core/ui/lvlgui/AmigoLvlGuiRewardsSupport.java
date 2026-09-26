package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfigService;
import br.tones.amigonpc.core.rewards.RewardsState;
import br.tones.amigonpc.core.rewards.RewardsStateService;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.UUID;

final class AmigoLvlGuiRewardsSupport {
   private AmigoLvlGuiRewardsSupport() {
   }

   static void updateRewardsList(UUID ownerId, UICommandBuilder cmd) {
      AmigoLevelRewardsConfig cfg = AmigoLevelRewardsConfigService.getShared().get();
      RewardsState st = RewardsStateService.getShared().load(ownerId);
      int npcLevel = AmigoNpcManager.getShared().getNpcLevel(ownerId);
      String rowsSel = "#RewardsPage #RewardRows";
      cmd.clear("#RewardsPage #RewardRows");
      if (cfg != null && cfg.Rewards != null && !cfg.Rewards.isEmpty()) {
         AmigoLvlGuiRewardsListData rewardsData = AmigoLvlGuiRewardsListSupport.collect(cfg);
         int prevLevel = 0;
         boolean anyClaimable = false;

         for (AmigoLevelRewardsConfig.RewardEntry reward : rewardsData.rewards) {
            AmigoLvlGuiRewardRowState rowState = AmigoLvlGuiRewardRowStateSupport.build(
               reward, npcLevel, st.claimedRewardLevels, prevLevel, rewardsData.firstLevel, rewardsData.lastLevel
            );
            if (rowState.canClaim) {
               anyClaimable = true;
            }

            String rowMarkup = AmigoLvlGuiRewardMarkupSupport.buildRewardRowMarkup(
               rowState.level,
               rowState.isFirstRow,
               rowState.isLastRow,
               rowState.achieved,
               rowState.prevAchieved,
               rowState.items,
               rowState.resetPoints,
               rowState.resetPointsText,
               rowState.commandDisplay,
               rowState.claimText,
               rowState.canClaim
            );
            cmd.appendInline("#RewardsPage #RewardRows", rowMarkup);
            prevLevel = rowState.level;
         }

         cmd.set("#RewardsPageContainer #RewardsPage #ClaimAllButton.Visible", anyClaimable);
      } else {
         cmd.set("#RewardsPageContainer #RewardsPage #ClaimAllButton.Visible", false);
      }
   }
}
