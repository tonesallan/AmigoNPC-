package br.tones.amigonpc.core.npcstats;

public enum NpcAttribute {
   HEALTH,
   DAMAGE,
   CRITICAL_DAMAGE,
   DEFENSE;

   public static NpcAttribute parse(String s) {
      if (s == null) {
         return null;
      }

      String t = s.trim().toUpperCase();
      if (t.isEmpty()) {
         return null;
      }

      t = t.replace('-', '_');
      t = t.replace(' ', '_');
      switch (t) {
         case "HP":
         case "HITPOINT":
         case "HITPOINTS":
         case "LIFE":
         case "VIDA":
            t = "HEALTH";
            break;
         case "DMG":
         case "ATK":
         case "ATTACK":
         case "DANO":
            t = "DAMAGE";
            break;
         case "CRIT":
         case "CRITICAL":
         case "CRITICALDAMAGE":
         case "CRITICAL_DAMAGE":
            t = "CRITICAL_DAMAGE";
            break;
         case "DEF":
         case "ARMOR":
         case "ARMOUR":
         case "DEFESA":
            t = "DEFENSE";
      }

      try {
         return valueOf(t);
      } catch (Throwable ignored) {
         return null;
      }
   }
}
