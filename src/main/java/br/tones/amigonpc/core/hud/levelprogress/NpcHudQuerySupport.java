package br.tones.amigonpc.core.hud.levelprogress;

import br.tones.amigonpc.core.zones.ZoneModel;
import com.hypixel.hytale.protocol.Color;

public final class NpcHudQuerySupport {
   private NpcHudQuerySupport() {
   }

   public static Color defaultColor() {
      return new Color((byte)-1, (byte)-1, (byte)-1);
   }

   public static Color resolveZoneHudColor(Color color) {
      return color != null ? color : defaultColor();
   }

   public static int resolveZoneForHud(int rawZoneId, int inferredZoneId, int npcLevel) {
      if (rawZoneId > 0) {
         return rawZoneId;
      } else {
         return inferredZoneId > 0 ? inferredZoneId : ZoneModel.expectedZoneForLevel(Math.max(1, npcLevel));
      }
   }

   public static String resolveZoneName(int zoneId) {
      return zoneId <= 0 ? "" : ZoneModel.zoneName(zoneId);
   }
}
