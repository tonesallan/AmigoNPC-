package br.tones.amigonpc.core.hud.levelprogress;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

final class NativeHudBackend implements HudBackend {
   @Override
   public void show(Player player, PlayerRef playerRef, String hudId, CustomUIHud hud) {
      if (player != null && playerRef != null) {
         player.getHudManager().setCustomHud(playerRef, hud);
      }
   }

   @Override
   public void hide(Player player, PlayerRef playerRef, String hudId) {
      if (player != null && playerRef != null) {
         try {
            player.getHudManager().resetHud(playerRef);
         } catch (Throwable var5) {
         }
      }
   }

   @Override
   public boolean supportsIncrementalUpdates() {
      return false;
   }
}
