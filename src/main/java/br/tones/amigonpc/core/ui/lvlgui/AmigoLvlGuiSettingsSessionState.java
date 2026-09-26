package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsSessionState {
   final AmigoLvlGuiSettingsSnapshot settingsLive;
   final AmigoLvlGuiSettingsSnapshot settingsPending;
   final boolean settingsDirty;
   final boolean settingsModelPrefillDone;
   final boolean settingsAdminPrefillDone;

   AmigoLvlGuiSettingsSessionState(
      AmigoLvlGuiSettingsSnapshot settingsLive,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean settingsDirty,
      boolean settingsModelPrefillDone,
      boolean settingsAdminPrefillDone
   ) {
      this.settingsLive = settingsLive;
      this.settingsPending = settingsPending;
      this.settingsDirty = settingsDirty;
      this.settingsModelPrefillDone = settingsModelPrefillDone;
      this.settingsAdminPrefillDone = settingsAdminPrefillDone;
   }
}
