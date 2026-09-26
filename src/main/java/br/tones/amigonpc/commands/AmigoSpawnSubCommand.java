package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoSpawnSubCommand extends AbstractPlayerCommand {
   private final AmigoService service;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoSpawnSubCommand(AmigoService service) {
      super("spawn", AmigoText.text("cmd.desc.amigo.spawn"));
      this.service = service;
      this.setAllowsExtraArguments(false);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      try {
         ActionTraceService.getShared().record(ctx.sender().getUuid(), "command", "/amigo spawn");
      } catch (Throwable var7) {
      }

      this.service.spawn(ctx, world, store, playerEntityRef, playerRef);
   }
}
