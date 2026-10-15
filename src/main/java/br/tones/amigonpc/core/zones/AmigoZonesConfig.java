package br.tones.amigonpc.core.zones;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public final class AmigoZonesConfig {
   @SerializedName(
      value = "EnableZones",
      alternate = {"enableZones"}
   )
   public boolean enableZones = true;
   @SerializedName(
      value = "EnableMobLevelVariance",
      alternate = {"enableMobLevelVariance"}
   )
   public boolean enableMobLevelVariance = true;
   @SerializedName(
      value = "VarianceRange",
      alternate = {"varianceRange"}
   )
   public int varianceRange = 5;
   @SerializedName(
      value = "CacheMinutes",
      alternate = {"cacheMinutes"}
   )
   public int cacheMinutes = 5;
   @SerializedName(
      value = "DebugLog",
      alternate = {"debugLog"}
   )
   public boolean debugLog = false;
   @SerializedName(
      value = "ZoneRanges",
      alternate = {"zoneRanges"}
   )
   public List zoneRanges = new ArrayList();

   public static AmigoZonesConfig defaults() {
      AmigoZonesConfig c = new AmigoZonesConfig();
      c.zoneRanges.add(new ZoneRange(1, 1, 15));
      c.zoneRanges.add(new ZoneRange(2, 15, 50));
      c.zoneRanges.add(new ZoneRange(3, 50, 65));
      c.zoneRanges.add(new ZoneRange(4, 65, 80));
      c.zoneRanges.add(new ZoneRange(5, 80, 95));
      c.zoneRanges.add(new ZoneRange(6, 95, 100));
      return c;
   }

   public static record ZoneRange(int zoneId, int levelMin, int levelMax) {
      public ZoneRange(int zoneId, int levelMin, int levelMax) {
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

         this.zoneId = zoneId;
         this.levelMin = levelMin;
         this.levelMax = levelMax;
      }

      @SerializedName(
         value = "ZoneId",
         alternate = {"zoneId"}
      )
      public int zoneId() {
         return this.zoneId;
      }

      @SerializedName(
         value = "LevelMin",
         alternate = {"levelMin"}
      )
      public int levelMin() {
         return this.levelMin;
      }

      @SerializedName(
         value = "LevelMax",
         alternate = {"levelMax"}
      )
      public int levelMax() {
         return this.levelMax;
      }
   }
}
