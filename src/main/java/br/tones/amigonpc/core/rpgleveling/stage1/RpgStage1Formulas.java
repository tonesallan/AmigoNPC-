package br.tones.amigonpc.core.rpgleveling.stage1;

public final class RpgStage1Formulas {
   private RpgStage1Formulas() {
   }

   public static double calculateBaseXPFromMonsterLevel(int monsterLevel, RpgLevelingStage1Config cfg) {
      return RpgStage1FormulaSupport.calculateBaseXpFromMonsterLevel(monsterLevel, cfg);
   }

   public static double calculateLevelDiffMultiplier(int playerLevel, int monsterLevel, RpgLevelingStage1Config cfg) {
      return RpgStage1FormulaSupport.calculateLevelDiffMultiplier(playerLevel, monsterLevel, cfg);
   }

   public static double xpFromKill(int playerLevel, int monsterLevel, RpgLevelingStage1Config cfg) {
      return RpgStage1FormulaSupport.xpFromKill(playerLevel, monsterLevel, cfg);
   }

   public static double xpFromMaxHealth(double maxHealth, RpgLevelingStage1Config cfg) {
      return RpgStage1FormulaSupport.xpFromMaxHealth(maxHealth, cfg);
   }

   public static double xpRequiredForLevel(int level, RpgLevelingStage1Config cfg) {
      return RpgStage1FormulaSupport.xpRequiredForLevel(level, cfg);
   }

   public static double xpNeededForNextLevel(int level, RpgLevelingStage1Config cfg) {
      return RpgStage1FormulaSupport.xpNeededForNextLevel(level, cfg);
   }
}
