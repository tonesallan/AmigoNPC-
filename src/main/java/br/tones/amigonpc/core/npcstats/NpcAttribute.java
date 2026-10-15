package br.tones.amigonpc.core.npcstats;

public enum NpcAttribute {
   HEALTH,
   DAMAGE,
   CRITICAL_DAMAGE,
   DEFENSE;

   public static NpcAttribute parse(String s) {
      if (s == null) {
         return null;
      } else if (t.isEmpty()) {
         return null;
      } else {
         t = t.replace('-', '_');
         switch (t.replace(' ', '_')) {
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
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   // $FF: synthetic method
   private static NpcAttribute[] $values() {
      return new NpcAttribute[]{HEALTH, DAMAGE, CRITICAL_DAMAGE, DEFENSE};
   }
}
