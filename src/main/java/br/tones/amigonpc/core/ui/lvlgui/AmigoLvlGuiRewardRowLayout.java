package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import java.util.List;

final class AmigoLvlGuiRewardRowLayout {
   final List<AmigoLevelRewardsConfig.ItemEntry> safeItems;
   final int iconsCount;
   final int iconRows;
   final int iconsHeight;
   final int additionalHeight;
   final String additionalTextEsc;
   final int maxHeight;
   final int totalHeight;
   final int lvlTop;
   final int additionalTop;
   final int statusTop;

   AmigoLvlGuiRewardRowLayout(
      List<AmigoLevelRewardsConfig.ItemEntry> safeItems,
      int iconsCount,
      int iconRows,
      int iconsHeight,
      int additionalHeight,
      String additionalTextEsc,
      int maxHeight,
      int totalHeight,
      int lvlTop,
      int additionalTop,
      int statusTop
   ) {
      this.safeItems = safeItems;
      this.iconsCount = iconsCount;
      this.iconRows = iconRows;
      this.iconsHeight = iconsHeight;
      this.additionalHeight = additionalHeight;
      this.additionalTextEsc = additionalTextEsc;
      this.maxHeight = maxHeight;
      this.totalHeight = totalHeight;
      this.lvlTop = lvlTop;
      this.additionalTop = additionalTop;
      this.statusTop = statusTop;
   }
}
