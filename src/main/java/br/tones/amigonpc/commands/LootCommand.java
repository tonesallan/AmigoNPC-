package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.loot.LootService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LootCommand extends AbstractCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public LootCommand() {
      super("loot", "Abre a mochila do seu AmigoNPC");
   }

   protected CompletableFuture execute(CommandContext ctx) {
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
         return CompletableFuture.completedFuture((Object)null);
      } else {
         UUID owner = ctx.sender().getUuid();

         try {
            ActionTraceService.getShared().record(owner, "command", "/loot");
         } catch (Throwable var9) {
         }

         AmigoNpcManager manager = AmigoNpcManager.getShared();
         if (!manager.hasNpc(owner)) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Você ainda não tem um Amigo ativo. Use §f/amigo spawn§c."));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            Player sender = (Player)ctx.senderAs(Player.class);
            if (sender == null) {
               ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui obter o Player nesta build."));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               PlayerRef playerRef = sender.getPlayerRef();
               if (playerRef == null) {
                  ctx.sendMessage(Message.raw("§c[AmigoNPC] PlayerRef indisponível. Tente relogar."));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  Ref<EntityStore> ref = playerRef.getReference();
                  if (ref != null && ref.isValid()) {
                     Store<EntityStore> store = ref.getStore();
                     boolean ok = LootService.getShared().openLoot(owner, playerRef, ref, store);
                     if (!ok) {
                        ctx.sendMessage(Message.raw("§c[AmigoNPC] Não foi possível abrir a mochila agora."));
                     }

                     return CompletableFuture.completedFuture((Object)null);
                  } else {
                     ctx.sendMessage(Message.raw("§c[AmigoNPC] Você não está em um mundo agora. Tente relogar."));
                     return CompletableFuture.completedFuture((Object)null);
                  }
               }
            }
         }
      }
   }
}
