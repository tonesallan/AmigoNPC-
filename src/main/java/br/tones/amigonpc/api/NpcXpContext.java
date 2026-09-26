package br.tones.amigonpc.api;

public record NpcXpContext(String worldName, String instanceId, String biomeName, int zoneId, String mobId, int mobLevel, long timestampMs) {
   public static NpcXpContext now() {
      return new NpcXpContext(null, null, null, 0, null, 0, System.currentTimeMillis());
   }
}
