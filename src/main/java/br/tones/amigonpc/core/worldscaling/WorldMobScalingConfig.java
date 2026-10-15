package br.tones.amigonpc.core.worldscaling;

import java.util.ArrayList;
import java.util.List;

public final class WorldMobScalingConfig {
   public boolean enableWorldMobScaling = false;
   public List whitelistWorlds = new ArrayList();
   public List whitelistInstances = new ArrayList();
   public double hpPerLevelPercent = (double)0.0F;
   public double hpAddPerLevel = (double)0.0F;
   public double damagePerLevelPercent = (double)0.0F;
   public double damageAddPerLevel = (double)0.0F;
   public long cacheTtlMs = 10000L;
   public boolean debugLog = false;

   public static WorldMobScalingConfig defaults() {
      return new WorldMobScalingConfig();
   }
}
