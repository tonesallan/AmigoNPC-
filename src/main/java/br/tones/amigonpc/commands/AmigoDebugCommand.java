package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.HytaleBridge;
import br.tones.amigonpc.core.debug.BuildInfo;
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
      super("amigo", "Debug do AmigoNPC");
   }

   public static AmigoDebugCommand createAsStandalone() {
      return new AmigoDebugCommand("amigodebug");
   }

   private AmigoDebugCommand(String name) {
      super(name, "Mostra informações de debug do AmigoNPC");
      this.setAllowsExtraArguments(false);
   }

   protected CompletableFuture execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw("§b[AmigoNPC] Debug"));
      String v = BuildInfo.getModVersion();
      String bid = BuildInfo.getBuildId();
      ctx.sendMessage(Message.raw("§7- Version: " + (v == null ? "(unknown)" : v) + " §8| BuildId: " + (bid == null ? "(unknown)" : bid)));
      if (ctx.isPlayer()) {
         UUID ownerId = ctx.sender().getUuid();
         boolean enabled = AmigoNpcManager.getShared().toggleDebugLog(ownerId);
         ctx.sendMessage(Message.raw("§7- DebugLog: " + (enabled ? "§aON" : "§cOFF")));
      } else {
         ctx.sendMessage(Message.raw("§7- DebugLog: §8(apenas players)"));
      }

      String hb = HytaleBridge.getLastError();
      ctx.sendMessage(Message.raw("§7- HytaleBridge: " + (hb == null ? "OK/sem erro" : hb)));
      String ub = UiBridge.getLastError();
      ctx.sendMessage(Message.raw("§7- UiBridge: " + (ub == null ? "OK/sem erro" : ub)));
      String uf = AmigoUiFactory.getLastError();
      ctx.sendMessage(Message.raw("§7- AmigoUiFactory: " + (uf == null ? "OK/sem erro" : uf)));
      ctx.sendMessage(Message.raw("§7Dica: teste §f/amigo spawn§7, §f/amigo despawn§7, §f/amigo defender§7."));
      return CompletableFuture.completedFuture((Object)null);
   }
}
