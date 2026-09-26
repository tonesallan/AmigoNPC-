package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsNameActionSupport {
   private AmigoLvlGuiSettingsNameActionSupport() {
   }

   static AmigoLvlGuiSettingsNameActionResult handle(UUID ownerId, Store<EntityStore> store, AmigoLvlGuiEventData data, UICommandBuilder cmd, String action) {
      if ("settings_name_apply".equals(action)) {
         AmigoLvlGuiSettingsModelPrefillState prefillState = AmigoLvlGuiSettingsNameSupport.applyName(ownerId, store, data, cmd);
         return new AmigoLvlGuiSettingsNameActionResult(true, prefillState.modelPrefillDone, prefillState.adminPrefillDone);
      } else if ("settings_name_off".equals(action)) {
         AmigoLvlGuiSettingsModelPrefillState prefillState = AmigoLvlGuiSettingsNameSupport.removeName(ownerId, store, cmd);
         return new AmigoLvlGuiSettingsNameActionResult(true, prefillState.modelPrefillDone, prefillState.adminPrefillDone);
      } else {
         return new AmigoLvlGuiSettingsNameActionResult(false, false, false);
      }
   }
}
