package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsPagePlayerRouteSupport {
   private AmigoLvlGuiSettingsPagePlayerRouteSupport() {
   }

   static AmigoLvlGuiSettingsPageActionResult handle(
      UUID ownerId, Store<EntityStore> store, String action, AmigoLvlGuiSettingsPageState pageState
   ) {
      AmigoLvlGuiSettingsToggleActionResult playerToggleResult = AmigoLvlGuiSettingsToggleActionSupport.handlePlayerAction(
         action, pageState.settingsLive, pageState.settingsPending
      );
      if (!playerToggleResult.handled) {
         return new AmigoLvlGuiSettingsPageActionResult(false, pageState, null, false);
      }

      // Player-facing companion settings are persisted immediately. The pending
      // snapshot remains only for admin controls that deliberately use Apply.
      AmigoLvlGuiSettingsFooterState saved = AmigoLvlGuiSettingsFooterSupport.apply(
         ownerId, store, pageState.settingsLive, pageState.settingsPending, false
      );
      AmigoLvlGuiSettingsPageState savedState = new AmigoLvlGuiSettingsPageState(
         saved.settingsLive,
         saved.settingsPending,
         false,
         pageState.settingsModelPrefillDone,
         pageState.settingsAdminPrefillDone
      );
      return new AmigoLvlGuiSettingsPageActionResult(
         true, savedState, AmigoText.text("ui.settings.feedback.saved_automatically"), false
      );
   }
}
