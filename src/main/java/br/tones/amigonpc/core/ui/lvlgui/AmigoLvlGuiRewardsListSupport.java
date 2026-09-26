package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class AmigoLvlGuiRewardsListSupport {
   private AmigoLvlGuiRewardsListSupport() {
   }

   static AmigoLvlGuiRewardsListData collect(AmigoLevelRewardsConfig cfg) {
      List<AmigoLevelRewardsConfig.RewardEntry> rewards = new ArrayList<>();
      if (cfg != null && cfg.Rewards != null) {
         for (AmigoLevelRewardsConfig.RewardEntry reward : cfg.Rewards) {
            if (reward != null && reward.Level > 0) {
               rewards.add(reward);
            }
         }
      }

      rewards.sort(Comparator.comparingInt(entry -> entry.Level));
      int firstLevel = rewards.isEmpty() ? -1 : rewards.get(0).Level;
      int lastLevel = rewards.isEmpty() ? -1 : rewards.get(rewards.size() - 1).Level;
      return new AmigoLvlGuiRewardsListData(rewards, firstLevel, lastLevel);
   }
}
