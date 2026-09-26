package br.tones.amigonpc.core.lifecycle;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.debug.ErrorDumpService;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlayerSessionCoordinator {
   private final Supplier<EventRegistry> eventRegistrySupplier;

   public PlayerSessionCoordinator(Supplier<EventRegistry> eventRegistrySupplier) {
      this.eventRegistrySupplier = eventRegistrySupplier;
   }

   public void register() {
      EventRegistry registry = this.eventRegistrySupplier.get();
      if (registry == null) {
         return;
      }

      registry.register(PlayerDisconnectEvent.class, this::onDisconnect);
      registry.register(PlayerConnectEvent.class, this::onConnect);
   }

   private void onDisconnect(PlayerDisconnectEvent event) {
      if (event == null) {
         return;
      }

      PlayerRef playerRef = event.getPlayerRef();
      UUID ownerId = playerRef != null ? playerRef.getUuid() : null;
      if (ownerId == null) {
         return;
      }

      try {
         SimpleItemContainer bag = AmigoNpcManager.getShared().getOrLoadBackpack(ownerId);
         if (bag != null) {
            AmigoPersistence.saveBackpack(ownerId, bag);
         }
      } catch (Throwable ignored) {
      }

      AmigoNpcManager.getShared().despawnStored(ownerId);

      try {
         ErrorDumpService.getShared().maybeDumpOnDisconnect(ownerId, event);
      } catch (Throwable ignored) {
      }
   }

   private void onConnect(PlayerConnectEvent event) {
      if (event == null || event.getPlayerRef() == null) {
         return;
      }

      UUID ownerId = event.getPlayerRef().getUuid();
      if (ownerId != null && AmigoNpcManager.getShared().hasNpc(ownerId)) {
         AmigoNpcManager.getShared().despawnStored(ownerId);
      }
   }
}
