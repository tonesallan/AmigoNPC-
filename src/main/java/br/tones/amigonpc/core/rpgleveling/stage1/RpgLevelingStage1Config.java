package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RpgLevelingStage1Config {
   private final int maxLevel = 100;
   private final double rateExp = (double)3.0F;
   private final double baseXP = (double)1.0F;
   private final double levelBaseXP = (double)150.0F;
   private final double levelOffset = (double)0.0F;
   private final boolean enableGapLevelXpReducer = true;
   private final List blacklistedEntityRoles = new ArrayList(Collections.singletonList("Citizen_"));

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

   public List getBlacklistedEntityRoles() {
      return this.blacklistedEntityRoles;
   }

   public boolean isEntityRoleLevelingBlacklisted(String roleName) {
      if (roleName != null && !roleName.isEmpty()) {
         List<String> list = this.getBlacklistedEntityRoles();
         if (list != null && !list.isEmpty()) {
            String trimmed = roleName.trim();
            if (trimmed.isEmpty()) {
               return false;
            } else {
               String lower = trimmed.toLowerCase();

               for(String entry : list) {
                  if (entry != null) {
                     String e = entry.trim();
                     if (!e.isEmpty() && lower.contains(e.toLowerCase())) {
                        return true;
                     }
                  }
               }

               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
