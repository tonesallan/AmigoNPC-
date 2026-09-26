package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiStatsResetResult {
   final boolean error;
   final String message;

   AmigoLvlGuiStatsResetResult(boolean error, String message) {
      this.error = error;
      this.message = message;
   }
}
