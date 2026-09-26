package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;

public final class AmigoDespawnCommand extends AbstractCommand {
   private final AmigoService service;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoDespawnCommand(AmigoService service) {
      super("amigodespawn", AmigoText.text("cmd.desc.amigodespawn"));
      this.requireNoPermission();
      this.service = service;
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      this.service.despawn(ctx);
      return CompletableFuture.completedFuture(null);
   }
}
