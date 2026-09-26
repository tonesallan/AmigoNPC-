package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsPageActionSupport {
   private AmigoLvlGuiSettingsPageActionSupport() {
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
      AmigoLvlGuiSettingsPageActionResult playerRouteResult = AmigoLvlGuiSettingsPagePlayerRouteSupport.handle(action, pageState);
      if (playerRouteResult.handled) {
         return playerRouteResult;
      }

      AmigoLvlGuiSettingsPageActionResult adminRouteResult = AmigoLvlGuiSettingsPageAdminRouteSupport.handle(
         ownerId, viewerPlayerRef, store, data, action, isAdmin, pageState
      );
      return adminRouteResult.handled
         ? adminRouteResult
         : AmigoLvlGuiSettingsPageSecondaryRouteSupport.handle(ownerId, viewerPlayerRef, viewerRef, store, data, action, isAdmin, pageState, cmd);
   }
}
