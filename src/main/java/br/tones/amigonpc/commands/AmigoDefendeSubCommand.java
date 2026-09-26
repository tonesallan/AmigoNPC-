package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class AmigoDefendeSubCommand extends AbstractPlayerCommand {
   private final OptionalArg valueArg = this.withOptionalArg("valor", AmigoText.text("cmd.arg.amigo.defende.value"), ArgTypes.STRING);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoDefendeSubCommand() {
      super("defende", AmigoText.text("cmd.desc.amigo.defende"));
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      AmigoNpcManager manager = AmigoNpcManager.getShared();
      UUID owner = playerRef.getUuid();
      boolean current = manager.isDefendeEnabled(owner);
      boolean next;
      if (!this.valueArg.provided(ctx)) {
         next = !current;
      } else {
         Object raw = this.valueArg.get(ctx);
         String s = raw == null ? "" : raw.toString().trim().toLowerCase();
         if (s.isEmpty()) {
            next = !current;
         } else if (!s.equals("on") && !s.equals("true") && !s.equals("1") && !s.equals("sim")) {
            if (!s.equals("off") && !s.equals("false") && !s.equals("0") && !s.equals("nao") && !s.equals("não")) {
               ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.defende.usage")));
               return;
            }

            next = false;
         } else {
            next = true;
         }
      }

      manager.setDefendeEnabled(owner, next);
      if (next) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.defende.on")));
      } else {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.defende.off")));
      }
   }
}
