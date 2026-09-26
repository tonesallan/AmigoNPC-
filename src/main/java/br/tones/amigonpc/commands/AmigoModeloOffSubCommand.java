package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoModeloOffSubCommand extends AbstractPlayerCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoModeloOffSubCommand() {
      super("modelooff", AmigoText.text("cmd.desc.amigo.modelooff"));
      this.setAllowsExtraArguments(false);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      AmigoPersistence.saveModel(playerRef.getUuid(), null, 1.0);
      ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.modelo.removed")));
      ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.common.apply_respawn")));
   }
}
