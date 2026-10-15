package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class AmigoGodmodSubCommand extends AbstractPlayerCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoGodmodSubCommand() {
      super("godmod", "Liga/desliga o modo de invencibilidade do AmigoNPC");
      this.setAllowsExtraArguments(false);
   }

   protected void execute(CommandContext ctx, Store store, Ref playerEntityRef, PlayerRef playerRef, World world) {
      UUID ownerId = playerRef.getUuid();
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      if (!mgr.hasNpc(ownerId)) {
         ctx.sendMessage(Message.raw("§7[AmigoNPC] Você ainda não tem um NPC. Use §f/amigo spawn§7."));
      } else {
         mgr.toggleGodModeWithStore(store, ownerId);
         boolean enabled = mgr.isGodMode(ownerId);
         ctx.sendMessage(Message.raw("§7[AmigoNPC] GodMode: " + (enabled ? "§aON§7 (invencível)" : "§cOFF§7")));
      }
   }
}
