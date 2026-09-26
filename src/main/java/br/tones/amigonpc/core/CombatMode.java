package br.tones.amigonpc.core;

public enum CombatMode {
   PROTECT_OWNER,
   WEAKEST_ENEMY;

   public static CombatMode fromString(String value) {
      if (value != null) {
         try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
         } catch (Throwable ignored) {
         }
      }

      return PROTECT_OWNER;
   }
}
