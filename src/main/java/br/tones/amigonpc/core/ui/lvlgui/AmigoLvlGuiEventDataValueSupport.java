package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiEventDataValueSupport {
   private AmigoLvlGuiEventDataValueSupport() {
   }

   static String stringOrDefault(String value, String defaultValue) {
      return value != null ? value : defaultValue;
   }

   static Integer parseIntegerOrDefault(String raw, Integer defaultValue) {
      if (raw != null && !raw.isEmpty()) {
         try {
            return Integer.parseInt(raw.trim());
         } catch (NumberFormatException ignored) {
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   static Double parseDoubleOrNull(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            return Double.parseDouble(raw.trim());
         } catch (NumberFormatException ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   static String intToString(Integer value, String defaultValue) {
      return value != null ? String.valueOf(value) : defaultValue;
   }

   static String doubleToString(Double value, String defaultValue) {
      return value != null ? String.valueOf(value) : defaultValue;
   }
}
