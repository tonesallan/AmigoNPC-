package br.tones.amigonpc.core.loot;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class LootService {
   private static final LootService SHARED = new LootService();

   public static LootService getShared() {
      return SHARED;
   }

   private LootService() {
   }

   public boolean openLoot(UUID ownerId, PlayerRef viewerPlayerRef, Ref<EntityStore> viewerEntityRef, Store<EntityStore> store) {
      if (ownerId != null && viewerPlayerRef != null && viewerEntityRef != null && store != null) {
         AmigoNpcManager manager = AmigoNpcManager.getShared();
         if (!manager.hasNpc(ownerId)) {
            return false;
         }

         SimpleItemContainer bag = manager.getOrLoadBackpack(ownerId);

         World world;
         try {
            world = ((EntityStore)store.getExternalData()).getWorld();
         } catch (Throwable t) {
            return false;
         }

         if (world == null) {
            return false;
         }

         world.execute(() -> {
            try {
               Player playerComponent = (Player)store.getComponent(viewerEntityRef, Player.getComponentType());
               if (playerComponent == null) {
                  return;
               }

               ContainerWindow window = new ContainerWindow(bag);
               playerComponent.getPageManager().setPageWithWindows(viewerEntityRef, store, Page.Bench, true, new Window[]{window});
            } catch (Throwable var5x) {
            }
         });
         return true;
      } else {
         return false;
      }
   }
}
