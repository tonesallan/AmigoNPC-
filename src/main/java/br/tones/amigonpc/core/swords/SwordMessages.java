package br.tones.amigonpc.core.swords;

import br.tones.amigonpc.core.i18n.AmigoText;

public final class SwordMessages {
   private SwordMessages() {
   }

   public static String pickUp(int lvl) {
      return format(AmigoText.pick("sword.up"), lvl);
   }

   public static String pickDown(int lvl) {
      return format(AmigoText.pick("sword.down"), lvl);
   }

   public static String pickEpic(int lvl) {
      return format(AmigoText.pick("sword.epic"), lvl);
   }

   private static String format(String s, int lvl) {
      return s == null ? "" : s.replace("{lvl}", String.valueOf(lvl));
   }
}
