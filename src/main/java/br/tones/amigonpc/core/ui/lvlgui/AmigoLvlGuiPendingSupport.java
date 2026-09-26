package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.npcstats.NpcStatsState;
import java.util.Map;
import java.util.UUID;

final class AmigoLvlGuiPendingSupport {
   private AmigoLvlGuiPendingSupport() {
   }

   static boolean applyPendingValidated(
      UUID ownerId, Map<String, Integer> pendingChanges, String actionIncrement, String actionDecrement, String stat, String action, int amount
   ) {
      int cur = pendingChanges.getOrDefault(stat, 0);
      if (actionDecrement.equals(action)) {
         int next = Math.max(0, cur - amount);
         if (next <= 0) {
            pendingChanges.remove(stat);
         } else {
            pendingChanges.put(stat, next);
         }

         return next != cur;
      } else {
         if (!actionIncrement.equals(action)) {
            return false;
         }

         NpcAttribute attr = AmigoLvlGuiStatSupport.toAttr(stat);
         if (attr == null) {
            return false;
         }

         int npcLevel = AmigoNpcManager.getShared().getNpcLevel(ownerId);
         NpcStatsService svc = NpcStatsService.getShared();
         NpcStatsState st = svc.load(ownerId);
         int baseAvailable = svc.availablePoints(npcLevel, st);
         int pendingTotal = AmigoLvlGuiStatSupport.totalPending(pendingChanges);
         int available = Math.max(0, baseAvailable - pendingTotal);
         if (available <= 0) {
            return false;
         }

         int cap = svc.maxPointsFor(attr);
         int already = st.getAllocated(attr) + cur;
         int room = cap > 0 ? Math.max(0, cap - already) : Integer.MAX_VALUE;
         if (room <= 0) {
            return false;
         }

         int allow = Math.min(amount, Math.min(available, room));
         if (allow <= 0) {
            return false;
         }

         pendingChanges.put(stat, cur + allow);
         return true;
      }
   }
}
