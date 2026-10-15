package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;

public final class AmigoLvlCommand extends AbstractCommand {
   public AmigoLvlCommand() {
      super("amigolvl", "Ajusta o nível de espada do Amigo (admin)");
      this.setAllowsExtraArguments(true);
      this.addSubCommand(new AmigoLvlUpSubCommand());
      this.addSubCommand(new AmigoLvlDownSubCommand());
   }

   protected CompletableFuture execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigolvl up §7| §f/amigolvl down"));
      return CompletableFuture.completedFuture((Object)null);
   }

   static AmigoNpcManager manager() {
      return AmigoNpcManager.getShared();
   }
}
