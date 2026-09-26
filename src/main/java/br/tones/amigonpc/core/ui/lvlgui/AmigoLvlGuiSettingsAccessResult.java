package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsAccessResult {
   final boolean granted;
   final String feedbackMessage;

   AmigoLvlGuiSettingsAccessResult(boolean granted, String feedbackMessage) {
      this.granted = granted;
      this.feedbackMessage = feedbackMessage;
   }
}
