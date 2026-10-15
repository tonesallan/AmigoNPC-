package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class RpgStage1ZoneLevelConfig {
   private static final Map ZONES_BY_ID;

   private RpgStage1ZoneLevelConfig() {
   }

   private static ZoneData makeZone(int id, int levelMin, int levelMax) {
      int[] hp = calculateHPThresholds(levelMin, levelMax, id);
      return new ZoneData(id, hp[0], hp[1], levelMin, levelMax, Collections.emptySet());
   }

   private static int[] calculateHPThresholds(int levelMin, int levelMax, int zoneId) {
      int[] var10000;
      switch (zoneId) {
         case 1 -> var10000 = new int[]{15, 80};
         case 2 -> var10000 = new int[]{60, 110};
         case 3 -> var10000 = new int[]{80, 160};
         case 4 -> var10000 = new int[]{120, 300};
         case 5 -> var10000 = new int[]{100, 110};
         case 6 -> var10000 = new int[]{110, 120};
         default -> var10000 = new int[]{levelMin * 3, levelMax * 3 + (levelMax - levelMin) * 2};
      }

      return var10000;
   }

   public static ZoneData getZoneById(int id) {
      return (ZoneData)ZONES_BY_ID.get(id);
   }

   public static Integer remapZoneIdForBiome(String biomeId, Integer zoneId) {
      if (biomeId != null && !biomeId.isBlank()) {
         String trimmed = biomeId.trim();

         for(ZoneData z : ZONES_BY_ID.values()) {
            Set<String> missing = z.missingBiomeIds();
            if (missing != null && missing.contains(trimmed)) {
               return z.id();
            }
         }

         return zoneId;
      } else {
         return zoneId;
      }
   }

   static {
      Map<Integer, ZoneData> m = new HashMap();
      m.put(1, makeZone(1, 1, 15));
      m.put(2, makeZone(2, 15, 50));
      m.put(3, makeZone(3, 50, 75));
      m.put(4, makeZone(4, 75, 100));
      m.put(5, makeZone(5, 85, 100));
      m.put(6, makeZone(6, 90, 100));
      ZONES_BY_ID = Collections.unmodifiableMap(m);
   }

   public static record ZoneData(int id, int hpFloor, int hpCeiling, int levelMin, int levelMax, Set missingBiomeIds) {
   }
}
