package br.tones.amigonpc.commands;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.xp.sources.NpcXpAwardService;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfig;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfigService;
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

public final class AmigoAddNpcXpSubCommand extends AbstractPlayerCommand {
   private final OptionalArg amountArg = this.withOptionalArg("amount", AmigoText.text("cmd.arg.amigo.addnpcxp.amount"), ArgTypes.INTEGER);
   private final OptionalArg sourceArg = this.withOptionalArg("source", AmigoText.text("cmd.arg.amigo.addnpcxp.source"), ArgTypes.STRING);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoAddNpcXpSubCommand() {
      super("addnpcxp", AmigoText.text("cmd.desc.amigo.addnpcxp"));
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      if (!isAdmin(ctx)) {
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.addnpcxp.only_op")));
      } else {
         NpcXpSourcesConfig cfg = NpcXpSourcesConfigService.get();
         if (!cfg.enableCommandXP) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.addnpcxp.disabled")));
         } else {
            Integer amt = null;
            if (this.amountArg.provided(ctx) && this.amountArg.get(ctx) instanceof Integer i) {
               amt = i;
            }

            String srcTxt = null;
            if (this.sourceArg.provided(ctx)) {
               Object v = this.sourceArg.get(ctx);
               if (v != null) {
                  srcTxt = String.valueOf(v);
               }
            }

            if (amt == null) {
               try {
                  String in = ctx.getInputString();
                  if (in != null) {
                     if (in.startsWith("/")) {
                        in = in.substring(1);
                     }

                     String[] t = in.trim().split("\\s+");

                     for (int i = 0; i < t.length; i++) {
                        if ("addnpcxp".equalsIgnoreCase(t[i]) && i + 1 < t.length) {
                           try {
                              amt = Integer.parseInt(t[i + 1]);
                           } catch (Throwable var15) {
                           }

                           if (i + 2 < t.length) {
                              srcTxt = t[i + 2];
                           }
                           break;
                        }
                     }
                  }
               } catch (Throwable var16) {
               }
            }

            if (amt != null && amt > 0) {
               NpcXpSource src = NpcXpSource.COMMAND;
               if (srcTxt != null && !srcTxt.isBlank()) {
                  try {
                     src = NpcXpSource.valueOf(srcTxt.trim().toUpperCase(Locale.ROOT));
                  } catch (Throwable ignored) {
                     src = NpcXpSource.COMMAND;
                  }
               }

               String wn = "";

               try {
                  if (world != null && world.getName() != null) {
                     wn = world.getName();
                  }
               } catch (Throwable var13) {
               }

               NpcXpContext ctxObj = new NpcXpContext(wn, wn, null, 0, null, 0, System.currentTimeMillis());
               boolean ok = NpcXpAwardService.getShared().awardFixed(playerRef.getUuid(), amt.intValue(), src, ctxObj);
               if (!ok) {
                  ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.addnpcxp.failed")));
               } else {
                  ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.addnpcxp.success", amt, src.name())));
               }
            } else {
               ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.addnpcxp.usage")));
            }
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
