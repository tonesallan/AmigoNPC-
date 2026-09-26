package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.List;

public final class RpgLevelingStage1Config {
   private final int maxLevel;
   private final double rateExp;
   private final double baseXP;
   private final double levelBaseXP;
   private final double levelOffset;
   private final boolean enableGapLevelXpReducer;
   private final List<String> blacklistedEntityRoles;

   public RpgLevelingStage1Config() {
      RpgStage1ConfigSupport.Defaults defaults = RpgStage1ConfigSupport.createDefaults();
      this.maxLevel = defaults.maxLevel;
      this.rateExp = defaults.rateExp;
      this.baseXP = defaults.baseXP;
      this.levelBaseXP = defaults.levelBaseXP;
      this.levelOffset = defaults.levelOffset;
      this.enableGapLevelXpReducer = defaults.enableGapLevelXpReducer;
      this.blacklistedEntityRoles = defaults.blacklistedEntityRoles;
   }

   public int getMaxLevel() {
      return this.maxLevel;
   }

   public double getRateExp() {
      return this.rateExp;
   }

   public double getBaseXP() {
      return this.baseXP;
   }

   public double getLevelBaseXP() {
      return this.levelBaseXP;
   }

   public double getLevelOffset() {
      return this.levelOffset;
   }

   public boolean isEnableGapLevelXpReducer() {
      return this.enableGapLevelXpReducer;
   }

   public List<String> getBlacklistedEntityRoles() {
      return this.blacklistedEntityRoles;
   }

   public boolean isEntityRoleLevelingBlacklisted(String roleName) {
      return RpgStage1ConfigSupport.isEntityRoleLevelingBlacklisted(this.getBlacklistedEntityRoles(), roleName);
   }
}
