package br.tones.amigonpc.core;

import br.tones.amigonpc.core.i18n.AmigoText;
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

   public void spawn(CommandContext ctx, World world, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef) {
      if (ctx != null && world != null && store != null && playerEntityRef != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.isDowned(ownerId)
            ? this.manager.requestRespawn(world, ownerId, playerRef)
            : this.manager.spawnWithStore(world, store, playerEntityRef, ownerId, playerRef);
         if (!ok) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.spawn_failed")));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
            }
         } else {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.spawned")));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.invalid_spawn_data")));
         }
      }
   }

   public void despawn(CommandContext ctx, World world, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef) {
      if (ctx != null && world != null && store != null && playerEntityRef != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.despawnWithStore(store, ownerId);
         if (!ok) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.despawn_failed")));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
            }
         } else {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.despawned")));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.invalid_despawn_data")));
         }
      }
   }

   public void spawn(CommandContext ctx) {
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.only_players")));
      } else {
         UUID ownerId = ctx.sender().getUuid();
         Object world = HytaleBridge.tryGetWorldFromCommandContext(ctx);
         if (world == null) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.world_unavailable")));
            String err = HytaleBridge.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
            }
         } else {
            boolean ok = this.manager.isDowned(ownerId)
               ? this.manager.requestRespawn(world, ownerId, ctx.sender())
               : this.manager.spawn(world, ownerId, ctx.sender());
            if (!ok) {
               ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.spawn_failed")));
               String err = this.manager.getLastError();
               if (err != null) {
                  ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
               }
            } else {
               ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.spawned")));
            }
         }
      }
   }

   public void despawn(CommandContext ctx) {
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.only_players")));
      } else {
         UUID ownerId = ctx.sender().getUuid();
         Object world = HytaleBridge.tryGetWorldFromCommandContext(ctx);
         if (world == null) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.world_unavailable")));
            String err = HytaleBridge.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
            }
         } else {
            boolean ok = this.manager.despawn(world, ownerId);
            if (!ok) {
               ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.despawn_failed")));
               String err = this.manager.getLastError();
               if (err != null) {
                  ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
               }
            } else {
               ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.despawned")));
            }
         }
      }
   }

   public void spawn(CommandContext ctx, World world, PlayerRef playerRef) {
      if (ctx != null && world != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.isDowned(ownerId)
            ? this.manager.requestRespawn(world, ownerId, playerRef)
            : this.manager.spawn(world, ownerId, playerRef);
         if (!ok) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.spawn_failed")));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
            }
         } else {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.spawned")));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.invalid_spawn_data")));
         }
      }
   }

   public void despawn(CommandContext ctx, World world, PlayerRef playerRef) {
      if (ctx != null && world != null && playerRef != null) {
         UUID ownerId = playerRef.getUuid();
         boolean ok = this.manager.despawn(world, ownerId);
         if (!ok) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.despawn_failed")));
            String err = this.manager.getLastError();
            if (err != null) {
               ctx.sendMessage(Message.raw(AmigoText.format("svc.amigo.debug", err)));
            }
         } else {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.despawned")));
         }
      } else {
         if (ctx != null) {
            ctx.sendMessage(Message.raw(AmigoText.text("svc.amigo.invalid_despawn_data")));
         }
      }
   }
}
