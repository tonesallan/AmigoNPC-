package br.tones.amigonpc.core.rpgleveling.stage1;

import java.util.Random;
import java.util.UUID;

final class RpgStage1MobLevelComputeSupport {
   private RpgStage1MobLevelComputeSupport() {
   }

   static Integer remapZone(Integer zoneId, String biomeId) {
      return biomeId != null && !biomeId.isBlank() ? RpgStage1ZoneLevelConfig.remapZoneIdForBiome(biomeId, zoneId) : zoneId;
   }

   static RpgStage1MobLevelComputeSupport.LevelBand resolveLevelBand(float maxHp, Integer zoneId, String instanceId) {
      RpgStage1InstanceLevelConfig.InstanceData inst = instanceId != null && !instanceId.isBlank()
         ? RpgStage1InstanceLevelConfig.getInstanceById(instanceId)
         : null;
      if (inst != null) {
         return new RpgStage1MobLevelComputeSupport.LevelBand(
            computeClampedLevel(maxHp, inst.hpFloor(), inst.hpCeiling(), inst.levelMin(), inst.levelMax()), inst.levelMax()
         );
      }

      RpgStage1ZoneLevelConfig.ZoneData zone = zoneId != null ? RpgStage1ZoneLevelConfig.getZoneById(zoneId) : null;
      if (zone != null) {
         return new RpgStage1MobLevelComputeSupport.LevelBand(
            computeClampedLevel(maxHp, zone.hpFloor(), zone.hpCeiling(), zone.levelMin(), zone.levelMax()), zone.levelMax()
         );
      }

      int fallbackLevel = Math.round(maxHp / 3.0F);
      fallbackLevel = Math.max(1, Math.min(100, fallbackLevel));
      return new RpgStage1MobLevelComputeSupport.LevelBand(fallbackLevel, 100);
   }

   static int applyDeterministicVariation(int baseLevel, int maxLevel, UUID uuid) {
      Random r = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
      int variation = r.nextInt(11) - 5;
      int out = baseLevel + variation;
      return Math.max(1, Math.min(maxLevel, out));
   }

   private static int computeClampedLevel(float maxHp, int hpFloor, int hpCeiling, int levelMin, int levelMax) {
      float hpRange = hpCeiling - hpFloor;
      float lvlRange = levelMax - levelMin;
      int baseLevel;
      if (hpRange <= 0.0F) {
         baseLevel = levelMin;
      } else {
         float raw = levelMin + (maxHp - hpFloor) / hpRange * lvlRange;
         baseLevel = Math.round(raw);
      }

      return Math.max(levelMin, Math.min(levelMax, baseLevel));
   }

   static final class LevelBand {
      final int baseLevel;
      final int maxLevel;

      LevelBand(int baseLevel, int maxLevel) {
         this.baseLevel = baseLevel;
         this.maxLevel = maxLevel;
      }
   }
}
