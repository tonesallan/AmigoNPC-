package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AutoLootCommand extends AbstractCommand {
   public AutoLootCommand() {
      super("autoloot", AmigoText.text("cmd.desc.autoloot"));
      this.setAllowsExtraArguments(true);
      this.addSubCommand(new AutoLootCommand.On());
      this.addSubCommand(new AutoLootCommand.Off());
   }

   public boolean canGeneratePermission() {
      return false;
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.autoloot.only_players")));
         return CompletableFuture.completedFuture(null);
      } else {
         UUID ownerId = ctx.sender().getUuid();
         boolean enabled = AmigoNpcManager.getShared().toggleAutoLoot(ownerId);
         ctx.sendMessage(Message.raw(AmigoText.format("cmd.autoloot.status", AmigoText.onOff(enabled))));
         return CompletableFuture.completedFuture(null);
      }
   }

   private static final class Off extends AbstractCommand {
      Off() {
         super("off", AmigoText.text("cmd.desc.autoloot.off"));
      }

      public boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.autoloot.only_players")));
            return CompletableFuture.completedFuture(null);
         } else {
            UUID ownerId = ctx.sender().getUuid();
            AmigoNpcManager.getShared().setAutoLootEnabled(ownerId, false);
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.autoloot.status", AmigoText.text("common.off"))));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class On extends AbstractCommand {
      On() {
         super("on", AmigoText.text("cmd.desc.autoloot.on"));
      }

      public boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.autoloot.only_players")));
            return CompletableFuture.completedFuture(null);
         } else {
            UUID ownerId = ctx.sender().getUuid();
            AmigoNpcManager.getShared().setAutoLootEnabled(ownerId, true);
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.autoloot.status", AmigoText.text("common.on"))));
            return CompletableFuture.completedFuture(null);
         }
      }
   }
}
