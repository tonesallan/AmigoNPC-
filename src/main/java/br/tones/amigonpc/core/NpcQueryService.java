package br.tones.amigonpc.core;

import br.tones.amigonpc.core.hud.levelprogress.NpcHudQuerySupport;
import br.tones.amigonpc.core.progress.NpcLevelProgressSnapshot;
import br.tones.amigonpc.core.progress.NpcProgressionSupport;
import com.hypixel.hytale.protocol.Color;
import java.util.Map;
import java.util.UUID;

final class NpcQueryService {
   private NpcQueryService() {
   }

   static NpcLevelProgressSnapshot getLevelProgressSnapshot(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId) {
      if (ownerId == null) {
         return NpcProgressionSupport.emptySnapshot();
      }

      try {
         AmigoNpcManager.NpcRecord rec = records.get(ownerId);
         long totalXp = rec != null
            ? NpcProgressionSupport.sanitizeTotalXp(rec.totalXp)
            : NpcProgressionSupport.sanitizeTotalXp(AmigoPersistence.loadTotalXp(ownerId));
         return NpcProgressionSupport.buildSnapshot(totalXp);
      } catch (Throwable var5) {
         return NpcProgressionSupport.emptySnapshot();
      }
   }

   static Color getZoneHudColor(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId) {
      if (ownerId == null) {
         return NpcHudQuerySupport.defaultColor();
      }

      AmigoNpcManager.NpcRecord rec = records.get(ownerId);
      return NpcHudQuerySupport.resolveZoneHudColor(rec != null ? rec.zoneHudColor : null);
   }

   static int getZoneForHud(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId) {
      if (ownerId == null) {
         return 0;
      }

      AmigoNpcManager.NpcRecord rec = records.get(ownerId);
      return rec == null ? 0 : NpcHudQuerySupport.resolveZoneForHud(rec.zoneRawId, rec.zoneInferredId, rec.npcLevelCached);
   }

   static String getZoneNameForHud(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId) {
      return NpcHudQuerySupport.resolveZoneName(getZoneForHud(records, ownerId));
   }

   static int getNpcLevel(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId) {
      if (ownerId == null) {
         return 1;
      }

      AmigoNpcManager.NpcRecord rec = records.get(ownerId);
      if (rec != null) {
         return Math.max(1, rec.npcLevelCached);
      }

      try {
         long totalXp = AmigoPersistence.loadTotalXp(ownerId);
         return NpcProgressionSupport.resolveLevel(totalXp);
      } catch (Throwable var5) {
         return 1;
      }
   }

   static long getNpcTotalXp(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId) {
      if (ownerId == null) {
         return 0L;
      }

      AmigoNpcManager.NpcRecord rec = records.get(ownerId);
      if (rec != null) {
         return NpcProgressionSupport.sanitizeTotalXp(rec.totalXp);
      }

      try {
         return NpcProgressionSupport.sanitizeTotalXp(AmigoPersistence.loadTotalXp(ownerId));
      } catch (Throwable var4) {
         return 0L;
      }
   }

   static boolean isInCombat(Map<UUID, AmigoNpcManager.NpcRecord> records, UUID ownerId, long nowMillis) {
      if (ownerId == null) {
         return false;
      }

      AmigoNpcManager.NpcRecord rec = records.get(ownerId);
      return rec == null ? false : NpcProgressionSupport.isRecentlyInCombat(rec.wasInCombat, rec.lastCombatTagMillis, nowMillis);
   }
}
