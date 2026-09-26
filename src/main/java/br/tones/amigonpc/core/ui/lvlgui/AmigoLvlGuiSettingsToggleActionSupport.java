package br.tones.amigonpc.core.ui.lvlgui;

import java.util.UUID;

final class AmigoLvlGuiSettingsToggleActionSupport {
   private AmigoLvlGuiSettingsToggleActionSupport() {
   }

   static AmigoLvlGuiSettingsToggleActionResult handlePlayerAction(
      String action, AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending
   ) {
      if ("settings_toggle_autoloot".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsPlayerToggleSupport.toggleAutoLoot(settingsLive, settingsPending);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else if ("settings_toggle_defender".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsPlayerToggleSupport.toggleDefender(settingsLive, settingsPending);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else if ("settings_toggle_hud".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsPlayerToggleSupport.toggleHud(settingsLive, settingsPending);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else {
         return new AmigoLvlGuiSettingsToggleActionResult(false, false, null);
      }
   }

   static AmigoLvlGuiSettingsToggleActionResult handleAdminAction(
      UUID ownerId, String action, AmigoLvlGuiSettingsSnapshot settingsLive, AmigoLvlGuiSettingsSnapshot settingsPending
   ) {
      if ("settings_admin_toggle_godmode".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsAdminToggleSupport.toggleGodMode(ownerId, settingsLive, settingsPending);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else if ("settings_admin_toggle_pvp".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsAdminToggleSupport.togglePvp(settingsLive, settingsPending);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else {
         return new AmigoLvlGuiSettingsToggleActionResult(false, false, null);
      }
   }
}
