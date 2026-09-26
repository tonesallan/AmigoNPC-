package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.npcstats.NpcAttribute;
import java.util.Map;
import java.util.Map.Entry;

final class AmigoLvlGuiStatSupport {
   private AmigoLvlGuiStatSupport() {
   }

   static boolean isEditableStat(String stat) {
      return "Health".equals(stat) || "Damage".equals(stat) || "CriticalDamage".equals(stat) || "Defense".equals(stat);
   }

   static NpcAttribute toAttr(String stat) {
      if ("Health".equals(stat)) {
         return NpcAttribute.HEALTH;
      } else if ("Damage".equals(stat)) {
         return NpcAttribute.DAMAGE;
      } else if ("CriticalDamage".equals(stat)) {
         return NpcAttribute.CRITICAL_DAMAGE;
      } else {
         return "Defense".equals(stat) ? NpcAttribute.DEFENSE : null;
      }
   }

   static int totalPending(Map<String, Integer> pendingChanges) {
      int sum = 0;

      for (Entry<String, Integer> e : pendingChanges.entrySet()) {
         if (e.getKey() != null && isEditableStat(e.getKey())) {
            Integer v = e.getValue();
            if (v != null && v > 0) {
               sum += v;
            }
         }
      }

      return sum;
   }
}
