package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.Map;
import java.util.Set;

public final class RpgStage1ZoneLevelConfig {
   private static final Map<Integer, RpgStage1ZoneLevelConfig.ZoneData> ZONES_BY_ID = RpgStage1ZoneSupport.createDefaultZones();

   private RpgStage1ZoneLevelConfig() {
   }

   public static RpgStage1ZoneLevelConfig.ZoneData getZoneById(int id) {
      return ZONES_BY_ID.get(id);
   }

   public static Integer remapZoneIdForBiome(String biomeId, Integer zoneId) {
      return RpgStage1ZoneSupport.remapZoneIdForBiome(ZONES_BY_ID, biomeId, zoneId);
   }

   public record ZoneData(int id, int hpFloor, int hpCeiling, int levelMin, int levelMax, Set<String> missingBiomeIds) {
   }
}
