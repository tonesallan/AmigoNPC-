package br.tones.amigonpc.core.hud.levelprogress;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.lang.reflect.Method;

final class MultipleHudBackend implements HudBackend {
   private final Object mhudInstance;
   private final Method setCustomHudMethod;
   private final Method hideCustomHudMethod;

   MultipleHudBackend() throws Exception {
      Class<?> mhudClass = Class.forName("com.buuz135.mhud.MultipleHUD");
      Method getInstance = mhudClass.getMethod("getInstance");
      this.mhudInstance = getInstance.invoke(null);
      this.setCustomHudMethod = mhudClass.getMethod("setCustomHud", Player.class, PlayerRef.class, String.class, CustomUIHud.class);
      this.hideCustomHudMethod = mhudClass.getMethod("hideCustomHud", Player.class, PlayerRef.class, String.class);
   }

   @Override
   public void show(Player player, PlayerRef playerRef, String hudId, CustomUIHud hud) {
      try {
         this.setCustomHudMethod.invoke(this.mhudInstance, player, playerRef, hudId, hud);
      } catch (Throwable var6) {
      }
   }

   @Override
   public void hide(Player player, PlayerRef playerRef, String hudId) {
      try {
         this.hideCustomHudMethod.invoke(this.mhudInstance, player, playerRef, hudId);
      } catch (Throwable var5) {
      }
   }

   @Override
   public boolean supportsIncrementalUpdates() {
      return true;
   }
}
