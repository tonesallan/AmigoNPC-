package br.tones.amigonpc.core.ui.lvlgui;

final class AmigoLvlGuiSettingsPagePlayerRouteSupport {
   private AmigoLvlGuiSettingsPagePlayerRouteSupport() {
   }

   static AmigoLvlGuiSettingsPageActionResult handle(String action, AmigoLvlGuiSettingsPageState pageState) {
      AmigoLvlGuiSettingsToggleActionResult playerToggleResult = AmigoLvlGuiSettingsToggleActionSupport.handlePlayerAction(
         action, pageState.settingsLive, pageState.settingsPending
      );
      return !playerToggleResult.handled
         ? new AmigoLvlGuiSettingsPageActionResult(false, pageState, null, false)
         : new AmigoLvlGuiSettingsPageActionResult(
            true, AmigoLvlGuiSettingsPageStateSupport.withDirty(pageState, playerToggleResult.dirty), playerToggleResult.message, false
         );
   }
}
