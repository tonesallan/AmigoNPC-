package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiMainActionResult {
   final boolean handled;
   final boolean updateStats;
   final boolean updateRewards;
   final boolean clearError;
   final String errorMessage;

   AmigoLvlGuiMainActionResult(boolean handled, boolean updateStats, boolean updateRewards, boolean clearError, String errorMessage) {
      this.handled = handled;
      this.updateStats = updateStats;
      this.updateRewards = updateRewards;
      this.clearError = clearError;
      this.errorMessage = errorMessage;
   }
}
