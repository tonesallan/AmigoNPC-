package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class RpgStage1MobLevelCacheSupport {
   private RpgStage1MobLevelCacheSupport() {
   }

   static Integer tryGetCachedLevel(
      Map<UUID, RpgStage1MobLevelCacheSupport.CacheEntry> levelCache, UUID uuid, float maxHp, Integer zoneId, String instanceId, long cacheExpiryMillis
   ) {
      if (levelCache != null && uuid != null) {
         RpgStage1MobLevelCacheSupport.CacheEntry e = levelCache.get(uuid);
         if (e != null && e.matches(maxHp, zoneId, instanceId)) {
            long age = System.currentTimeMillis() - e.timestamp;
            return age >= cacheExpiryMillis ? null : e.level;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   static void putLevel(Map<UUID, RpgStage1MobLevelCacheSupport.CacheEntry> levelCache, UUID uuid, int level, float maxHp, Integer zoneId, String instanceId) {
      if (levelCache != null && uuid != null) {
         levelCache.put(uuid, new RpgStage1MobLevelCacheSupport.CacheEntry(level, maxHp, zoneId, instanceId));
      }
   }

   static final class CacheEntry {
      final int level;
      final float maxHp;
      final Integer zoneId;
      final String instanceId;
      final long timestamp;

      CacheEntry(int level, float maxHp, Integer zoneId, String instanceId) {
         this.level = level;
         this.maxHp = maxHp;
         this.zoneId = zoneId;
         this.instanceId = instanceId;
         this.timestamp = System.currentTimeMillis();
      }

      boolean matches(float maxHp, Integer zoneId, String instanceId) {
         if (Math.abs(maxHp - this.maxHp) >= 0.1F) {
            return false;
         }

         boolean zoneOk = this.zoneId != null ? this.zoneId.equals(zoneId) : zoneId == null;
         boolean instOk = this.instanceId != null ? Objects.equals(this.instanceId, instanceId) : instanceId == null;
         return zoneOk && instOk;
      }
   }
}
