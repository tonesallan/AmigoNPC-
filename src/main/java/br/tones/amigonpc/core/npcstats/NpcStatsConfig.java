package br.tones.amigonpc.core.npcstats;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NpcStatsConfig {
   @SerializedName(
      value = "EnableNpcStats",
      alternate = {"enableNpcStats"}
   )
   public boolean EnableNpcStats = true;
   @SerializedName(
      value = "StatPointsPerLevel",
      alternate = {"statPointsPerLevel"}
   )
   public int StatPointsPerLevel = 5;
   @SerializedName(
      value = "MaxPointsPerStat",
      alternate = {"maxPointsPerStat"}
   )
   public Map MaxPointsPerStat = defaultMax();
   @SerializedName(
      value = "BlacklistedStats",
      alternate = {"blacklistedStats"}
   )
   public List BlacklistedStats = new ArrayList();
   @SerializedName(
      value = "HealthStatValuePerPoint",
      alternate = {"healthStatValuePerPoint"}
   )
   public double HealthStatValuePerPoint = (double)1.0F;
   @SerializedName(
      value = "DamageStatValuePerPoint",
      alternate = {"damageStatValuePerPoint"}
   )
   public double DamageStatValuePerPoint = (double)1.5F;
   @SerializedName(
      value = "CriticalDamageStatValuePerPoint",
      alternate = {"criticalDamageStatValuePerPoint"}
   )
   public double CriticalDamageStatValuePerPoint = 0.2;
   @SerializedName(
      value = "DefenseStatValuePerPoint",
      alternate = {"defenseStatValuePerPoint"}
   )
   public double DefenseStatValuePerPoint = (double)1.5F;
   @SerializedName(
      value = "DefenseMaxReductionRatio",
      alternate = {"defenseMaxReductionRatio"}
   )
   public double DefenseMaxReductionRatio = 0.8;
   @SerializedName(
      value = "DebugLogging",
      alternate = {"debugLogging"}
   )
   public boolean DebugLogging = false;

   private static Map defaultMax() {
      EnumMap<NpcAttribute, Integer> m = new EnumMap(NpcAttribute.class);
      m.put(NpcAttribute.HEALTH, 50);
      m.put(NpcAttribute.DAMAGE, 50);
      m.put(NpcAttribute.CRITICAL_DAMAGE, 50);
      m.put(NpcAttribute.DEFENSE, 50);
      return m;
   }
}
