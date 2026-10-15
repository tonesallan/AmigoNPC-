package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class AmigoService {
   private final AmigoNpcManager manager = AmigoNpcManager.getShared();

   public void spawn(CommandContext ctx, World world, Store store, Ref playerEntityRef, PlayerRef playerRef) {
      if (ctx != null && world != null && store != null && playerEntityRef != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.spawnWithStore(world, store, playerEntityRef, ownerId, playerRef);
         if (!ok) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Falha ao spawnar o NPC."));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            }

         } else {
            ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC criado! Use §f/amigo despawn §apara remover."));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para spawn."));
         }

      }
   }

   public void despawn(CommandContext ctx, World world, Store store, Ref playerEntityRef, PlayerRef playerRef) {
      if (ctx != null && world != null && store != null && playerEntityRef != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.despawnWithStore(store, ownerId);
         if (!ok) {
            ctx.sendMessage(Message.raw("§e[AmigoNPC] Não foi possível remover o NPC agora."));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            }

         } else {
            ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC removido!"));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para despawn."));
         }

      }
   }

   public void spawn(CommandContext ctx) {
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
      } else {
         UUID ownerId = ctx.sender().getUuid();
         Object world = HytaleBridge.tryGetWorldFromCommandContext(ctx);
         if (world == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui obter o World do jogador nesta build."));
            String err = HytaleBridge.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            }

         } else {
            boolean ok = this.manager.spawn(world, ownerId, ctx.sender());
            if (!ok) {
               ctx.sendMessage(Message.raw("§c[AmigoNPC] Falha ao spawnar o NPC."));
               String err = this.manager.getLastError();
               if (err != null) {
                  ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
               }

            } else {
               ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC criado! Use §f/amigo despawn §apara remover."));
            }
         }
      }
   }

   public void despawn(CommandContext ctx) {
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
      } else {
         UUID ownerId = ctx.sender().getUuid();
         Object world = HytaleBridge.tryGetWorldFromCommandContext(ctx);
         if (world == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui obter o World do jogador nesta build."));
            String err = HytaleBridge.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            }

         } else {
            boolean ok = this.manager.despawn(world, ownerId);
            if (!ok) {
               ctx.sendMessage(Message.raw("§e[AmigoNPC] Não foi possível remover o NPC agora."));
               String err = this.manager.getLastError();
               if (err != null) {
                  ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
               }

            } else {
               ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC removido!"));
            }
         }
      }
   }

   public void spawn(CommandContext ctx, World world, PlayerRef playerRef) {
      if (ctx != null && world != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.spawn(world, ownerId, playerRef);
         if (!ok) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Falha ao spawnar o NPC."));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            }

         } else {
            ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC criado! Use §f/amigo despawn §apara remover."));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para spawn."));
         }

      }
   }

   public void despawn(CommandContext ctx, World world, PlayerRef playerRef) {
      if (ctx != null && world != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.despawn(world, ownerId);
         if (!ok) {
            ctx.sendMessage(Message.raw("§e[AmigoNPC] Não foi possível remover o NPC agora."));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            }

         } else {
            ctx.sendMessage(Message.raw("§a[AmigoNPC] NPC removido!"));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Dados inválidos para despawn."));
         }

      }
   }
}
