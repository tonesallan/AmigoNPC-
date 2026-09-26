package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsFooterActionResult {
   final boolean handled;
   final AmigoLvlGuiSettingsSnapshot settingsLive;
   final AmigoLvlGuiSettingsSnapshot settingsPending;
   final boolean settingsDirty;
   final String feedbackMessage;
   final boolean shouldClose;

   AmigoLvlGuiSettingsFooterActionResult(
      boolean handled,
      AmigoLvlGuiSettingsSnapshot settingsLive,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean settingsDirty,
      String feedbackMessage,
      boolean shouldClose
   ) {
      this.handled = handled;
      this.settingsLive = settingsLive;
      this.settingsPending = settingsPending;
      this.settingsDirty = settingsDirty;
      this.feedbackMessage = feedbackMessage;
      this.shouldClose = shouldClose;
   }
}
