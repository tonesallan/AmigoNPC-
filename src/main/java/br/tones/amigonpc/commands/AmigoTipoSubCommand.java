package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoTipoSubCommand extends AbstractPlayerCommand {
   private final RequiredArg typeArg = this.withRequiredArg("npcType", "Nome do role template (use /amigo tipos)", ArgTypes.STRING);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoTipoSubCommand() {
      super("tipo", AmigoText.text("cmd.desc.amigo.tipo"));
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      String npcType = (String)this.typeArg.get(ctx);
      if (npcType != null && !npcType.isBlank()) {
         AmigoPersistence.saveNpcType(playerRef.getUuid(), npcType);
         ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.tipo.saved", npcType)));
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.common.apply_respawn")));
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.tipo.model_priority")));
      } else {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.tipo.invalid")));
      }
   }
}
