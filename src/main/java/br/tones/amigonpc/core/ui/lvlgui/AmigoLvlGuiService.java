package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AmigoLvlGuiService {
   private static final AmigoLvlGuiService SHARED = new AmigoLvlGuiService();
   private final Map<UUID, AmigoLvlGuiPage> openPages = new ConcurrentHashMap<>();
   private final Map<UUID, Long> nextUpdateNanos = new ConcurrentHashMap<>();

   public static AmigoLvlGuiService getShared() {
      return SHARED;
   }

   private AmigoLvlGuiService() {
   }

   public boolean isOpen(UUID ownerId) {
      return this.openPages.containsKey(ownerId);
   }

   public void toggle(Player player, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, PlayerRef playerRef) {
      UUID ownerId = AmigoLvlGuiPageOpenSupport.ownerIdOrNull(playerRef);
      if (ownerId != null) {
         if (!AmigoLvlGuiPageOpenSupport.closeIfOpen(ownerId, this.openPages)) {
            AmigoLvlGuiPageOpenSupport.open(player, playerEntityRef, store, playerRef, this.openPages, this);
         }
      }
   }

   public void open(Player player, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, PlayerRef playerRef) {
      UUID ownerId = AmigoLvlGuiPageOpenSupport.ownerIdOrNull(playerRef);
      if (ownerId != null) {
         if (!AmigoLvlGuiPageOpenSupport.isAlreadyOpen(ownerId, this.openPages)) {
            AmigoLvlGuiPageOpenSupport.open(player, playerEntityRef, store, playerRef, this.openPages, this);
         }
      }
   }

   void markClosed(UUID ownerId) {
      if (ownerId != null) {
         this.openPages.remove(ownerId);
      }
   }

   public void notifyNpcXpChanged(UUID ownerId) {
      AmigoLvlGuiPageRefreshSupport.notifyNpcXpChanged(ownerId, this.openPages, this.nextUpdateNanos);
   }
}
