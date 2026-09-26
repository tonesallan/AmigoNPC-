package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;

final class AmigoLvlGuiPageOpenSupport {
   private AmigoLvlGuiPageOpenSupport() {
   }

   static UUID ownerIdOrNull(PlayerRef playerRef) {
      if (playerRef == null) {
         return null;
      }

      try {
         return playerRef.getUuid();
      } catch (Throwable ignored) {
         return null;
      }
   }

   static boolean closeIfOpen(UUID ownerId, Map<UUID, AmigoLvlGuiPage> openPages) {
      if (ownerId == null) {
         return false;
      }

      AmigoLvlGuiPage existing = openPages.get(ownerId);
      if (existing == null) {
         return false;
      }

      try {
         existing.requestClose();
      } catch (Throwable var4) {
      }

      openPages.remove(ownerId);
      return true;
   }

   static boolean isAlreadyOpen(UUID ownerId, Map<UUID, AmigoLvlGuiPage> openPages) {
      return ownerId != null && openPages.get(ownerId) != null;
   }

   static void open(
      Player player,
      Ref<EntityStore> playerEntityRef,
      Store<EntityStore> store,
      PlayerRef playerRef,
      Map<UUID, AmigoLvlGuiPage> openPages,
      AmigoLvlGuiService ownerService
   ) {
      UUID ownerId = ownerIdOrNull(playerRef);
      if (player != null && ownerId != null) {
         AmigoLvlGuiPage page = new AmigoLvlGuiPage(playerRef, ownerService);
         openPages.put(ownerId, page);

         try {
            player.getPageManager().openCustomPage(playerEntityRef, store, page);
         } catch (Throwable ignored) {
            openPages.remove(ownerId);
         }
      }
   }
}
