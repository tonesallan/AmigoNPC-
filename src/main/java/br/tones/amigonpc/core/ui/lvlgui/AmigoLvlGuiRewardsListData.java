package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import java.util.List;

final class AmigoLvlGuiRewardsListData {
   final List<AmigoLevelRewardsConfig.RewardEntry> rewards;
   final int firstLevel;
   final int lastLevel;

   AmigoLvlGuiRewardsListData(List<AmigoLevelRewardsConfig.RewardEntry> rewards, int firstLevel, int lastLevel) {
      this.rewards = rewards;
      this.firstLevel = firstLevel;
      this.lastLevel = lastLevel;
   }
}
