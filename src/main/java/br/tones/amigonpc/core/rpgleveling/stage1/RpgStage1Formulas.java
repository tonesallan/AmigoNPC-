package br.tones.amigonpc.core.rpgleveling.stage1;

public final class RpgStage1Formulas {
   private RpgStage1Formulas() {
   }

   public static double calculateBaseXPFromMonsterLevel(int monsterLevel, RpgLevelingStage1Config cfg) {
      return monsterLevel <= 0 ? (double)0.0F : cfg.getBaseXP() * Math.pow((double)monsterLevel, (double)1.5F);
   }

   public static double calculateLevelDiffMultiplier(int playerLevel, int monsterLevel, RpgLevelingStage1Config cfg) {
      int diff = monsterLevel - playerLevel;
      if (!cfg.isEnableGapLevelXpReducer() && diff > 0) {
         return (double)1.0F;
      } else if (diff == 0) {
         return (double)1.5F;
      } else if (diff >= 1 && diff <= 5) {
         return (double)1.0F + (double)diff * 0.1;
      } else if (diff >= 6 && diff <= 25) {
         return (double)1.0F;
      } else if (diff > 25) {
         if (diff <= 30) {
            return (double)0.5F;
         } else if (diff <= 40) {
            return 0.4;
         } else {
            return diff <= 50 ? 0.3 : 0.2;
         }
      } else if (diff >= -5 && diff <= -1) {
         return (double)1.5F - (double)Math.abs(diff) * 0.1;
      } else {
         return diff >= -25 && diff <= -6 ? (double)1.0F - (double)(Math.abs(diff) - 5) * 0.04 : (double)1.0F;
      }
   }

   public static double xpFromKill(int playerLevel, int monsterLevel, RpgLevelingStage1Config cfg) {
      if (monsterLevel <= 0) {
         return (double)0.0F;
      } else if (playerLevel <= 0) {
         return (double)0.0F;
      } else {
         double base = calculateBaseXPFromMonsterLevel(monsterLevel, cfg);
         double mult = calculateLevelDiffMultiplier(playerLevel, monsterLevel, cfg);
         return base * mult * cfg.getRateExp();
      }
   }

   public static double xpFromMaxHealth(double maxHealth, RpgLevelingStage1Config cfg) {
      return !(maxHealth > (double)0.0F) ? (double)0.0F : Math.sqrt(maxHealth) * cfg.getBaseXP() * cfg.getRateExp();
   }

   public static double xpRequiredForLevel(int level, RpgLevelingStage1Config cfg) {
      return level <= 1 ? (double)0.0F : cfg.getLevelBaseXP() * Math.pow((double)level, (double)2.5F) + cfg.getLevelOffset();
   }

   public static double xpNeededForNextLevel(int level, RpgLevelingStage1Config cfg) {
      if (level >= cfg.getMaxLevel()) {
         return (double)0.0F;
      } else {
         double a = xpRequiredForLevel(level, cfg);
         double b = xpRequiredForLevel(level + 1, cfg);
         return b - a;
      }
   }
}
