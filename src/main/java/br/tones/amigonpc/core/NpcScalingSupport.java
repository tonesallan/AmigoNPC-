package br.tones.amigonpc.core;

import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import java.lang.reflect.Method;

final class NpcScalingSupport {
   private static volatile Integer DEF_STAT_INDEX = null;

   private NpcScalingSupport() {
   }

   static int resolveDefenseStatIndex() {
      Integer cached = DEF_STAT_INDEX;
      if (cached != null) {
         return cached;
      }

      synchronized (NpcScalingSupport.class) {
         cached = DEF_STAT_INDEX;
         if (cached != null) {
            return cached;
         }

         int idx = -1;
         String[] candidates = new String[]{
            "getDefense",
            "getDefence",
            "getArmor",
            "getArmour",
            "getPhysicalDefense",
            "getPhysicalDefence",
            "getProtection",
            "getResistance",
            "getPhysicalResistance"
         };

         for (String name : candidates) {
            try {
               Method m = DefaultEntityStatTypes.class.getMethod(name);
               Object out = m.invoke(null);
               if (out instanceof Integer) {
                  idx = (Integer)out;
                  break;
               }
            } catch (Throwable var11) {
            }
         }

         DEF_STAT_INDEX = idx;
         return idx;
      }
   }
}
