package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import java.util.ArrayList;
import java.util.List;

final class AmigoLvlGuiRewardRowLayoutSupport {
   private AmigoLvlGuiRewardRowLayoutSupport() {
   }

   static AmigoLvlGuiRewardRowLayout build(
      List<AmigoLevelRewardsConfig.ItemEntry> items, int resetPoints, String commandDisplay, boolean isFirstRow, int iconsPerRow, int iconRowHeight
   ) {
      List<AmigoLevelRewardsConfig.ItemEntry> safeItems = new ArrayList<>();
      if (items != null) {
         for (AmigoLevelRewardsConfig.ItemEntry it : items) {
            if (it != null && it.ItemId != null && !it.ItemId.isEmpty() && it.Quantity > 0) {
               safeItems.add(it);
            }
         }
      }

      int iconsCount = safeItems.size() + (resetPoints > 0 ? 1 : 0);
      int iconRows = iconsCount == 0 ? 0 : (iconsCount + iconsPerRow - 1) / iconsPerRow;
      int iconsHeight = iconRows == 0 ? iconRowHeight : iconRows * iconRowHeight + 8;
      List<String> lines = AmigoLvlGuiRewardTextSupport.wrapTextToLines(commandDisplay != null ? commandDisplay : "");
      int lineCount = Math.max(1, lines.size());
      int additionalHeight = lineCount * 18 + 8;
      String additionalTextEsc = lines.isEmpty() ? "" : AmigoLvlGuiRewardTextSupport.escapeUiStringMultiline(String.join("\n", lines));
      int maxHeight = Math.max(70, Math.max(iconsHeight, additionalHeight + 20));
      int topPad = isFirstRow ? 10 : 0;
      int totalHeight = maxHeight + topPad + 5;
      int lvlTop = isFirstRow ? 10 : 20;
      int additionalTop = isFirstRow ? 10 : 20;
      int statusTop = isFirstRow ? 15 : 25;
      return new AmigoLvlGuiRewardRowLayout(
         safeItems, iconsCount, iconRows, iconsHeight, additionalHeight, additionalTextEsc, maxHeight, totalHeight, lvlTop, additionalTop, statusTop
      );
   }
}
