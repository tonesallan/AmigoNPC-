package br.tones.amigonpc;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.debug.ServerErrorHook;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.bootstrap.AmigoCommandRegistrar;
import br.tones.amigonpc.core.bootstrap.AmigoRuntimeBootstrap;
import br.tones.amigonpc.core.bootstrap.AmigoSystemRegistrar;
import br.tones.amigonpc.core.bootstrap.NpcRefApiRegistrar;
import br.tones.amigonpc.core.lifecycle.HudLifecycleCoordinator;
import br.tones.amigonpc.core.lifecycle.PlayerSessionCoordinator;
import br.tones.amigonpc.core.lifecycle.TeleportRespawnCoordinator;
import br.tones.amigonpc.core.tick.AmigoTickerCoordinator;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;

public final class AmigoNPCPlugin extends JavaPlugin {
   public static final PluginManifest MANIFEST = PluginManifest.corePlugin(AmigoNPCPlugin.class).build();
   private AmigoService service;
   private final AmigoTickerCoordinator tickerCoordinator = new AmigoTickerCoordinator();
   private final HudLifecycleCoordinator hudLifecycleCoordinator = new HudLifecycleCoordinator(this::getEventRegistry);
   private final PlayerSessionCoordinator playerSessionCoordinator = new PlayerSessionCoordinator(this::getEventRegistry);
   private final TeleportRespawnCoordinator teleportRespawnCoordinator = new TeleportRespawnCoordinator(this::getEventRegistry);
   private final AmigoRuntimeBootstrap runtimeBootstrap = new AmigoRuntimeBootstrap();
   private final NpcRefApiRegistrar npcRefApiRegistrar = new NpcRefApiRegistrar();

   public AmigoNPCPlugin(@Nonnull JavaPluginInit init) {
      super(init);
   }

   protected void setup() {
      this.service = new AmigoService();
      this.npcRefApiRegistrar.register();
      this.runtimeBootstrap.initialize();
      new AmigoCommandRegistrar(this.getCommandRegistry(), this.service).registerAll();
      this.playerSessionCoordinator.register();
      new AmigoSystemRegistrar(this.getEntityStoreRegistry()).registerCoreSystems();
      this.tickerCoordinator.startDownedTicker();
      this.tickerCoordinator.startFollowTicker();
      this.teleportRespawnCoordinator.register();
      this.hudLifecycleCoordinator.register();
      this.tickerCoordinator.startHudTicker();
   }

   protected void shutdown() {
      this.npcRefApiRegistrar.unregister();
      this.tickerCoordinator.shutdown();

      try {
         LevelProgressHudService.getShared().shutdown();
      } catch (Throwable ignored) {
      }

      try {
         AmigoNpcManager.getShared().shutdownForReload();
      } catch (Throwable ignored) {
      }

      try {
         ServerErrorHook.uninstall();
      } catch (Throwable ignored) {
      }

      this.service = null;
   }
}
