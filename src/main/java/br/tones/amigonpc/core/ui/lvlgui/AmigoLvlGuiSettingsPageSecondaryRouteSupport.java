package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsPageSecondaryRouteSupport {
   private AmigoLvlGuiSettingsPageSecondaryRouteSupport() {
   }

   static AmigoLvlGuiSettingsPageActionResult handle(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      Ref<EntityStore> viewerRef,
      Store<EntityStore> store,
      AmigoLvlGuiEventData data,
      String action,
      boolean isAdmin,
      AmigoLvlGuiSettingsPageState pageState,
      UICommandBuilder cmd
   ) {
      AmigoLvlGuiSettingsFooterActionResult footerActionResult = AmigoLvlGuiSettingsFooterActionSupport.handle(
         ownerId, action, store, pageState.settingsLive, pageState.settingsPending, isAdmin
      );
      if (footerActionResult.handled) {
         return new AmigoLvlGuiSettingsPageActionResult(
            true,
            AmigoLvlGuiSettingsPageStateSupport.withFooterActionResult(pageState, footerActionResult),
            footerActionResult.feedbackMessage,
            footerActionResult.shouldClose
         );
      }

      AmigoLvlGuiSettingsImmediateActionResult immediateActionResult = AmigoLvlGuiSettingsImmediateActionSupport.handle(
         ownerId, viewerPlayerRef, viewerRef, store, action
      );
      if (immediateActionResult.handled) {
         return new AmigoLvlGuiSettingsPageActionResult(true, pageState, immediateActionResult.message, false);
      }

      AmigoLvlGuiSettingsImmediateActionResult languageActionResult = AmigoLvlGuiSettingsLanguageSupport.handle(ownerId, data, cmd, action);
      if (languageActionResult.handled) {
         return new AmigoLvlGuiSettingsPageActionResult(true, pageState, null, false);
      }

      AmigoLvlGuiSettingsNameActionResult nameActionResult = AmigoLvlGuiSettingsNameActionSupport.handle(ownerId, store, data, cmd, action);
      if (nameActionResult.handled) {
         return new AmigoLvlGuiSettingsPageActionResult(
            true,
            AmigoLvlGuiSettingsPageStateSupport.withModelAndAdminPrefillDone(pageState, nameActionResult.modelPrefillDone, nameActionResult.adminPrefillDone),
            null,
            false
         );
      }

      AmigoLvlGuiSettingsModelActionResult modelActionResult = AmigoLvlGuiSettingsModelActionSupport.handle(
         ownerId, viewerPlayerRef, viewerRef, store, data, cmd, action
      );
      return modelActionResult.handled
         ? new AmigoLvlGuiSettingsPageActionResult(
            true,
            AmigoLvlGuiSettingsPageStateSupport.withModelAndAdminPrefillDone(pageState, modelActionResult.modelPrefillDone, modelActionResult.adminPrefillDone),
            null,
            false
         )
         : new AmigoLvlGuiSettingsPageActionResult(false, pageState, null, false);
   }
}
