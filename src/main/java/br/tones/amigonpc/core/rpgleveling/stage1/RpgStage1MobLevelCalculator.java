package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgStage1MobLevelCalculator {
   private static final long CACHE_EXPIRY_MS = 300000L;
   private final Map levelCache = new ConcurrentHashMap();
   private final RpgLevelingStage1Config config;

   public RpgStage1MobLevelCalculator(RpgLevelingStage1Config config) {
      this.config = config;
   }

   public int computeLevel(UUID uuid, float maxHp, Integer zoneId, String biomeId, String instanceId) {
      Integer remappedZone = zoneId;
      if (biomeId != null && !biomeId.isBlank()) {
         remappedZone = RpgStage1ZoneLevelConfig.remapZoneIdForBiome(biomeId, zoneId);
      }

      CacheEntry e = (CacheEntry)this.levelCache.get(uuid);
      if (e != null && e.matches(maxHp, remappedZone, instanceId)) {
         long age = System.currentTimeMillis() - e.timestamp;
         if (age < 300000L) {
            return e.level;
         }
      }

      int level = this.computeLevelInternal(maxHp, remappedZone, instanceId, uuid);
      this.levelCache.put(uuid, new CacheEntry(level, maxHp, remappedZone, instanceId));
      return level;
   }

   public int computeLevel(UUID uuid, float maxHp, Integer zoneId, String instanceId) {
      return this.computeLevel(uuid, maxHp, zoneId, (String)null, instanceId);
   }

   private int computeLevelInternal(float maxHp, Integer zoneId, String instanceId, UUID uuid) {
      RpgStage1InstanceLevelConfig.InstanceData inst = instanceId != null && !instanceId.isBlank() ? RpgStage1InstanceLevelConfig.getInstanceById(instanceId) : null;
      RpgStage1ZoneLevelConfig.ZoneData zone = zoneId != null ? RpgStage1ZoneLevelConfig.getZoneById(zoneId) : null;
      int maxLevel;
      int baseLevel;
      if (inst != null) {
         float hpRange = (float)(inst.hpCeiling() - inst.hpFloor());
         float lvlRange = (float)(inst.levelMax() - inst.levelMin());
         if (hpRange <= 0.0F) {
            baseLevel = inst.levelMin();
         } else {
            float raw = (float)inst.levelMin() + (maxHp - (float)inst.hpFloor()) / hpRange * lvlRange;
            baseLevel = Math.round(raw);
         }

         baseLevel = Math.max(inst.levelMin(), Math.min(inst.levelMax(), baseLevel));
         maxLevel = inst.levelMax();
      } else if (zone != null) {
         float hpRange = (float)(zone.hpCeiling() - zone.hpFloor());
         float lvlRange = (float)(zone.levelMax() - zone.levelMin());
         if (hpRange <= 0.0F) {
            baseLevel = zone.levelMin();
         } else {
            float raw = (float)zone.levelMin() + (maxHp - (float)zone.hpFloor()) / hpRange * lvlRange;
            baseLevel = Math.round(raw);
         }

         baseLevel = Math.max(zone.levelMin(), Math.min(zone.levelMax(), baseLevel));
         maxLevel = zone.levelMax();
      } else {
         baseLevel = Math.round(maxHp / 3.0F);
         baseLevel = Math.max(1, Math.min(100, baseLevel));
         maxLevel = 100;
      }

      Random r = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
      int variation = r.nextInt(11) - 5;
      int out = baseLevel + variation;
      out = Math.max(1, Math.min(maxLevel, out));
      return out;
   }

   private static final class CacheEntry {
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
         } else {
            boolean zoneOk = this.zoneId != null ? this.zoneId.equals(zoneId) : zoneId == null;
            boolean instOk = this.instanceId != null ? Objects.equals(this.instanceId, instanceId) : instanceId == null;
            return zoneOk && instOk;
         }
      }
   }
}
