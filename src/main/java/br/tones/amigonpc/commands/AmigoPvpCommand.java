package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import java.util.concurrent.CompletableFuture;

public final class AmigoPvpCommand extends AbstractCommand {
   public AmigoPvpCommand() {
      super("amigopvp", AmigoText.text("cmd.desc.amigopvp"));
      this.setAllowsExtraArguments(true);
      this.addSubCommand(new AmigoPvpCommand.On());
      this.addSubCommand(new AmigoPvpCommand.Off());
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigopvp.usage")));
      return CompletableFuture.completedFuture(null);
   }

   private static boolean isAdmin(CommandContext ctx) {
      try {
         CommandSender sender = ctx.sender();
         if (sender == null) {
            return false;
         }

         String opNode = HytalePermissions.fromCommand("op");
         return sender.hasPermission(opNode, false)
            || sender.hasPermission("hytale.command.op", false)
            || sender.hasPermission("hytale.*", false)
            || sender.hasPermission("*", false);
      } catch (Throwable t) {
         return false;
      }
   }

   private static final class Off extends AbstractCommand {
      Off() {
         super("off", AmigoText.text("cmd.desc.amigopvp.off"));
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (!AmigoPvpCommand.isAdmin(ctx)) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigopvp.only_admin")));
            return CompletableFuture.completedFuture(null);
         } else {
            AmigoNpcManager.getShared().setPvpEnabled(false);
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigopvp.status", AmigoText.text("common.off"))));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class On extends AbstractCommand {
      On() {
         super("on", AmigoText.text("cmd.desc.amigopvp.on"));
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (!AmigoPvpCommand.isAdmin(ctx)) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigopvp.only_admin")));
            return CompletableFuture.completedFuture(null);
         } else {
            AmigoNpcManager.getShared().setPvpEnabled(true);
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigopvp.status", AmigoText.text("common.on"))));
            return CompletableFuture.completedFuture(null);
         }
      }
   }
}
