package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Map;

public final class RpgStage1InstanceLevelConfig {
   private static final Map<String, RpgStage1InstanceLevelConfig.InstanceData> INSTANCES_BY_KEY = RpgStage1InstanceSupport.createDefaultInstances();

   private RpgStage1InstanceLevelConfig() {
   }

   public static String resolveInstanceId(World world) {
      return RpgStage1InstanceSupport.resolveInstanceId(world);
   }

   public static RpgStage1InstanceLevelConfig.InstanceData getInstanceById(String id) {
      return RpgStage1InstanceSupport.findById(INSTANCES_BY_KEY, id);
   }

   public record InstanceData(String id, int hpFloor, int hpCeiling, int levelMin, int levelMax, boolean disableRPGLeveling, boolean exactIdMatch) {
   }
}
