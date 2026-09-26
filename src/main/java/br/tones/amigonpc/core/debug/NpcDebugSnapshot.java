package br.tones.amigonpc.core.debug;

public final class NpcDebugSnapshot {
   public String ownerUuid;
   public boolean hasRecord;
   public boolean hasNpcRef;
   public String state;
   public boolean downed;
   public long downedUntilMillis;
   public long deathDespawnAtMillis;
   public int npcLevel;
   public long totalXp;
   public double xpRemainder;
   public boolean inCombatRecently;
   public long lastCombatTagMillis;
   public long lastCombatEndMillis;
   public boolean lootingActive;
   public long lootStickUntilMillis;
   public boolean regenActive;
   public long regenStartAtMillis;
   public long regenLastApplyMillis;
   public boolean lootPausedInventoryFull;
   public DebugVec3 lastOwnerPos;
   public DebugVec3 lastNpcPos;
   public DebugVec3 lastBattleCenter;
   public int backpackSlotsUsed;
   public int backpackSlotsTotal;
}
