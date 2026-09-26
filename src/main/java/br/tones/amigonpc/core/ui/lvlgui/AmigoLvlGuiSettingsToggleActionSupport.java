package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.CombatMode;
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
      } else if ("settings_combat_protect_owner".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsPlayerToggleSupport.selectCombatMode(settingsLive, settingsPending, CombatMode.PROTECT_OWNER);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else if ("settings_combat_weakest".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsPlayerToggleSupport.selectCombatMode(settingsLive, settingsPending, CombatMode.WEAKEST_ENEMY);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else if ("settings_toggle_auto_weapon".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsPlayerToggleSupport.toggleAutoWeaponSwitch(settingsLive, settingsPending);
         return new AmigoLvlGuiSettingsToggleActionResult(true, toggleResult.dirty, toggleResult.message);
      } else if ("settings_toggle_interrupt".equals(action)) {
         AmigoLvlGuiSettingsToggleResult toggleResult = AmigoLvlGuiSettingsPlayerToggleSupport.toggleInterruptAttacks(settingsLive, settingsPending);
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
