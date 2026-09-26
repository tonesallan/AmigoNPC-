package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiEventDataReadSupport {
   private AmigoLvlGuiEventDataReadSupport() {
   }

   static String navBarOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null ? data.navBar : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static String actionOrEmpty(AmigoLvlGuiEventData data) {
      try {
         return data != null && data.action != null ? data.action : "";
      } catch (Throwable ignored) {
         return "";
      }
   }

   static String actionOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null ? data.action : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static String statNameOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null ? data.statName : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static String statNameOrEmpty(AmigoLvlGuiEventData data) {
      String value = statNameOrNull(data);
      return value != null ? value : "";
   }

   static Integer amountOrDefault(AmigoLvlGuiEventData data, int defaultValue) {
      try {
         return data != null && data.amount != null ? data.amount : defaultValue;
      } catch (Throwable ignored) {
         return defaultValue;
      }
   }

   static Integer amountOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null && data.amount != null ? data.amount : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static int claimLevelOrDefault(AmigoLvlGuiEventData data, int defaultValue) {
      try {
         return data != null && data.claimLevel != null ? data.claimLevel : defaultValue;
      } catch (Throwable ignored) {
         return defaultValue;
      }
   }

   static Integer claimLevelOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null && data.claimLevel != null ? data.claimLevel : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static String npcNameOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null ? data.npcName : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static String languageOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null ? data.language : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   static String languageTextOrNull(AmigoLvlGuiEventData data) {
      try {
         return data != null ? data.languageText : null;
      } catch (Throwable ignored) {
         return null;
      }
   }
}
