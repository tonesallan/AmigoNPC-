package br.tones.amigonpc.core.zones;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public final class AmigoZonesConfig {
   @SerializedName(value = "EnableZones", alternate = "enableZones")
   public boolean enableZones = true;
   @SerializedName(value = "EnableMobLevelVariance", alternate = "enableMobLevelVariance")
   public boolean enableMobLevelVariance = true;
   @SerializedName(value = "VarianceRange", alternate = "varianceRange")
   public int varianceRange = 5;
   @SerializedName(value = "CacheMinutes", alternate = "cacheMinutes")
   public int cacheMinutes = 5;
   @SerializedName(value = "DebugLog", alternate = "debugLog")
   public boolean debugLog = false;
   @SerializedName(value = "ZoneRanges", alternate = "zoneRanges")
   public List<AmigoZonesConfig.ZoneRange> zoneRanges = new ArrayList<>();

   public static AmigoZonesConfig defaults() {
      AmigoZonesConfig c = new AmigoZonesConfig();
      c.zoneRanges.add(new AmigoZonesConfig.ZoneRange(1, 1, 15));
      c.zoneRanges.add(new AmigoZonesConfig.ZoneRange(2, 15, 50));
      c.zoneRanges.add(new AmigoZonesConfig.ZoneRange(3, 50, 65));
      c.zoneRanges.add(new AmigoZonesConfig.ZoneRange(4, 65, 80));
      c.zoneRanges.add(new AmigoZonesConfig.ZoneRange(5, 80, 95));
      c.zoneRanges.add(new AmigoZonesConfig.ZoneRange(6, 95, 100));
      return c;
   }

   public record ZoneRange(
      @SerializedName(value = "ZoneId", alternate = "zoneId") int zoneId,
      @SerializedName(value = "LevelMin", alternate = "levelMin") int levelMin,
      @SerializedName(value = "LevelMax", alternate = "levelMax") int levelMax
   ) {
      public ZoneRange {
         if (zoneId < 1) {
            zoneId = 1;
         }

         if (levelMin < 1) {
            levelMin = 1;
         }

         if (levelMax < levelMin) {
            levelMax = levelMin;
         }

         if (levelMax > 100) {
            levelMax = 100;
         }
      }
   }
}
