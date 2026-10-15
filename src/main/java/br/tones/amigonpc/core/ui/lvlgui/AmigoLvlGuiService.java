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
   private final Map openPages = new ConcurrentHashMap();
   private final Map nextUpdateNanos = new ConcurrentHashMap();

   public static AmigoLvlGuiService getShared() {
      return SHARED;
   }

   private AmigoLvlGuiService() {
   }

   public boolean isOpen(UUID ownerId) {
      return this.openPages.containsKey(ownerId);
   }

   public void toggle(Player player, Ref playerEntityRef, Store store, PlayerRef playerRef) {
      if (player != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         if (ownerId != null) {
            AmigoLvlGuiPage existing = (AmigoLvlGuiPage)this.openPages.get(ownerId);
            if (existing != null) {
               try {
                  existing.requestClose();
               } catch (Throwable var9) {
               }

               this.openPages.remove(ownerId);
            } else {
               AmigoLvlGuiPage page = new AmigoLvlGuiPage(playerRef, this);
               this.openPages.put(ownerId, page);

               try {
                  player.getPageManager().openCustomPage(playerEntityRef, store, page);
               } catch (Throwable var10) {
                  this.openPages.remove(ownerId);
               }

            }
         }
      }
   }

   public void open(Player player, Ref playerEntityRef, Store store, PlayerRef playerRef) {
      if (player != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         if (ownerId != null) {
            AmigoLvlGuiPage existing = (AmigoLvlGuiPage)this.openPages.get(ownerId);
            if (existing == null) {
               AmigoLvlGuiPage page = new AmigoLvlGuiPage(playerRef, this);
               this.openPages.put(ownerId, page);

               try {
                  player.getPageManager().openCustomPage(playerEntityRef, store, page);
               } catch (Throwable var9) {
                  this.openPages.remove(ownerId);
               }

            }
         }
      }
   }

   void markClosed(UUID ownerId) {
      if (ownerId != null) {
         this.openPages.remove(ownerId);
      }

   }

   public void notifyNpcXpChanged(UUID ownerId) {
      if (ownerId != null) {
         AmigoLvlGuiPage p = (AmigoLvlGuiPage)this.openPages.get(ownerId);
         if (p != null) {
            long now = System.nanoTime();
            long next = (Long)this.nextUpdateNanos.getOrDefault(ownerId, 0L);
            if (now < next) {
               return;
            }

            this.nextUpdateNanos.put(ownerId, now + 250000000L);

            try {
               p.requestHeaderRefresh();
            } catch (Throwable var8) {
            }
         }

      }
   }
}
