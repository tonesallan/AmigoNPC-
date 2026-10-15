package br.tones.amigonpc.core.zones;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobLevelVarianceCalculator {
   private final Map cache = new ConcurrentHashMap();
   private volatile long cacheTtlMillis;

   public MobLevelVarianceCalculator() {
      this.reloadConfig();
   }

   public void reloadConfig() {
      AmigoZonesConfig cfg = AmigoZonesConfigService.get();
      this.cacheTtlMillis = (long)cfg.cacheMinutes * 60000L;
   }

   public Result compute(UUID mobUuid, float maxHealth, int zoneId, int instanceLevelMin, int instanceLevelMax, String instanceId) {
      long now = System.currentTimeMillis();
      AmigoZonesConfig cfg = AmigoZonesConfigService.get();
      int variance = cfg.enableMobLevelVariance ? Math.max(0, cfg.varianceRange) : 0;
      if (mobUuid == null) {
         mobUuid = UUID.nameUUIDFromBytes(("AmigoNPC:nulluuid:" + now).getBytes());
         variance = 0;
      }

      CacheEntry e = (CacheEntry)this.cache.get(mobUuid);
      if (e != null && now - e.atMillis < this.cacheTtlMillis && e.matches(maxHealth, zoneId, instanceId)) {
         return new Result(e.level, 0, 0, e.level, true);
      } else {
         Base base = this.computeBaseLevel(maxHealth, zoneId, instanceLevelMin, instanceLevelMax, cfg);
         int offset = 0;
         if (variance > 0) {
            long seed = mobUuid.getMostSignificantBits() ^ mobUuid.getLeastSignificantBits();
            Random r = new Random(seed);
            int span = variance * 2 + 1;
            offset = r.nextInt(span) - variance;
         }

         int lvl = clamp(1, base.maxLevelCap, base.baseLevel + offset);
         this.cache.put(mobUuid, new CacheEntry(lvl, maxHealth, zoneId, instanceId, now));
         return new Result(base.baseLevel, offset, base.maxLevelCap, lvl, false);
      }
   }

   private Base computeBaseLevel(float maxHealth, int zoneId, int instanceLevelMin, int instanceLevelMax, AmigoZonesConfig cfg) {
      if (instanceLevelMin > 0 && instanceLevelMax > 0) {
         int lvl = linearHpToLevel(maxHealth, instanceLevelMin, instanceLevelMax, (float)instanceLevelMin * 3.0F, (float)instanceLevelMax * 3.0F);
         return new Base(lvl, instanceLevelMax);
      } else {
         if (zoneId > 0) {
            AmigoZonesConfig.ZoneRange zr = findZoneRange(cfg, zoneId);
            if (zr != null) {
               int lvl = linearHpToLevel(maxHealth, zr.levelMin(), zr.levelMax(), (float)zr.levelMin() * 3.0F, (float)zr.levelMax() * 3.0F);
               return new Base(lvl, zr.levelMax());
            }
         }

         int base = Math.round(maxHealth / 3.0F);
         base = clamp(1, 100, base);
         return new Base(base, 100);
      }
   }

   private static AmigoZonesConfig.ZoneRange findZoneRange(AmigoZonesConfig cfg, int zoneId) {
      if (cfg != null && cfg.zoneRanges != null) {
         for(AmigoZonesConfig.ZoneRange r : cfg.zoneRanges) {
            if (r != null && r.zoneId() == zoneId) {
               return r;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static int linearHpToLevel(float maxHealth, int levelMin, int levelMax, float hpFloor, float hpCeiling) {
      float hpRange = hpCeiling - hpFloor;
      float lvlRange = (float)(levelMax - levelMin);
      int base;
      if (!(hpRange > 0.0F)) {
         base = levelMin;
      } else {
         float raw = (float)levelMin + (maxHealth - hpFloor) / hpRange * lvlRange;
         base = Math.round(raw);
      }

      return clamp(levelMin, levelMax, base);
   }

   private static int clamp(int min, int max, int v) {
      if (v < min) {
         return min;
      } else {
         return v > max ? max : v;
      }
   }

   private static final class CacheEntry {
      final int level;
      final float maxHealth;
      final int zoneId;
      final String instanceId;
      final long atMillis;

      CacheEntry(int level, float maxHealth, int zoneId, String instanceId, long atMillis) {
         this.level = level;
         this.maxHealth = maxHealth;
         this.zoneId = zoneId;
         this.instanceId = instanceId;
         this.atMillis = atMillis;
      }

      boolean matches(float maxHealth, int zoneId, String instanceId) {
         if (Float.compare(this.maxHealth, maxHealth) != 0) {
            return false;
         } else if (this.zoneId != zoneId) {
            return false;
         } else if (this.instanceId == null) {
            return instanceId == null;
         } else {
            return this.instanceId.equals(instanceId);
         }
      }
   }

   private static final class Base {
      final int baseLevel;
      final int maxLevelCap;

      Base(int baseLevel, int maxLevelCap) {
         this.baseLevel = baseLevel;
         this.maxLevelCap = maxLevelCap;
      }
   }

   public static record Result(int baseLevel, int offset, int maxLevelCap, int finalLevel, boolean cacheHit) {
   }
}
