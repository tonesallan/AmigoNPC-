package br.tones.amigonpc.core.worldscaling;

import java.util.ArrayList;
import java.util.List;

public final class WorldMobScalingConfig {
   public boolean enableWorldMobScaling = false;
   public List<String> whitelistWorlds = new ArrayList<>();
   public List<String> whitelistInstances = new ArrayList<>();
   public double hpPerLevelPercent = 0.0;
   public double hpAddPerLevel = 0.0;
   public double damagePerLevelPercent = 0.0;
   public double damageAddPerLevel = 0.0;
   public long cacheTtlMs = 10000L;
   public boolean debugLog = false;

   public static WorldMobScalingConfig defaults() {
      return new WorldMobScalingConfig();
   }
}
