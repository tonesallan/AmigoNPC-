package br.tones.amigonpc.core.rpgleveling.stage1;

final class RpgStage1FormulaSupport {
   private RpgStage1FormulaSupport() {
   }

   static double calculateBaseXpFromMonsterLevel(int monsterLevel, RpgLevelingStage1Config cfg) {
      return monsterLevel <= 0 ? 0.0 : cfg.getBaseXP() * Math.pow(monsterLevel, 1.5);
   }

   static double calculateLevelDiffMultiplier(int playerLevel, int monsterLevel, RpgLevelingStage1Config cfg) {
      int diff = monsterLevel - playerLevel;
      if (!cfg.isEnableGapLevelXpReducer() && diff > 0) {
         return 1.0;
      }

      if (diff == 0) {
         return 1.5;
      }

      if (diff >= 1 && diff <= 5) {
         return 1.0 + diff * 0.1;
      }

      if (diff >= 6 && diff <= 25) {
         return 1.0;
      }

      if (diff > 25) {
         if (diff <= 30) {
            return 0.5;
         } else if (diff <= 40) {
            return 0.4;
         } else {
            return diff <= 50 ? 0.3 : 0.2;
         }
      } else if (diff >= -5 && diff <= -1) {
         return 1.5 - Math.abs(diff) * 0.1;
      } else {
         return diff >= -25 && diff <= -6 ? 1.0 - (Math.abs(diff) - 5) * 0.04 : 1.0;
      }
   }

   static double xpRequiredForLevel(int level, RpgLevelingStage1Config cfg) {
      return level <= 1 ? 0.0 : cfg.getLevelBaseXP() * Math.pow(level, 2.5) + cfg.getLevelOffset();
   }

   static double xpFromKill(int playerLevel, int monsterLevel, RpgLevelingStage1Config cfg) {
      if (monsterLevel <= 0) {
         return 0.0;
      }

      if (playerLevel <= 0) {
         return 0.0;
      }

      double base = calculateBaseXpFromMonsterLevel(monsterLevel, cfg);
      double mult = calculateLevelDiffMultiplier(playerLevel, monsterLevel, cfg);
      return base * mult * cfg.getRateExp();
   }

   static double xpFromMaxHealth(double maxHealth, RpgLevelingStage1Config cfg) {
      return !(maxHealth > 0.0) ? 0.0 : Math.sqrt(maxHealth) * cfg.getBaseXP() * cfg.getRateExp();
   }

   static double xpNeededForNextLevel(int level, RpgLevelingStage1Config cfg) {
      if (level >= cfg.getMaxLevel()) {
         return 0.0;
      }

      double a = xpRequiredForLevel(level, cfg);
      double b = xpRequiredForLevel(level + 1, cfg);
      return b - a;
   }
}
