package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.progress.NpcLevelProgressSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcLevelDataView {
   public final int level;
   public final long xp;
   public final long xpNeeded;
   public final int availablePoints;
   public final Map<String, Integer> allocatedPoints;
   public final Map<String, String> statValueText;

   private NpcLevelDataView(int level, long xp, long xpNeeded, int availablePoints, Map<String, Integer> allocatedPoints, Map<String, String> statValueText) {
      this.level = Math.max(1, level);
      this.xp = Math.max(0L, xp);
      this.xpNeeded = Math.max(0L, xpNeeded);
      this.availablePoints = Math.max(0, availablePoints);
      this.allocatedPoints = allocatedPoints;
      this.statValueText = statValueText;
   }

   public static NpcLevelDataView fromOwner(UUID ownerId) {
      NpcLevelProgressSnapshot snap = AmigoNpcManager.getShared().getLevelProgressSnapshot(ownerId);
      long scaledHp = NpcLevelDataViewStatsSupport.loadScaledHp(ownerId, snap.level());
      Map<String, Integer> alloc = new HashMap<>();
      Map<String, String> values = new HashMap<>();
      int available = NpcLevelDataViewStatsSupport.populateConfiguredStats(ownerId, snap.level(), scaledHp, alloc, values);
      NpcLevelDataViewStatsSupport.ensureFallbackStats(alloc, values, scaledHp);
      return new NpcLevelDataView(snap.level(), snap.xp(), snap.xpNeeded(), available, alloc, values);
   }

   public float progress01() {
      if (this.xpNeeded <= 0L) {
         return 0.0F;
      }

      double p = (double)this.xp / this.xpNeeded;
      if (p < 0.0) {
         p = 0.0;
      }

      if (p > 1.0) {
         p = 1.0;
      }

      return (float)p;
   }
}
