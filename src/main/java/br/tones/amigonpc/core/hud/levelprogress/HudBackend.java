package br.tones.amigonpc.core.hud.levelprogress;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

interface HudBackend {
   void show(Player var1, PlayerRef var2, String var3, CustomUIHud var4);

   void hide(Player var1, PlayerRef var2, String var3);

   boolean supportsIncrementalUpdates();
}
