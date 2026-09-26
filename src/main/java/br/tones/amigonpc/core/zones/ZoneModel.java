package br.tones.amigonpc.core.zones;

import com.hypixel.hytale.protocol.Color;

public final class ZoneModel {
   public static final Color WHITE = new Color((byte)-1, (byte)-1, (byte)-1);
   public static final Color GREEN = new Color((byte)0, (byte)-1, (byte)0);
   public static final Color ORANGE = new Color((byte)-1, (byte)-91, (byte)0);
   public static final Color RED = new Color((byte)-1, (byte)0, (byte)0);
   public static final Color PURPLE = new Color((byte)-108, (byte)0, (byte)-45);
   private static final int SEVERITY_ORANGE = 5;
   private static final int SEVERITY_RED = 15;

   private ZoneModel() {
   }

   public static String zoneName(int zoneId) {
      return switch (zoneId) {
         case 1 -> "Bosque Esmeralda";
         case 2 -> "Areias Uivantes";
         case 3 -> "Borea";
         case 4 -> "Terras Devastadas";
         case 5 -> "Terras Celestes";
         case 6 -> "Terras Venenosas";
         default -> zoneId > 0 ? "Zona " + zoneId : "";
      };
   }

   public static int expectedZoneForLevel(int lvl) {
      int level = Math.max(1, lvl);
      if (level <= 15) {
         return 1;
      } else if (level <= 50) {
         return 2;
      } else if (level <= 65) {
         return 3;
      } else if (level <= 80) {
         return 4;
      } else {
         return level <= 95 ? 5 : 6;
      }
   }

   public static int inferZoneForMobLevel(int mobLevel) {
      int L = Math.max(1, Math.min(100, mobLevel));
      return expectedZoneForLevel(L);
   }

   public static Color colorForNpcVsZone(int npcLevel, AmigoZonesConfig.ZoneRange zr) {
      if (zr == null) {
         return WHITE;
      } else {
         int lvl = Math.max(1, npcLevel);
         int min = Math.max(1, zr.levelMin());
         int max = Math.max(min, zr.levelMax());
         if (lvl >= min && lvl <= max) {
            return GREEN;
         } else if (lvl > max) {
            return WHITE;
         } else {
            int severity = min - lvl;
            if (severity <= 5) {
               return ORANGE;
            } else {
               return severity <= 15 ? RED : PURPLE;
            }
         }
      }
   }

   public static AmigoZonesConfig.ZoneRange findZoneRange(int zoneId) {
      if (zoneId <= 0) {
         return null;
      }

      AmigoZonesConfig cfg = AmigoZonesConfigService.get();
      if (cfg != null && cfg.zoneRanges != null && !cfg.zoneRanges.isEmpty()) {
         for (AmigoZonesConfig.ZoneRange r : cfg.zoneRanges) {
            if (r != null && r.zoneId() == zoneId) {
               return r;
            }
         }
      }
      return switch (zoneId) {
         case 1 -> new AmigoZonesConfig.ZoneRange(1, 1, 15);
         case 2 -> new AmigoZonesConfig.ZoneRange(2, 15, 50);
         case 3 -> new AmigoZonesConfig.ZoneRange(3, 50, 65);
         case 4 -> new AmigoZonesConfig.ZoneRange(4, 65, 80);
         case 5 -> new AmigoZonesConfig.ZoneRange(5, 80, 95);
         case 6 -> new AmigoZonesConfig.ZoneRange(6, 95, 100);
         default -> null;
      };
   }

   public static Color colorForNpcLevel(int npcLevel, int zoneId) {
      return colorForNpcVsZone(npcLevel, findZoneRange(zoneId));
   }

   public static Integer currentZoneFromContext(int zoneId, int instanceLevelMin, int instanceLevelMax) {
      if (zoneId > 0) {
         return zoneId;
      } else if (instanceLevelMin > 0 && instanceLevelMax > 0) {
         int avg = Math.round(((float)instanceLevelMin + instanceLevelMax) / 2.0F);
         return expectedZoneForLevel(avg);
      } else {
         return null;
      }
   }
}
