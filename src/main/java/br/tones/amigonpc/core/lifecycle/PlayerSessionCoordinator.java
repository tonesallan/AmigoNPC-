package br.tones.amigonpc.core.lifecycle;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.debug.ErrorDumpService;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerSessionCoordinator {
   private final Supplier<Object> eventRegistrySupplier;

   public PlayerSessionCoordinator(Supplier<Object> eventRegistrySupplier) {
      this.eventRegistrySupplier = eventRegistrySupplier;
   }

   public void register() {
      this.hookLogoutCleanupViaEventRegistry();
      this.hookLoginFailsafeViaEventRegistry();
   }

   private void hookLogoutCleanupViaEventRegistry() {
      try {
         Object registry = this.eventRegistrySupplier.get();
         if (registry == null) {
            return;
         }

         String[] candidates = new String[]{
            "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectedEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLeaveEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerQuitEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLogoutEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedOutEvent"
         };

         for (String cn : candidates) {
            this.tryRegister(registry, cn, true);
         }
      } catch (Throwable var7) {
      }
   }

   private void hookLoginFailsafeViaEventRegistry() {
      try {
         Object registry = this.eventRegistrySupplier.get();
         if (registry == null) {
            return;
         }

         String[] candidates = new String[]{
            "com.hypixel.hytale.server.core.event.events.player.PlayerJoinEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerConnectedEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLoginEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedInEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerSpawnedEvent"
         };

         for (String cn : candidates) {
            this.tryRegister(registry, cn, false);
         }
      } catch (Throwable var7) {
      }
   }

   private void tryRegister(Object registry, String eventClassName, boolean isLogout) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = PluginEventReflection.findRegisterMethod(registry);
         if (register == null) {
            return;
         }

         Consumer handler = evt -> {
            UUID uuid = PluginEventReflection.extractUuid(evt);
            if (uuid != null) {
               if (isLogout) {
                  try {
                     SimpleItemContainer bag = AmigoNpcManager.getShared().getOrLoadBackpack(uuid);
                     if (bag != null) {
                        AmigoPersistence.saveBackpack(uuid, bag);
                     }
                  } catch (Throwable var5x) {
                  }
               }

               AmigoNpcManager.getShared().despawnStored(uuid);
               if (isLogout) {
                  try {
                     ErrorDumpService.getShared().maybeDumpOnDisconnect(uuid, evt);
                  } catch (Throwable var4x) {
                  }
               }
            }
         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var7) {
      }
   }
}
