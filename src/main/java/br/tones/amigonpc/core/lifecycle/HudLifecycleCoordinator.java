package br.tones.amigonpc.core.lifecycle;

import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksService;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.function.Supplier;

public final class HudLifecycleCoordinator {
   private final Supplier<EventRegistry> eventRegistrySupplier;

   public HudLifecycleCoordinator(Supplier<EventRegistry> eventRegistrySupplier) {
      this.eventRegistrySupplier = eventRegistrySupplier;
   }

   public void register() {
      EventRegistry registry = this.eventRegistrySupplier.get();
      if (registry == null) {
         return;
      }

      registry.register(PlayerConnectEvent.class, this::onPlayerConnect);
      registry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
   }

   private void onPlayerConnect(PlayerConnectEvent event) {
      if (event == null) {
         return;
      }

      PlayerRef playerRef = event.getPlayerRef();
      if (playerRef == null) {
         return;
      }

      UUID ownerId = playerRef.getUuid();
      if (ownerId == null) {
         return;
      }

      try {
         ActionTraceService.getShared().record(ownerId, "session", "connect");
      } catch (Throwable ignored) {
      }

      Player player = event.getPlayer();
      LevelProgressHudService.getShared().onPlayerJoin(player, playerRef, ownerId);

      try {
         if (player != null) {
            PlayerStatTweaksService.getShared().onPlayerJoin(player, playerRef, ownerId);
         }
      } catch (Throwable ignored) {
      }
   }

   private void onPlayerDisconnect(PlayerDisconnectEvent event) {
      if (event == null) {
         return;
      }

      PlayerRef playerRef = event.getPlayerRef();
      if (playerRef == null) {
         return;
      }

      LevelProgressHudService.getShared().onPlayerLeave(null, playerRef);

      try {
         PlayerStatTweaksService.getShared().onPlayerLeave(null, playerRef);
      } catch (Throwable ignored) {
      }
   }
}
