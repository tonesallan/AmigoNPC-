package br.tones.amigonpc.core.lifecycle;

import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksService;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class HudLifecycleCoordinator {
   private final Supplier<Object> eventRegistrySupplier;

   public HudLifecycleCoordinator(Supplier<Object> eventRegistrySupplier) {
      this.eventRegistrySupplier = eventRegistrySupplier;
   }

   public void register() {
      try {
         Object registry = this.eventRegistrySupplier.get();
         if (registry == null) {
            return;
         }

         String[] joinCandidates = new String[]{
            "com.hypixel.hytale.server.core.event.events.player.PlayerJoinEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerConnectedEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLoginEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedInEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerSpawnedEvent"
         };
         String[] leaveCandidates = new String[]{
            "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectedEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLeaveEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerQuitEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLogoutEvent",
            "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedOutEvent"
         };

         for (String cn : joinCandidates) {
            this.tryRegisterHudJoin(registry, cn);
         }

         for (String cn : leaveCandidates) {
            this.tryRegisterHudLeave(registry, cn);
         }
      } catch (Throwable var8) {
      }
   }

   private void tryRegisterHudJoin(Object registry, String eventClassName) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = PluginEventReflection.findRegisterMethod(registry);
         if (register == null) {
            return;
         }

         Consumer handler = evt -> {
            try {
               UUID uuid = PluginEventReflection.extractUuid(evt);
               if (uuid == null) {
                  return;
               }

               try {
                  ActionTraceService.getShared().record(uuid, "session", "join " + evt.getClass().getSimpleName());
               } catch (Throwable var6x) {
               }

               PlayerRef pref = PluginEventReflection.extractPlayerRef(evt);
               Player player = PluginEventReflection.extractPlayer(evt);
               if (pref != null) {
                  LevelProgressHudService.getShared().onPlayerJoin(player, pref, uuid);
               }

               try {
                  if (player != null) {
                     PlayerStatTweaksService.getShared().onPlayerJoin(player, pref, uuid);
                  }
               } catch (Throwable var5x) {
               }
            } catch (Throwable var7) {
            }
         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var6) {
      }
   }

   private void tryRegisterHudLeave(Object registry, String eventClassName) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = PluginEventReflection.findRegisterMethod(registry);
         if (register == null) {
            return;
         }

         Consumer handler = evt -> {
            try {
               PlayerRef pref = PluginEventReflection.extractPlayerRef(evt);
               Player player = PluginEventReflection.extractPlayer(evt);
               if (pref != null) {
                  LevelProgressHudService.getShared().onPlayerLeave(player, pref);
               }

               try {
                  if (player != null) {
                     PlayerStatTweaksService.getShared().onPlayerLeave(player, pref);
                  }
               } catch (Throwable var4x) {
               }
            } catch (Throwable var5x) {
            }
         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var6) {
      }
   }
}
