package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
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
   private final OptionalArg valueArg;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoDefendeSubCommand() {
      super("defende", "Alterna o modo de defesa/assistência do AmigoNPC (ON/OFF)");
      this.valueArg = this.withOptionalArg("valor", "on/off (sem argumento alterna)", ArgTypes.STRING);
   }

   protected void execute(CommandContext ctx, Store store, Ref playerEntityRef, PlayerRef playerRef, World world) {
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
               ctx.sendMessage(Message.raw("§e[AmigoNPC] Use: §f/amigo defende §7(para alternar) §eou §f/amigo defende on|off"));
               return;
            }

            next = false;
         } else {
            next = true;
         }
      }

      manager.setDefendeEnabled(owner, next);
      if (next) {
         ctx.sendMessage(Message.raw("§a[AmigoNPC] Defesa: §fON§a. (O NPC vai atacar quem te atacar e ajudar no seu alvo)"));
      } else {
         ctx.sendMessage(Message.raw("§c[AmigoNPC] Defesa: §fOFF§c. (O NPC não causará dano)"));
      }

   }
}
