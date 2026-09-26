package br.tones.amigonpc.core.npcstats;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public final class NpcStatsState {
   public final Map<NpcAttribute, Integer> allocations = new EnumMap<>(NpcAttribute.class);

   public int getAllocated(NpcAttribute a) {
      if (a == null) {
         return 0;
      }

      Integer v = this.allocations.get(a);
      return v == null ? 0 : Math.max(0, v);
   }

   public int spentPoints() {
      int s = 0;

      for (Entry<NpcAttribute, Integer> e : this.allocations.entrySet()) {
         if (e.getValue() != null) {
            s += Math.max(0, e.getValue());
         }
      }

      return s;
   }
}
