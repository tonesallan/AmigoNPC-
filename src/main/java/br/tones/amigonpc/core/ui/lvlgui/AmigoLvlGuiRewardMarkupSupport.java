package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import java.util.List;

final class AmigoLvlGuiRewardMarkupSupport {
   private AmigoLvlGuiRewardMarkupSupport() {
   }

   static String formatCommandDisplay(String command, String commandTitle) {
      return AmigoLvlGuiRewardTextSupport.formatCommandDisplay(command, commandTitle);
   }

   static String buildRewardRowMarkup(
      int level,
      boolean isFirstRow,
      boolean isLastRow,
      boolean achieved,
      boolean prevAchieved,
      List<AmigoLevelRewardsConfig.ItemEntry> items,
      int resetPoints,
      String resetPointsText,
      String commandDisplay,
      String claimText,
      boolean canClaim
   ) {
      String claimTextEsc = AmigoLvlGuiRewardTextSupport.escapeUiString(claimText);
      String statusColor = canClaim ? "#90ee90" : "#888888";
      String levelColor = achieved ? "#5a8bd8" : "#e8e8e8";
      int width = 660;
      int lvlW = 76;
      int rewardsW = 240;
      int additionalW = 194;
      int statusW = 120;
      int leftPad = 15;
      int itemHGap = 10;
      int iconsPerRow = 4;
      int iconRowHeight = 52;
      AmigoLvlGuiRewardRowLayout layout = AmigoLvlGuiRewardRowLayoutSupport.build(items, resetPoints, commandDisplay, isFirstRow, 4, 52);
      StringBuilder sb = new StringBuilder();
      sb.append("Group {");
      sb.append("LayoutMode: Left; Anchor: (Width:")
         .append(660)
         .append(", Height:")
         .append(layout.totalHeight)
         .append(", Bottom: 6); Background: #1a1f26(0.7); Padding: (Left:")
         .append(15)
         .append(", Right:")
         .append(0)
         .append(", Top:")
         .append(isFirstRow ? 10 : 0)
         .append(", Bottom: 5);");
      sb.append("Label { Style: (FontSize: 15, TextColor: ")
         .append(levelColor)
         .append(", RenderBold: true); Anchor: (Top:")
         .append(layout.lvlTop)
         .append(", Width:")
         .append(76)
         .append("); Text: \"LVL")
         .append(level)
         .append("\"; }");
      sb.append("Group { LayoutMode: Top; Anchor: (Width:").append(240).append(", Height:").append(layout.iconsHeight).append("); Padding: (Right: 8);");
      int iconIndex = 0;

      for (int r = 0; r < layout.iconRows; r++) {
         sb.append("Group { LayoutMode: Left; Anchor: (Height:").append(52).append(");");

         for (int c = 0; c < 4 && iconIndex < layout.iconsCount; c++) {
            if (iconIndex < layout.safeItems.size()) {
               AmigoLevelRewardsConfig.ItemEntry it = layout.safeItems.get(iconIndex);
               String itemId = AmigoLvlGuiRewardTextSupport.escapeUiString(it.ItemId);
               int qty = it.Quantity;
               AmigoLvlGuiRewardIconMarkupSupport.appendItemIconMarkup(sb, itemId, qty, 10);
            } else {
               int qty = resetPoints;
               AmigoLvlGuiRewardIconMarkupSupport.appendResetPointIconMarkup(sb, qty, 10);
            }

            iconIndex++;
         }

         sb.append("}");
      }

      sb.append("}");
      sb.append("Label { Style: (FontSize: 14, TextColor: #c0c0c0); Anchor: (Top:")
         .append(layout.additionalTop)
         .append(", Width:")
         .append(194)
         .append(", Height:")
         .append(layout.additionalHeight)
         .append("); Padding: (Left: 0); Text: \"")
         .append(layout.additionalTextEsc)
         .append("\"; }");
      sb.append("Group { FlexWeight: 1; Anchor: (Height:").append(layout.maxHeight).append("); }");
      sb.append("Group { LayoutMode: Left; Anchor: (Width:")
         .append(120)
         .append(", Height:")
         .append(layout.maxHeight)
         .append("); Padding: (Left: 4, Right: 4);");
      sb.append("Label { Style: (FontSize: 14, TextColor: ")
         .append(statusColor)
         .append("); Anchor: (Top:")
         .append(layout.statusTop)
         .append(", Width: 96, Height: 35); Text: \"")
         .append(claimTextEsc)
         .append("\"; }");
      sb.append("}");
      sb.append("}");
      return sb.toString();
   }
}
