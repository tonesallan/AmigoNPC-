package br.tones.amigonpc.core.lifecycle;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.debug.ActionTraceService;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TeleportRespawnCoordinator {
   private final Supplier<Object> eventRegistrySupplier;

   public TeleportRespawnCoordinator(Supplier<Object> eventRegistrySupplier) {
      this.eventRegistrySupplier = eventRegistrySupplier;
   }

   public void register() {
      try {
         Object registry = this.eventRegistrySupplier.get();
         if (registry == null) {
            return;
         }

         String[] candidates = new String[]{
            "com.hypixel.hytale.server.core.event.events.player.PlayerTeleportEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerTeleportedEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerChangedWorldEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerWorldChangeEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerDimensionChangeEvent"
         };

         for (String cn : candidates) {
            this.tryRegisterTeleport(registry, cn);
         }
      } catch (Throwable var7) {
      }
   }

   private void tryRegisterTeleport(Object registry, String eventClassName) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = PluginEventReflection.findRegisterMethod(registry);
         if (register == null) {
            return;
         }

         Consumer handler = evt -> {
            UUID uuid = PluginEventReflection.extractUuid(evt);
            if (uuid != null) {
               Object playerRef = PluginEventReflection.invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
               Object worldObj = this.extractWorld(evt, playerRef);
               if (worldObj != null) {
                  try {
                     ActionTraceService.getShared().record(uuid, "session", "teleport/world-change " + evt.getClass().getSimpleName());
                  } catch (Throwable var6x) {
                  }

                  AmigoNpcManager.getShared().requestRespawn(worldObj, uuid, playerRef != null ? playerRef : evt);
               }
            }
         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var6) {
      }
   }

   private Object extractWorld(Object evt, Object playerRef) {
      Object w = PluginEventReflection.invokeNoArg(evt, "getWorld", "getToWorld", "getDestinationWorld", "getNewWorld", "getUniverseWorld");
      if (w != null) {
         return w;
      }

      if (playerRef != null) {
         Object w2 = PluginEventReflection.invokeNoArg(playerRef, "getWorld", "getUniverseWorld", "world");
         if (w2 != null) {
            return w2;
         }
      }

      return null;
   }
}
