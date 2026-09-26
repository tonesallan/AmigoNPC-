package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.npcstats.NpcStatsState;
import br.tones.amigonpc.core.rewards.RewardsStateService;
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
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class AmigoStatsSubCommand extends AbstractPlayerCommand {
   private final OptionalArg actionArg = this.withOptionalArg("action", AmigoText.text("cmd.arg.amigo.stats.action"), ArgTypes.STRING);
   private final OptionalArg attrArg = this.withOptionalArg("attribute", AmigoText.text("cmd.arg.amigo.stats.attribute"), ArgTypes.STRING);
   private final OptionalArg amountArg = this.withOptionalArg("amount", AmigoText.text("cmd.arg.amigo.stats.amount"), ArgTypes.INTEGER);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoStatsSubCommand() {
      super("stats", AmigoText.text("cmd.desc.amigo.stats"));
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      String action = this.actionArg.provided(ctx) ? String.valueOf(this.actionArg.get(ctx)) : null;
      String attrInput = this.attrArg.provided(ctx) ? String.valueOf(this.attrArg.get(ctx)) : null;
      Integer amtInput = null;
      if (this.amountArg.provided(ctx) && this.amountArg.get(ctx) instanceof Integer i) {
         amtInput = i;
      }

      if (action == null) {
         AmigoStatsSubCommand.ParsedArgs fb = parseFallback(ctx);
         if (fb != null) {
            action = fb.action;
            if (attrInput == null) {
               attrInput = fb.attr;
            }

            if (amtInput == null) {
               amtInput = fb.amount;
            }
         }
      }

      action = action != null && !action.isBlank() ? action.trim().toLowerCase(Locale.ROOT) : "info";
      UUID ownerId = playerRef.getUuid();
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      int lvl = mgr.getNpcLevel(ownerId);
      boolean op = isAdmin(ctx);
      if (action.equals("add")) {
         if (attrInput != null && !attrInput.isBlank()) {
            NpcAttribute a = NpcAttribute.parse(attrInput);
            if (a == null) {
               ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.stats.invalid_attribute")));
            } else {
               int amt = 1;
               if (amtInput != null) {
                  amt = amtInput;
               }

               if (amt <= 0) {
                  amt = 1;
               }

               NpcStatsService svc = NpcStatsService.getShared();
               NpcStatsService.AddResult res = op ? svc.addForce(ownerId, lvl, a, amt) : svc.add(ownerId, lvl, a, amt);
               if (!res.ok()) {
                  if ("no_points".equals(res.error())) {
                     NpcStatsState st = svc.load(ownerId);
                     int earned = svc.totalEarnedPoints(lvl);
                     int spent = st.spentPoints();
                     int avail = Math.max(0, earned - spent);
                     ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.stats.no_points", lvl, earned, spent, avail)));
                  } else {
                     ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.stats.add.failed", res.error())));
                  }
               } else {
                  ctx.sendMessage(
                     Message.raw(AmigoText.format(op ? "cmd.amigo.stats.add.success_admin" : "cmd.amigo.stats.add.success", res.applied(), a.name()))
                  );

                  try {
                     NpcStatsState st2 = res.state() != null ? res.state() : svc.load(ownerId);
                     int earned = svc.totalEarnedPoints(lvl);
                     int spent = st2.spentPoints();
                     int avail = Math.max(0, earned - spent);
                     int cur = st2.getAllocated(a);
                     ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.stats.current_value", a.name(), cur, avail, earned, spent)));
                  } catch (Throwable var23) {
                  }

                  mgr.requestRescale(ownerId);
               }
            }
         } else {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.stats.add.usage")));
         }
      } else if (action.equals("reset")) {
         if (!op && !RewardsStateService.getShared().consumeResetPoint(ownerId)) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.stats.reset.no_points")));
         } else {
            NpcStatsService.getShared().resetAll(ownerId);
            ctx.sendMessage(Message.raw(AmigoText.text(op ? "cmd.amigo.stats.reset.admin" : "cmd.amigo.stats.reset.player")));
            mgr.requestRescale(ownerId);
         }
      } else {
         NpcStatsState st = NpcStatsService.getShared().load(ownerId);
         int earned = NpcStatsService.getShared().totalEarnedPoints(lvl);
         int spent = st.spentPoints();
         int avail = Math.max(0, earned - spent);
         int resetPts = RewardsStateService.getShared().getResetPoints(ownerId);
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.stats.info.header")));
         ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.stats.info.summary", lvl, earned, spent, avail, resetPts)));

         for (NpcAttribute a : NpcAttribute.values()) {
            int v = st.getAllocated(a);
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.stats.info.row", a.name(), v)));
         }

         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.stats.info.usage")));
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.stats.info.reset_usage")));
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
         try {
            CommandSender sender = ctx != null ? ctx.sender() : null;
            if (sender == null) {
               return false;
            }

            for (String m : new String[]{"isOp", "isOperator", "isAdmin"}) {
               try {
                  Method mm = sender.getClass().getMethod(m);
                  if (mm.invoke(sender) instanceof Boolean b) {
                     return b;
                  }
               } catch (Throwable var10) {
               }
            }
         } catch (Throwable var11) {
         }

         return false;
      }
   }

   private static AmigoStatsSubCommand.ParsedArgs parseFallback(CommandContext ctx) {
      try {
         if (ctx == null) {
            return null;
         }

         String in = ctx.getInputString();
         if (in == null) {
            return null;
         }

         in = in.trim();
         if (in.isEmpty()) {
            return null;
         }

         if (in.startsWith("/")) {
            in = in.substring(1);
         }

         String[] t = in.split("\\s+");
         if (t.length == 0) {
            return null;
         }

         int idxStats = -1;

         for (int i = 0; i < t.length; i++) {
            if ("stats".equalsIgnoreCase(t[i])) {
               idxStats = i;
               break;
            }
         }

         if (idxStats >= 0) {
            String action = idxStats + 1 < t.length ? t[idxStats + 1] : null;
            String attr = idxStats + 2 < t.length ? t[idxStats + 2] : null;
            Integer amt = null;
            if (idxStats + 3 < t.length) {
               try {
                  amt = Integer.parseInt(t[idxStats + 3]);
               } catch (Throwable var9) {
               }
            }

            if (action != null) {
               return new AmigoStatsSubCommand.ParsedArgs(action, attr, amt);
            }
         }

         if (t.length >= 2 && "stats".equalsIgnoreCase(t[0])) {
            String action = t[1];
            String attr = t.length >= 3 ? t[2] : null;
            Integer amt = null;
            if (t.length >= 4) {
               try {
                  amt = Integer.parseInt(t[3]);
               } catch (Throwable var8) {
               }
            }

            return new AmigoStatsSubCommand.ParsedArgs(action, attr, amt);
         }
      } catch (Throwable var10) {
      }

      return null;
   }

   private record ParsedArgs(String action, String attr, Integer amount) {
   }
}
