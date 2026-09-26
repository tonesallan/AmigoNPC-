package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class RpgStage1InstanceSupport {
   private RpgStage1InstanceSupport() {
   }

   static Map<String, RpgStage1InstanceLevelConfig.InstanceData> createDefaultInstances() {
      Map<String, RpgStage1InstanceLevelConfig.InstanceData> m = new HashMap<>();
      String id = "Yungs_HyDungeons_Skeleton_Dungeon";
      int levelMin = 55;
      int levelMax = 75;
      int[] hp = calculateHPThresholds(levelMin, levelMax);
      RpgStage1InstanceLevelConfig.InstanceData data = new RpgStage1InstanceLevelConfig.InstanceData(id, hp[0], hp[1], levelMin, levelMax, false, false);
      m.put(normalizeKey(id), data);
      return Collections.unmodifiableMap(m);
   }

   static String resolveInstanceId(World world) {
      if (world == null) {
         return null;
      }

      try {
         String name = world.getName();
         if (name != null && !name.isBlank()) {
            return name.trim();
         }

         String disp = world.getWorldConfig().getDisplayName();
         if (disp != null && !disp.isBlank()) {
            return disp.trim();
         }
      } catch (Exception var3) {
      }

      return null;
   }

   static RpgStage1InstanceLevelConfig.InstanceData findById(Map<String, RpgStage1InstanceLevelConfig.InstanceData> instancesByKey, String id) {
      if (instancesByKey != null && id != null && !id.isBlank()) {
         String key = normalizeKey(id);
         RpgStage1InstanceLevelConfig.InstanceData exact = instancesByKey.get(key);
         if (exact != null) {
            return exact;
         }

         RpgStage1InstanceLevelConfig.InstanceData best = null;
         int bestLen = 0;

         for (RpgStage1InstanceLevelConfig.InstanceData data : instancesByKey.values()) {
            if (!data.exactIdMatch()) {
               String dk = normalizeKey(data.id());
               if (!dk.isEmpty() && key.contains(dk) && dk.length() > bestLen) {
                  best = data;
                  bestLen = dk.length();
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private static int[] calculateHPThresholds(int levelMin, int levelMax) {
      int floor = levelMin * 3;
      int ceil = levelMax * 3 + Math.max(0, (levelMax - levelMin) * 2);
      return new int[]{floor, ceil};
   }

   private static String normalizeKey(String s) {
      return s != null && !s.isBlank() ? s.trim().toLowerCase().replaceAll("[\\s_]+", "") : "";
   }
}
