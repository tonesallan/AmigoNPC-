package br.tones.amigonpc.core;

public final class SwordProgression {
   public static final int[] MILESTONES = new int[]{10, 15, 20, 25, 30, 35, 40, 55, 75};
   public static final String CRUDE_SWORD = "Weapon_Sword_Crude";
   public static final String ONYXIUM_SWORD = "Weapon_Sword_Onyxium";
   public static final String RUSTY_STEEL_SWORD = "Weapon_Sword_Steel_Rusty";
   public static final String THORIUM_SWORD = "Weapon_Sword_Thorium";
   public static final String IRON_LONGSWORD = "Weapon_Longsword_Iron";
   public static final String MITHRIL_SWORD = "Weapon_Sword_Mithril";
   public static final String THORIUM_LONGSWORD = "Weapon_Longsword_Thorium";
   public static final String KATANA = "Weapon_Longsword_Katana";
   public static final String FLAME_LONGSWORD = "Weapon_Longsword_Flame";
   public static final String ONYXIUM_LONGSWORD = "Weapon_Longsword_Onyxium";

   private SwordProgression() {
   }

   public static String weaponIdForLevel(int lvl) {
      if (lvl < 1) {
         lvl = 1;
      }

      if (lvl < 10) {
         return "Weapon_Sword_Crude";
      } else if (lvl < 15) {
         return "Weapon_Sword_Onyxium";
      } else if (lvl < 20) {
         return "Weapon_Sword_Steel_Rusty";
      } else if (lvl < 25) {
         return "Weapon_Sword_Thorium";
      } else if (lvl < 30) {
         return "Weapon_Longsword_Iron";
      } else if (lvl < 35) {
         return "Weapon_Sword_Mithril";
      } else if (lvl < 40) {
         return "Weapon_Longsword_Thorium";
      } else if (lvl < 55) {
         return "Weapon_Longsword_Katana";
      } else {
         return lvl < 75 ? "Weapon_Longsword_Flame" : "Weapon_Longsword_Onyxium";
      }
   }

   public static int lastMilestoneCrossed(int oldLvl, int newLvl) {
      if (newLvl <= oldLvl) {
         return -1;
      }

      int last = -1;

      for (int m : MILESTONES) {
         if (oldLvl < m && newLvl >= m) {
            last = m;
         }
      }

      return last;
   }
}
