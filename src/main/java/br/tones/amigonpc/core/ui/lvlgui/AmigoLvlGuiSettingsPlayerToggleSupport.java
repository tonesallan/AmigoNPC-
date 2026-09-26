package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;

final class AmigoLvlGuiSettingsPlayerToggleSupport {
   private AmigoLvlGuiSettingsPlayerToggleSupport() {
   }

   static AmigoLvlGuiSettingsToggleResult toggleAutoLoot(AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending) {
      settingsPending.autoLoot = !settingsPending.autoLoot;
      return new AmigoLvlGuiSettingsToggleResult(
         AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending),
         AmigoText.format("ui.settings.feedback.autoloot_pending", AmigoText.onOff(settingsPending.autoLoot))
      );
   }

   static AmigoLvlGuiSettingsToggleResult toggleDefender(AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending) {
      settingsPending.defender = !settingsPending.defender;
      return new AmigoLvlGuiSettingsToggleResult(
         AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending),
         AmigoText.format("ui.settings.feedback.defender_pending", AmigoText.onOff(settingsPending.defender))
      );
   }

   static AmigoLvlGuiSettingsToggleResult toggleHud(AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending) {
      settingsPending.hud = !settingsPending.hud;
      return new AmigoLvlGuiSettingsToggleResult(
         AmigoLvlGuiSettingsStateSupport.isDirty(settingsLive, settingsPending),
         AmigoText.format("ui.settings.feedback.hud_pending", AmigoText.onOff(settingsPending.hud))
      );
   }
}
