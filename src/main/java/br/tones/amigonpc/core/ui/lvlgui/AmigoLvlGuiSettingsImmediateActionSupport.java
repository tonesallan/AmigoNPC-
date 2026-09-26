package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

final class AmigoLvlGuiSettingsImmediateActionSupport {
   private AmigoLvlGuiSettingsImmediateActionSupport() {
   }

   static AmigoLvlGuiSettingsImmediateActionResult handle(
      UUID ownerId, PlayerRef viewerPlayerRef, Ref<EntityStore> viewerRef, Store<EntityStore> store, String action
   ) {
      if ("settings_spawn".equals(action)) {
         return new AmigoLvlGuiSettingsImmediateActionResult(true, AmigoLvlGuiSettingsImmediateSupport.spawnNpc(ownerId, viewerPlayerRef, viewerRef, store));
      } else if ("settings_despawn".equals(action)) {
         return new AmigoLvlGuiSettingsImmediateActionResult(true, AmigoLvlGuiSettingsImmediateSupport.despawnNpc(ownerId, store));
      } else {
         return "settings_loot".equals(action)
            ? new AmigoLvlGuiSettingsImmediateActionResult(true, AmigoLvlGuiSettingsImmediateSupport.openLoot(ownerId, viewerPlayerRef, viewerRef, store))
            : new AmigoLvlGuiSettingsImmediateActionResult(false, null);
      }
   }
}
