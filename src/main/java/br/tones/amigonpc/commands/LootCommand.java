package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.loot.LootService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class LootCommand extends AbstractPlayerCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public LootCommand() {
      super("loot", AmigoText.text("cmd.desc.loot"));
      this.requireNoPermission();
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      UUID owner = ctx.sender().getUuid();

      try {
         ActionTraceService.getShared().record(owner, "command", "/loot");
      } catch (Throwable var9) {
      }

      AmigoNpcManager manager = AmigoNpcManager.getShared();
      if (!manager.hasNpc(owner)) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.loot.no_active_npc")));
      } else if (playerRef == null) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.loot.player_ref_unavailable")));
      } else if (playerEntityRef != null && playerEntityRef.isValid()) {
         boolean ok = LootService.getShared().openLoot(owner, playerRef, playerEntityRef, store);
         if (!ok) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.loot.open_failed")));
         }
      } else {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.loot.world_unavailable")));
      }
   }
}
