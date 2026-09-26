package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingConfig;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingConfigService;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Locale;

public final class AmigoMobScalingSubCommand extends AbstractPlayerCommand {
   private final OptionalArg actionArg = this.withOptionalArg("action", AmigoText.text("cmd.arg.amigo.mobscaling.action"), ArgTypes.STRING);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoMobScalingSubCommand() {
      super("mobscaling", AmigoText.text("cmd.desc.amigo.mobscaling"));
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      if (!isAdmin(ctx)) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.mobscaling.only_op")));
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
                     if ("mobscaling".equalsIgnoreCase(t[i]) && i + 1 < t.length) {
                        action = t[i + 1];
                        break;
                     }
                  }
               }
            } catch (Throwable var10) {
            }
         }

         action = action == null ? "status" : action.trim().toLowerCase(Locale.ROOT);
         WorldMobScalingConfig cfg = WorldMobScalingConfigService.get();
         if (action.equals("status")) {
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.mobscaling.status", AmigoText.coloredOnOff(cfg.enableWorldMobScaling))));
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.mobscaling.whitelist_worlds", String.valueOf(cfg.whitelistWorlds))));
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.mobscaling.whitelist_instances", String.valueOf(cfg.whitelistInstances))));
         } else if (!action.equals("on") && !action.equals("off")) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.mobscaling.usage")));
         } else {
            cfg.enableWorldMobScaling = action.equals("on");
            WorldMobScalingConfigService.save(cfg);
            WorldMobScalingConfigService.reloadNow();
            WorldMobScalingService.getShared().clearCache();
            ctx.sendMessage(Message.raw(AmigoText.text(cfg.enableWorldMobScaling ? "cmd.amigo.mobscaling.on" : "cmd.amigo.mobscaling.off")));
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
