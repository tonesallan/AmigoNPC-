package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class AmigoLogSubCommand extends AbstractPlayerCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoLogSubCommand() {
      super("log", AmigoText.text("cmd.desc.amigo.log"));
      this.setAllowsExtraArguments(false);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      UUID ownerId = playerRef.getUuid();
      boolean enabled = AmigoNpcManager.getShared().toggleDebugLog(ownerId);
      ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.log.status", AmigoText.coloredOnOff(enabled))));
   }
}
