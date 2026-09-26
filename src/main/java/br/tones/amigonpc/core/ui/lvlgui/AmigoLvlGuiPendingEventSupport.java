package br.tones.amigonpc.core.ui.lvlgui;

import java.util.Map;
import java.util.UUID;

final class AmigoLvlGuiPendingEventSupport {
   private AmigoLvlGuiPendingEventSupport() {
   }

   static AmigoLvlGuiPendingActionResult handlePendingStatAction(
      UUID ownerId, Map<String, Integer> pendingChanges, AmigoLvlGuiEventData data, String action, String actionIncrement, String actionDecrement
   ) {
      String stat = AmigoLvlGuiEventDataReadSupport.statNameOrEmpty(data);
      int amount = AmigoLvlGuiEventDataReadSupport.amountOrDefault(data, 0);
      if (stat == null || stat.isEmpty() || amount <= 0) {
         return new AmigoLvlGuiPendingActionResult(false, false);
      }

      if (!AmigoLvlGuiStatSupport.isEditableStat(stat)) {
         return new AmigoLvlGuiPendingActionResult(false, false);
      }

      boolean changed = AmigoLvlGuiPendingSupport.applyPendingValidated(ownerId, pendingChanges, actionIncrement, actionDecrement, stat, action, amount);
      return new AmigoLvlGuiPendingActionResult(true, changed);
   }
}
