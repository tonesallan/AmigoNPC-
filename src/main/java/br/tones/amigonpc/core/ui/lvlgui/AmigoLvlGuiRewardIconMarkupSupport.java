package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiRewardIconMarkupSupport {
   private AmigoLvlGuiRewardIconMarkupSupport() {
   }

   static int badgeWidthForQuantity(int quantity) {
      int digits = quantity > 0 ? (int)Math.ceil(Math.log10(quantity + 0.5)) : 1;
      return Math.max(10, 5 + 5 * (1 + digits));
   }

   static void appendItemIconMarkup(StringBuilder sb, String itemId, int quantity, int itemHGap) {
      sb.append("Group { LayoutMode: Left; Padding: (Right:").append(itemHGap).append(");");
      sb.append("Group { LayoutMode: Full; Anchor: (Width: 48, Height: 48); Background: #404858; Padding: 1;");
      sb.append("Group { LayoutMode: Full; Anchor: (Full: 0); Background: #252830(0.92);");
      sb.append("ItemIcon { Anchor: (Left: 3, Top: 3, Width: 40, Height: 40); ItemId: \"").append(itemId).append("\"; }");
      int badgeW = badgeWidthForQuantity(quantity);
      int badgeLeft = 46 - badgeW - 2;
      appendQuantityBadge(sb, quantity, badgeLeft, badgeW);
      sb.append("} } }");
   }

   static void appendResetPointIconMarkup(StringBuilder sb, int quantity, int itemHGap) {
      sb.append("Group { LayoutMode: Left; Padding: (Right:").append(itemHGap).append(");");
      sb.append("Group { LayoutMode: Full; Anchor: (Width: 48, Height: 48); Background: #404858; Padding: 1;");
      sb.append("Group { LayoutMode: Full; Anchor: (Full: 0); Background: #252830(0.92);");
      sb.append("Group { LayoutMode: Full; Anchor: (Full: 0); Background: \"Pages/AmigoNPC_ResetPointIcon.png\";");
      int badgeW = badgeWidthForQuantity(quantity);
      int badgeLeft = 46 - badgeW - 2;
      appendQuantityBadge(sb, quantity, badgeLeft, badgeW);
      sb.append("} } } }");
   }

   private static void appendQuantityBadge(StringBuilder sb, int quantity, int badgeLeft, int badgeWidth) {
      sb.append("Group { LayoutMode: Full; Anchor: (Left:").append(badgeLeft).append(", Bottom: 2, Width:").append(badgeWidth).append(", Height: 12);");
      sb.append("Background: #252830(0.92);");
      sb.append("Label { Style: (FontSize: 9, TextColor: #e8e8e8); Anchor: (Full: 0); Text: \"x").append(quantity).append("\"; }");
      sb.append("}");
   }
}
