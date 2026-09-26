package br.tones.amigonpc.core.swords;

public final class SwordProgression {
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
   public static final int M10 = 10;
   public static final int M15 = 15;
   public static final int M20 = 20;
   public static final int M25 = 25;
   public static final int M30 = 30;
   public static final int M35 = 35;
   public static final int M40 = 40;
   public static final int M55 = 55;
   public static final int M75 = 75;

   private SwordProgression() {
   }

   public static int clampLevel(int lvl) {
      return Math.max(1, lvl);
   }

   public static String weaponIdForLevel(int level) {
      int lvl = clampLevel(level);
      if (lvl >= 75) {
         return "Weapon_Longsword_Onyxium";
      } else if (lvl >= 55) {
         return "Weapon_Longsword_Flame";
      } else if (lvl >= 40) {
         return "Weapon_Longsword_Katana";
      } else if (lvl >= 35) {
         return "Weapon_Longsword_Thorium";
      } else if (lvl >= 30) {
         return "Weapon_Sword_Mithril";
      } else if (lvl >= 25) {
         return "Weapon_Longsword_Iron";
      } else if (lvl >= 20) {
         return "Weapon_Sword_Thorium";
      } else if (lvl >= 15) {
         return "Weapon_Sword_Steel_Rusty";
      } else {
         return lvl >= 10 ? "Weapon_Sword_Onyxium" : "Weapon_Sword_Crude";
      }
   }

   public static boolean weaponChanges(int oldLevel, int newLevel) {
      return !weaponIdForLevel(oldLevel).equals(weaponIdForLevel(newLevel));
   }

   public static boolean isMilestoneLevel(int level) {
      int lvl = clampLevel(level);
      return lvl == 10 || lvl == 15 || lvl == 20 || lvl == 25 || lvl == 30 || lvl == 35 || lvl == 40 || lvl == 55 || lvl == 75;
   }

   public static boolean crossedAnyMilestone(int oldLevel, int newLevel) {
      int a = clampLevel(oldLevel);
      int b = clampLevel(newLevel);
      if (b <= a) {
         return false;
      }

      int[] ms = new int[]{10, 15, 20, 25, 30, 35, 40, 55, 75};

      for (int m : ms) {
         if (a < m && b >= m) {
            return true;
         }
      }

      return false;
   }
}
