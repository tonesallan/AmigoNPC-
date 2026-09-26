package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsNameActionResult {
   final boolean handled;
   final boolean modelPrefillDone;
   final boolean adminPrefillDone;

   AmigoLvlGuiSettingsNameActionResult(boolean handled, boolean modelPrefillDone, boolean adminPrefillDone) {
      this.handled = handled;
      this.modelPrefillDone = modelPrefillDone;
      this.adminPrefillDone = adminPrefillDone;
   }
}
