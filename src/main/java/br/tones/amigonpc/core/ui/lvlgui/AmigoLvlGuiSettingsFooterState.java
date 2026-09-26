package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsFooterState {
   final AmigoLvlGuiSettingsSnapshot settingsLive;
   final AmigoLvlGuiSettingsSnapshot settingsPending;
   final boolean settingsDirty;
   final String feedbackMessage;

   AmigoLvlGuiSettingsFooterState(
      AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending, boolean settingsDirty, String feedbackMessage
   ) {
      this.settingsLive = settingsLive;
      this.settingsPending = settingsPending;
      this.settingsDirty = settingsDirty;
      this.feedbackMessage = feedbackMessage;
   }
}
