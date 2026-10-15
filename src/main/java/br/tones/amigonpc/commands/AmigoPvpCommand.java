package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import java.util.concurrent.CompletableFuture;

public final class AmigoPvpCommand extends AbstractCommand {
   public AmigoPvpCommand() {
      super("amigopvp", "Ativa/Desativa PvP entre NPCs do AmigoNPC (admin)");
      this.setAllowsExtraArguments(true);
      this.addSubCommand(new On());
      this.addSubCommand(new Off());
   }

   protected CompletableFuture execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigopvp on §7| §f/amigopvp off"));
      return CompletableFuture.completedFuture((Object)null);
   }

   private static boolean isAdmin(CommandContext ctx) {
      try {
         CommandSender sender = ctx.sender();
         if (sender == null) {
            return false;
         } else {
            String opNode = HytalePermissions.fromCommand("op");
            return sender.hasPermission(opNode, false) || sender.hasPermission("hytale.command.op", false) || sender.hasPermission("hytale.*", false) || sender.hasPermission("*", false);
         }
      } catch (Throwable var3) {
         return false;
      }
   }

   private static final class On extends AbstractCommand {
      On() {
         super("on", "Ativar PvP entre NPCs (admin)");
      }

      protected CompletableFuture execute(CommandContext ctx) {
         if (!AmigoPvpCommand.isAdmin(ctx)) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas admin do servidor pode usar este comando."));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            AmigoNpcManager.getShared().setPvpEnabled(true);
            ctx.sendMessage(Message.raw("§a[AmigoNPC] PvP entre NPCs: §fON"));
            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   private static final class Off extends AbstractCommand {
      Off() {
         super("off", "Desativar PvP entre NPCs (admin)");
      }

      protected CompletableFuture execute(CommandContext ctx) {
         if (!AmigoPvpCommand.isAdmin(ctx)) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas admin do servidor pode usar este comando."));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            AmigoNpcManager.getShared().setPvpEnabled(false);
            ctx.sendMessage(Message.raw("§a[AmigoNPC] PvP entre NPCs: §fOFF"));
            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }
}
