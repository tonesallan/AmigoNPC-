package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class RpgStage1ZoneSupport {
   private RpgStage1ZoneSupport() {
   }

   static Map<Integer, RpgStage1ZoneLevelConfig.ZoneData> createDefaultZones() {
      Map<Integer, RpgStage1ZoneLevelConfig.ZoneData> m = new HashMap<>();
      m.put(1, makeZone(1, 1, 15));
      m.put(2, makeZone(2, 15, 50));
      m.put(3, makeZone(3, 50, 75));
      m.put(4, makeZone(4, 75, 100));
      m.put(5, makeZone(5, 85, 100));
      m.put(6, makeZone(6, 90, 100));
      return Collections.unmodifiableMap(m);
   }

   static Integer remapZoneIdForBiome(Map<Integer, RpgStage1ZoneLevelConfig.ZoneData> zonesById, String biomeId, Integer zoneId) {
      if (zonesById != null && biomeId != null && !biomeId.isBlank()) {
         String trimmed = biomeId.trim();

         for (RpgStage1ZoneLevelConfig.ZoneData z : zonesById.values()) {
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

   private static RpgStage1ZoneLevelConfig.ZoneData makeZone(int id, int levelMin, int levelMax) {
      int[] hp = calculateHPThresholds(levelMin, levelMax, id);
      return new RpgStage1ZoneLevelConfig.ZoneData(id, hp[0], hp[1], levelMin, levelMax, Collections.emptySet());
   }

   private static int[] calculateHPThresholds(int levelMin, int levelMax, int zoneId) {
      return switch (zoneId) {
         case 1 -> new int[]{15, 80};
         case 2 -> new int[]{60, 110};
         case 3 -> new int[]{80, 160};
         case 4 -> new int[]{120, 300};
         case 5 -> new int[]{100, 110};
         case 6 -> new int[]{110, 120};
         default -> new int[]{levelMin * 3, levelMax * 3 + (levelMax - levelMin) * 2};
      };
   }
}
