package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.ui.lvlgui.AmigoLvlGuiService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.CompletableFuture;

public final class AmigoCommand extends AbstractCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoCommand(AmigoService service) {
      super("amigo", "Comandos do AmigoNPC");
      this.setAllowsExtraArguments(true);
      this.addSubCommand(new AmigoSpawnSubCommand(service));
      this.addSubCommand(new AmigoDespawnSubCommand(service));
      this.addSubCommand(new AmigoModeloSubCommand());
      this.addSubCommand(new AmigoModeloOffSubCommand());
      this.addSubCommand(new AmigoDefenderSubCommand());
      this.addSubCommand(new AmigoLogSubCommand());
      this.addSubCommand(new AmigoGodmodSubCommand());
      this.addSubCommand(new AmigoHudSubCommand());
      this.addSubCommand(new AmigoStatsSubCommand());
      this.addSubCommand(new AmigoRewardsSubCommand());
      this.addSubCommand(new AmigoAddNpcXpSubCommand());
      this.addSubCommand(new AmigoPlayerStatsSubCommand());
      this.addSubCommand(new AmigoMobScalingSubCommand());
      this.addSubCommand(new AmigoReloadSubCommand());
   }

   protected CompletableFuture execute(CommandContext ctx) {
      try {
         String in = ctx.getInputString();
         if (in != null) {
            in = in.trim();
            if (in.startsWith("/")) {
               in = in.substring(1);
            }

            if (!in.isBlank()) {
               String[] parts = in.split("\\s+");
               if (parts.length > 1 && "amigo".equalsIgnoreCase(parts[0])) {
                  String sub = parts[1].toLowerCase();
                  if (!"ui".equals(sub) && !"ui2".equals(sub) && !"lvl".equals(sub) && !"lvlup".equals(sub) && !"lvldown".equals(sub)) {
                     ctx.sendMessage(Message.raw("§c[AmigoNPC] Subcomando desconhecido. Use apenas §f/amigo§c ou um subcomando válido."));
                     return CompletableFuture.completedFuture((Object)null);
                  }

                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      } catch (Throwable var12) {
      }

      CommandSender sender = ctx.sender();
      if (sender instanceof Player player) {
         try {
            ActionTraceService.getShared().record(ctx.sender().getUuid(), "command", "/amigo");
         } catch (Throwable var11) {
         }

         Ref<EntityStore> ref = player.getReference();
         if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = null;

            try {
               Object ext = store.getExternalData();
               if (ext instanceof EntityStore) {
                  EntityStore es = (EntityStore)ext;
                  world = es.getWorld();
               }
            } catch (Throwable var10) {
            }

            if (world == null) {
               try {
                  PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
                  if (playerRef != null) {
                     AmigoLvlGuiService.getShared().toggle(player, ref, store, playerRef);
                  }
               } catch (Throwable var9) {
               }

               return CompletableFuture.completedFuture((Object)null);
            } else {
               return CompletableFuture.runAsync(() -> {
                  try {
                     PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
                     if (playerRef != null) {
                        AmigoLvlGuiService.getShared().toggle(player, ref, store, playerRef);
                     }
                  } catch (Throwable var4) {
                  }

               }, world);
            }
         } else {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados do player indisponíveis (Ref inválida)."));
            return CompletableFuture.completedFuture((Object)null);
         }
      } else {
         ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando é apenas para players."));
         return CompletableFuture.completedFuture((Object)null);
      }
   }
}
