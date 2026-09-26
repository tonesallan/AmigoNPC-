package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsAdminActionResult {
   final boolean handled;
   final String message;
   final boolean adminPrefillDone;

   AmigoLvlGuiSettingsAdminActionResult(boolean handled, String message, boolean adminPrefillDone) {
      this.handled = handled;
      this.message = message;
      this.adminPrefillDone = adminPrefillDone;
   }
}
