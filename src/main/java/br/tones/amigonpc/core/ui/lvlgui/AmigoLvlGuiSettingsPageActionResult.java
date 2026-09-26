package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsPageActionResult {
   final boolean handled;
   final AmigoLvlGuiSettingsPageState pageState;
   final String feedbackMessage;
   final boolean shouldClose;

   AmigoLvlGuiSettingsPageActionResult(boolean handled, AmigoLvlGuiSettingsPageState pageState, String feedbackMessage, boolean shouldClose) {
      this.handled = handled;
      this.pageState = pageState;
      this.feedbackMessage = feedbackMessage;
      this.shouldClose = shouldClose;
   }
}
