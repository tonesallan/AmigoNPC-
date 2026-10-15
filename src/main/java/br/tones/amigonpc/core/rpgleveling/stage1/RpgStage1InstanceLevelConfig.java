package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RpgStage1InstanceLevelConfig {
   private static final Map INSTANCES_BY_KEY;

   private RpgStage1InstanceLevelConfig() {
   }

   private static int[] calculateHPThresholds(int levelMin, int levelMax) {
      int floor = levelMin * 3;
      int ceil = levelMax * 3 + Math.max(0, (levelMax - levelMin) * 2);
      return new int[]{floor, ceil};
   }

   private static String normalizeKey(String s) {
      return s != null && !s.isBlank() ? s.trim().toLowerCase().replaceAll("[\\s_]+", "") : "";
   }

   public static String resolveInstanceId(World world) {
      if (world == null) {
         return null;
      } else {
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
   }

   public static InstanceData getInstanceById(String id) {
      if (id != null && !id.isBlank()) {
         String key = normalizeKey(id);
         InstanceData exact = (InstanceData)INSTANCES_BY_KEY.get(key);
         if (exact != null) {
            return exact;
         } else {
            InstanceData best = null;
            int bestLen = 0;

            for(InstanceData data : INSTANCES_BY_KEY.values()) {
               if (!data.exactIdMatch()) {
                  String dk = normalizeKey(data.id());
                  if (!dk.isEmpty() && key.contains(dk) && dk.length() > bestLen) {
                     best = data;
                     bestLen = dk.length();
                  }
               }
            }

            return best;
         }
      } else {
         return null;
      }
   }

   static {
      Map<String, InstanceData> m = new HashMap();
      String id = "Yungs_HyDungeons_Skeleton_Dungeon";
      int levelMin = 55;
      int levelMax = 75;
      int[] hp = calculateHPThresholds(levelMin, levelMax);
      InstanceData data = new InstanceData(id, hp[0], hp[1], levelMin, levelMax, false, false);
      m.put(normalizeKey(id), data);
      INSTANCES_BY_KEY = Collections.unmodifiableMap(m);
   }

   public static record InstanceData(String id, int hpFloor, int hpCeiling, int levelMin, int levelMax, boolean disableRPGLeveling, boolean exactIdMatch) {
   }
}
