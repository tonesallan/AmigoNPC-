package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.HytaleBridge;
import br.tones.amigonpc.core.debug.BuildInfo;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.ui.AmigoUiFactory;
import br.tones.amigonpc.ui.UiBridge;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AmigoDebugCommand extends AbstractCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoDebugCommand() {
      super("amigo", AmigoText.text("cmd.desc.amigodebug"));
   }

   public static AmigoDebugCommand createAsStandalone() {
      return new AmigoDebugCommand("amigodebug");
   }

   private AmigoDebugCommand(String name) {
      super(name, "Mostra informações de debug do AmigoNPC");
      this.setAllowsExtraArguments(false);
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigodebug.title")));
      String v = BuildInfo.getModVersion();
      String bid = BuildInfo.getBuildId();
      ctx.sendMessage(
         Message.raw(
            AmigoText.format(
               "cmd.amigodebug.version_build",
               v == null ? AmigoText.text("cmd.amigodebug.unknown") : v,
               bid == null ? AmigoText.text("cmd.amigodebug.unknown") : bid
            )
         )
      );
      if (ctx.isPlayer()) {
         UUID ownerId = ctx.sender().getUuid();
         boolean enabled = AmigoNpcManager.getShared().toggleDebugLog(ownerId);
         ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigodebug.debuglog", AmigoText.coloredOnOff(enabled))));
      } else {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigodebug.debuglog_players_only")));
      }

      String hb = HytaleBridge.getLastError();
      ctx.sendMessage(
         Message.raw(AmigoText.format("cmd.amigodebug.bridge_status", "HytaleBridge", hb == null ? AmigoText.text("cmd.amigodebug.bridge_ok") : hb))
      );
      String ub = UiBridge.getLastError();
      ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigodebug.bridge_status", "UiBridge", ub == null ? AmigoText.text("cmd.amigodebug.bridge_ok") : ub)));
      String uf = AmigoUiFactory.getLastError();
      ctx.sendMessage(
         Message.raw(AmigoText.format("cmd.amigodebug.bridge_status", "AmigoUiFactory", uf == null ? AmigoText.text("cmd.amigodebug.bridge_ok") : uf))
      );
      ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigodebug.tip")));
      return CompletableFuture.completedFuture(null);
   }
}
