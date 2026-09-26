package br.tones.amigonpc.core.npcstats;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NpcStatsConfig {
   @SerializedName(value = "EnableNpcStats", alternate = "enableNpcStats")
   public boolean EnableNpcStats = true;
   @SerializedName(value = "StatPointsPerLevel", alternate = "statPointsPerLevel")
   public int StatPointsPerLevel = 5;
   @SerializedName(value = "MaxPointsPerStat", alternate = "maxPointsPerStat")
   public Map<NpcAttribute, Integer> MaxPointsPerStat = defaultMax();
   @SerializedName(value = "BlacklistedStats", alternate = "blacklistedStats")
   public List<String> BlacklistedStats = new ArrayList<>();
   @SerializedName(value = "HealthStatValuePerPoint", alternate = "healthStatValuePerPoint")
   public double HealthStatValuePerPoint = 1.0;
   @SerializedName(value = "DamageStatValuePerPoint", alternate = "damageStatValuePerPoint")
   public double DamageStatValuePerPoint = 1.5;
   @SerializedName(value = "CriticalDamageStatValuePerPoint", alternate = "criticalDamageStatValuePerPoint")
   public double CriticalDamageStatValuePerPoint = 0.2;
   @SerializedName(value = "DefenseStatValuePerPoint", alternate = "defenseStatValuePerPoint")
   public double DefenseStatValuePerPoint = 1.5;
   @SerializedName(value = "DefenseMaxReductionRatio", alternate = "defenseMaxReductionRatio")
   public double DefenseMaxReductionRatio = 0.8;
   @SerializedName(value = "DebugLogging", alternate = "debugLogging")
   public boolean DebugLogging = false;

   private static Map<NpcAttribute, Integer> defaultMax() {
      EnumMap<NpcAttribute, Integer> m = new EnumMap<>(NpcAttribute.class);
      m.put(NpcAttribute.HEALTH, 50);
      m.put(NpcAttribute.DAMAGE, 50);
      m.put(NpcAttribute.CRITICAL_DAMAGE, 50);
      m.put(NpcAttribute.DEFENSE, 50);
      return m;
   }
}
