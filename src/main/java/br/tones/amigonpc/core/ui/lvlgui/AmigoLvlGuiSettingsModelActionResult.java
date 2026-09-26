package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsModelActionResult {
   final boolean handled;
   final boolean modelPrefillDone;
   final boolean adminPrefillDone;

   AmigoLvlGuiSettingsModelActionResult(boolean handled, boolean modelPrefillDone, boolean adminPrefillDone) {
      this.handled = handled;
      this.modelPrefillDone = modelPrefillDone;
      this.adminPrefillDone = adminPrefillDone;
   }
}
