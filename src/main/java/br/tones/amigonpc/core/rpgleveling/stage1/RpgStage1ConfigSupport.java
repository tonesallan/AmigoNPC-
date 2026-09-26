package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RpgStage1ConfigSupport {
   private RpgStage1ConfigSupport() {
   }

   static RpgStage1ConfigSupport.Defaults createDefaults() {
      return new RpgStage1ConfigSupport.Defaults(100, 3.0, 1.0, 150.0, 0.0, true, new ArrayList<>(Collections.singletonList("Citizen_")));
   }

   static boolean isEntityRoleLevelingBlacklisted(List<String> blacklistedEntityRoles, String roleName) {
      if (roleName != null && !roleName.isEmpty()) {
         if (blacklistedEntityRoles != null && !blacklistedEntityRoles.isEmpty()) {
            String trimmed = roleName.trim();
            if (trimmed.isEmpty()) {
               return false;
            }

            String lower = trimmed.toLowerCase();

            for (String entry : blacklistedEntityRoles) {
               if (entry != null) {
                  String e = entry.trim();
                  if (!e.isEmpty() && lower.contains(e.toLowerCase())) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static final class Defaults {
      final int maxLevel;
      final double rateExp;
      final double baseXP;
      final double levelBaseXP;
      final double levelOffset;
      final boolean enableGapLevelXpReducer;
      final List<String> blacklistedEntityRoles;

      Defaults(
         int maxLevel,
         double rateExp,
         double baseXP,
         double levelBaseXP,
         double levelOffset,
         boolean enableGapLevelXpReducer,
         List<String> blacklistedEntityRoles
      ) {
         this.maxLevel = maxLevel;
         this.rateExp = rateExp;
         this.baseXP = baseXP;
         this.levelBaseXP = levelBaseXP;
         this.levelOffset = levelOffset;
         this.enableGapLevelXpReducer = enableGapLevelXpReducer;
         this.blacklistedEntityRoles = blacklistedEntityRoles;
      }
   }
}
