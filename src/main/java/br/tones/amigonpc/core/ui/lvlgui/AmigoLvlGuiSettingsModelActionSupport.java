package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsModelActionSupport {
   private AmigoLvlGuiSettingsModelActionSupport() {
   }

   static AmigoLvlGuiSettingsModelActionResult handle(
      UUID ownerId,
      PlayerRef viewerPlayerRef,
      Ref<EntityStore> viewerRef,
      Store<EntityStore> store,
      AmigoLvlGuiEventData data,
      UICommandBuilder cmd,
      String action
   ) {
      if ("settings_model_apply".equals(action)) {
         AmigoLvlGuiSettingsModelPrefillState prefillState = AmigoLvlGuiSettingsModelSupport.applyModel(ownerId, viewerPlayerRef, viewerRef, store, data, cmd);
         return new AmigoLvlGuiSettingsModelActionResult(true, prefillState.modelPrefillDone, prefillState.adminPrefillDone);
      } else if ("settings_model_off".equals(action)) {
         AmigoLvlGuiSettingsModelPrefillState prefillState = AmigoLvlGuiSettingsModelSupport.removeModel(ownerId, viewerPlayerRef, viewerRef, store, cmd);
         return new AmigoLvlGuiSettingsModelActionResult(true, prefillState.modelPrefillDone, prefillState.adminPrefillDone);
      } else {
         return new AmigoLvlGuiSettingsModelActionResult(false, false, false);
      }
   }
}
