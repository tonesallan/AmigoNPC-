package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksConfig;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksConfigService;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Locale;

public final class AmigoPlayerStatsSubCommand extends AbstractPlayerCommand {
   private final OptionalArg actionArg = this.withOptionalArg("action", AmigoText.text("cmd.arg.amigo.playerstats.action"), ArgTypes.STRING);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoPlayerStatsSubCommand() {
      super("playerstats", AmigoText.text("cmd.desc.amigo.playerstats"));
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      if (!isAdmin(ctx)) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.playerstats.only_op")));
      } else {
         String action = this.actionArg.provided(ctx) ? String.valueOf(this.actionArg.get(ctx)) : null;
         if (action == null) {
            try {
               String in = ctx.getInputString();
               if (in != null) {
                  if (in.startsWith("/")) {
                     in = in.substring(1);
                  }

                  String[] t = in.trim().split("\\s+");

                  for (int i = 0; i < t.length; i++) {
                     if ("playerstats".equalsIgnoreCase(t[i]) && i + 1 < t.length) {
                        action = t[i + 1];
                        break;
                     }
                  }
               }
            } catch (Throwable var11) {
            }
         }

         action = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
         if (!action.equals("on") && !action.equals("off")) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.playerstats.usage")));
         } else {
            PlayerStatTweaksConfig cfg = PlayerStatTweaksConfigService.get();
            cfg.enablePlayerStatTweaks = action.equals("on");
            PlayerStatTweaksConfigService.save(cfg);
            PlayerStatTweaksConfigService.reloadNow();

            try {
               Player p = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
               if (p != null) {
                  PlayerStatTweaksService.getShared().applyOrRemove(p, playerRef.getUuid(), true);
               }
            } catch (Throwable var10) {
            }

            ctx.sendMessage(Message.raw(AmigoText.text(cfg.enablePlayerStatTweaks ? "cmd.amigo.playerstats.on" : "cmd.amigo.playerstats.off")));
         }
      }
   }

   private static boolean isAdmin(CommandContext ctx) {
      try {
         CommandSender sender = ctx != null ? ctx.sender() : null;
         if (sender == null) {
            return false;
         }

         String opNode = HytalePermissions.fromCommand("op");
         return sender.hasPermission(opNode, false)
            || sender.hasPermission("hytale.command.op", false)
            || sender.hasPermission("hytale.*", false)
            || sender.hasPermission("*", false)
            || sender.hasPermission("OP")
            || sender.hasPermission("op")
            || sender.hasPermission("operator")
            || sender.hasPermission("admin");
      } catch (Throwable t) {
         return false;
      }
   }
}
