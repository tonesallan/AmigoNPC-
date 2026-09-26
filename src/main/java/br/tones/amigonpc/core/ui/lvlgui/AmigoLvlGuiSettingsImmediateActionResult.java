package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsImmediateActionResult {
   final boolean handled;
   final String message;

   AmigoLvlGuiSettingsImmediateActionResult(boolean handled, String message) {
      this.handled = handled;
      this.message = message;
   }
}
