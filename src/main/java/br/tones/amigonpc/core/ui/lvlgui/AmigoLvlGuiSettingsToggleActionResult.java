package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsToggleActionResult {
   final boolean handled;
   final boolean dirty;
   final String message;

   AmigoLvlGuiSettingsToggleActionResult(boolean handled, boolean dirty, String message) {
      this.handled = handled;
      this.dirty = dirty;
      this.message = message;
   }
}
