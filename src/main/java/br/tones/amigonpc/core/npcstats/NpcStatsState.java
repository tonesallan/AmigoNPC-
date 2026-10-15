package br.tones.amigonpc.core.npcstats;

import java.util.EnumMap;
import java.util.Map;

public final class NpcStatsState {
   public final Map allocations = new EnumMap(NpcAttribute.class);

   public int getAllocated(NpcAttribute a) {
      if (a == null) {
         return 0;
      } else {
         Integer v = (Integer)this.allocations.get(a);
         return v == null ? 0 : Math.max(0, v);
      }
   }

   public int spentPoints() {
      int s = 0;

      for(Map.Entry e : this.allocations.entrySet()) {
         if (e.getValue() != null) {
            s += Math.max(0, (Integer)e.getValue());
         }
      }

      return s;
   }
}
