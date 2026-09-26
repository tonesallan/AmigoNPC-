package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;

public final class AmigoLvlCommand extends AbstractCommand {
   public AmigoLvlCommand() {
      super("amigolvl", AmigoText.text("cmd.desc.amigolvl"));
      this.setAllowsExtraArguments(true);
      this.addSubCommand(new AmigoLvlUpSubCommand());
      this.addSubCommand(new AmigoLvlDownSubCommand());
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigolvl.usage")));
      return CompletableFuture.completedFuture(null);
   }

   static AmigoNpcManager manager() {
      return AmigoNpcManager.getShared();
   }
}
