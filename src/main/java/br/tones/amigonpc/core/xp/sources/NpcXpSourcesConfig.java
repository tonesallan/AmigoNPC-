package br.tones.amigonpc.core.xp.sources;

public final class NpcXpSourcesConfig {
   public boolean enableCommandXP = true;
   public boolean enableCollectXP = true;
   public int collectThrottleMs = 500;
   public int miningBaseXP = 1;
   public int woodBaseXP = 1;
   public int pickupBaseXP = 1;
   public double levelFactor = 0.05;
   public boolean disableDuringCombat = true;
   public boolean debugLog = false;

   public static NpcXpSourcesConfig defaults() {
      return new NpcXpSourcesConfig();
   }
}
