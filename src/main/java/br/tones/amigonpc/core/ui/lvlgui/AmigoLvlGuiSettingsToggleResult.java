package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsToggleResult {
   final boolean dirty;
   final String message;

   AmigoLvlGuiSettingsToggleResult(boolean dirty, String message) {
      this.dirty = dirty;
      this.message = message;
   }
}
