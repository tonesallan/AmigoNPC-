package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgStage1MobLevelCalculator {
   private static final long CACHE_EXPIRY_MS = 300000L;
   private final Map<UUID, RpgStage1MobLevelCacheSupport.CacheEntry> levelCache = new ConcurrentHashMap<>();
   private final RpgLevelingStage1Config config;

   public RpgStage1MobLevelCalculator(RpgLevelingStage1Config config) {
      this.config = config;
   }

   public int computeLevel(UUID uuid, float maxHp, Integer zoneId, String biomeId, String instanceId) {
      Integer remappedZone = RpgStage1MobLevelComputeSupport.remapZone(zoneId, biomeId);
      Integer cachedLevel = RpgStage1MobLevelCacheSupport.tryGetCachedLevel(this.levelCache, uuid, maxHp, remappedZone, instanceId, 300000L);
      if (cachedLevel != null) {
         return cachedLevel;
      }

      int level = this.computeLevelInternal(maxHp, remappedZone, instanceId, uuid);
      RpgStage1MobLevelCacheSupport.putLevel(this.levelCache, uuid, level, maxHp, remappedZone, instanceId);
      return level;
   }

   public int computeLevel(UUID uuid, float maxHp, Integer zoneId, String instanceId) {
      return this.computeLevel(uuid, maxHp, zoneId, null, instanceId);
   }

   private int computeLevelInternal(float maxHp, Integer zoneId, String instanceId, UUID uuid) {
      RpgStage1MobLevelComputeSupport.LevelBand levelBand = RpgStage1MobLevelComputeSupport.resolveLevelBand(maxHp, zoneId, instanceId);
      return RpgStage1MobLevelComputeSupport.applyDeterministicVariation(levelBand.baseLevel, levelBand.maxLevel, uuid);
   }
}
