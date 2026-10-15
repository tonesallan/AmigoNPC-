package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;

public final class AmigoSpawnCommand extends AbstractCommand {
   private final AmigoService service;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoSpawnCommand(AmigoService service) {
      super("amigospawn", "Spawna o NPC do AmigoNPC");
      this.service = service;
   }

   protected CompletableFuture execute(CommandContext ctx) {
      this.service.spawn(ctx);
      return CompletableFuture.completedFuture((Object)null);
   }
}
