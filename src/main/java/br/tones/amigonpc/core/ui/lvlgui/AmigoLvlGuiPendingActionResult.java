package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiPendingActionResult {
   final boolean accepted;
   final boolean changed;

   AmigoLvlGuiPendingActionResult(boolean accepted, boolean changed) {
      this.accepted = accepted;
      this.changed = changed;
   }
}
