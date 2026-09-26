package br.tones.amigonpc.core.lifecycle;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.debug.ActionTraceService;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.function.Supplier;

public final class TeleportRespawnCoordinator {
   private final Supplier<EventRegistry> eventRegistrySupplier;

   public TeleportRespawnCoordinator(Supplier<EventRegistry> eventRegistrySupplier) {
      this.eventRegistrySupplier = eventRegistrySupplier;
   }

   public void register() {
      EventRegistry registry = this.eventRegistrySupplier.get();
      if (registry != null) {
         registry.registerGlobal(AddPlayerToWorldEvent.class, this::onAddedToWorld);
      }
   }

   private void onAddedToWorld(AddPlayerToWorldEvent event) {
      if (event == null) {
         return;
      }

      Holder<EntityStore> holder = event.getHolder();
      World world = event.getWorld();
      if (holder == null || world == null) {
         return;
      }

      PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
      if (playerRef == null) {
         return;
      }

      UUID ownerId = playerRef.getUuid();
      if (ownerId == null || !AmigoNpcManager.getShared().hasNpc(ownerId)) {
         return;
      }

      try {
         ActionTraceService.getShared().record(ownerId, "session", "added_to_world");
      } catch (Throwable ignored) {
      }

      AmigoNpcManager.getShared().requestRespawn(world, ownerId, playerRef);
   }
}
