package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsFooterActionSupport {
   private AmigoLvlGuiSettingsFooterActionSupport() {
   }

   static AmigoLvlGuiSettingsFooterActionResult handle(
      UUID ownerId,
      String action,
      Store<EntityStore> store,
      AmigoLvlGuiSettingsSnapshot settingsLive,
      AmigoLvlGuiSettingsSnapshot settingsPending,
      boolean isAdmin
   ) {
      if ("settings_apply".equals(action)) {
         AmigoLvlGuiSettingsFooterState footerState = AmigoLvlGuiSettingsFooterSupport.apply(ownerId, store, settingsLive, settingsPending, isAdmin);
         return new AmigoLvlGuiSettingsFooterActionResult(
            true, footerState.settingsLive, footerState.settingsPending, footerState.settingsDirty, footerState.feedbackMessage, false
         );
      } else if ("settings_close".equals(action)) {
         AmigoLvlGuiSettingsFooterState footerState = AmigoLvlGuiSettingsFooterSupport.close(ownerId, settingsLive, isAdmin);
         return new AmigoLvlGuiSettingsFooterActionResult(
            true, footerState.settingsLive, footerState.settingsPending, footerState.settingsDirty, footerState.feedbackMessage, true
         );
      } else {
         return new AmigoLvlGuiSettingsFooterActionResult(false, settingsLive, settingsPending, false, null, false);
      }
   }
}
