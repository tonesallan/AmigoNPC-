package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
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

public final class AmigoHudSubCommand extends AbstractPlayerCommand {
   private final OptionalArg actionArg = this.withOptionalArg("action", AmigoText.text("cmd.arg.amigo.hud.action"), ArgTypes.STRING);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoHudSubCommand() {
      super("hud", AmigoText.text("cmd.desc.amigo.hud"));
      this.setAllowsExtraArguments(false);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      String action = this.actionArg.provided(ctx) ? String.valueOf(this.actionArg.get(ctx)) : "";
      action = action.trim().toLowerCase();
      if (!"toggle".equals(action)) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.hud.usage")));
      } else {
         boolean enabled = false;

         try {
            enabled = LevelProgressHudService.getShared().toggleEnabledPersisted(playerRef.getUuid());
         } catch (Throwable t) {
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.hud.toggle_failed", t.getClass().getSimpleName())));
            return;
         }

         ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.hud.status", AmigoText.coloredOnOff(enabled))));
      }
   }
}
