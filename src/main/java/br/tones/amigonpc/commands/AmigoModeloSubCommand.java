package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoModeloSubCommand extends AbstractPlayerCommand {
   private final RequiredArg modelArg = this.withRequiredArg("modelId", AmigoText.text("cmd.arg.amigo.modelo.model_id"), ArgTypes.STRING);
   private final OptionalArg scaleArg = this.withOptionalArg("scale", AmigoText.text("cmd.arg.amigo.modelo.scale"), ArgTypes.DOUBLE);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoModeloSubCommand() {
      super("modelo", AmigoText.text("cmd.desc.amigo.modelo"));
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      String modelId = (String)this.modelArg.get(ctx);
      double scale = 1.0;
      if (this.scaleArg.provided(ctx) && this.scaleArg.get(ctx) instanceof Double d) {
         scale = d;
      }

      AmigoPersistence.saveModel(playerRef.getUuid(), modelId, scale);
      ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.modelo.saved", modelId, scale)));
      ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.common.apply_respawn")));
   }
}
