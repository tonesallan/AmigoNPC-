package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
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
   private final OptionalArg actionArg;
   private final OptionalArg attrArg;
   private final OptionalArg amountArg;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoStatsSubCommand() {
      super("stats", "Gerencia pontos de atributo do NPC");
      this.actionArg = this.withOptionalArg("action", "info|add|reset", ArgTypes.STRING);
      this.attrArg = this.withOptionalArg("attribute", "HEALTH|DAMAGE|CRITICAL_DAMAGE|DEFENSE", ArgTypes.STRING);
      this.amountArg = this.withOptionalArg("amount", "Quantidade (default 1)", ArgTypes.INTEGER);
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store store, Ref playerEntityRef, PlayerRef playerRef, World world) {
      String action = this.actionArg.provided(ctx) ? String.valueOf(this.actionArg.get(ctx)) : null;
      String attrInput = this.attrArg.provided(ctx) ? String.valueOf(this.attrArg.get(ctx)) : null;
      Integer amtInput = null;
      if (this.amountArg.provided(ctx)) {
         Object v = this.amountArg.get(ctx);
         if (v instanceof Integer) {
            Integer i = (Integer)v;
            amtInput = i;
         }
      }

      if (action == null) {
         ParsedArgs fb = parseFallback(ctx);
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
               ctx.sendMessage(Message.raw("§cAtributo inválido. Use: HEALTH | DAMAGE | CRITICAL_DAMAGE | DEFENSE (aceita aliases: hp,dmg,crit,def)."));
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
                     ctx.sendMessage(Message.raw(String.format(Locale.ROOT, "§c[AmigoNPC] Sem pontos disponíveis. Level=%d Earned=%d Spent=%d Available=%d", lvl, earned, spent, avail)));
                  } else {
                     ctx.sendMessage(Message.raw("§c[AmigoNPC] Não foi possível alocar: " + res.error()));
                  }

               } else {
                  ctx.sendMessage(Message.raw(op ? "§a[AmigoNPC] +" + res.applied() + " em " + a.name() + " (admin)." : "§a[AmigoNPC] +" + res.applied() + " em " + a.name() + "."));

                  try {
                     NpcStatsState st2 = res.state() != null ? res.state() : svc.load(ownerId);
                     int earned = svc.totalEarnedPoints(lvl);
                     int spent = st2.spentPoints();
                     int avail = Math.max(0, earned - spent);
                     int cur = st2.getAllocated(a);
                     ctx.sendMessage(Message.raw(String.format(Locale.ROOT, "§7[AmigoNPC] %s=%d | Available=%d (Earned=%d Spent=%d)", a.name(), cur, avail, earned, spent)));
                  } catch (Throwable var23) {
                  }

                  mgr.requestRescale(ownerId);
               }
            }
         } else {
            ctx.sendMessage(Message.raw("§cUso: /amigo stats add <atributo> [qtd]"));
         }
      } else if (action.equals("reset")) {
         if (!op && !RewardsStateService.getShared().consumeResetPoint(ownerId)) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Você não tem ResetPoints. Claim rewards para ganhar."));
         } else {
            NpcStatsService.getShared().resetAll(ownerId);
            ctx.sendMessage(Message.raw(op ? "§e[AmigoNPC] Stats resetados (admin)." : "§e[AmigoNPC] Stats resetados (1 ResetPoint consumido)."));
            mgr.requestRescale(ownerId);
         }
      } else {
         NpcStatsState st = NpcStatsService.getShared().load(ownerId);
         int earned = NpcStatsService.getShared().totalEarnedPoints(lvl);
         int spent = st.spentPoints();
         int avail = Math.max(0, earned - spent);
         int resetPts = RewardsStateService.getShared().getResetPoints(ownerId);
         ctx.sendMessage(Message.raw("§7--- §fAmigoNPC Stats §7---"));
         ctx.sendMessage(Message.raw(String.format(Locale.ROOT, "§fLevel: §e%d §7| §fEarned: §e%d §7| §fSpent: §e%d §7| §fAvailable: §a%d §7| §fResetPoints: §e%d", lvl, earned, spent, avail, resetPts)));

         for(NpcAttribute a : NpcAttribute.values()) {
            int v = st.getAllocated(a);
            String var10001 = a.name();
            ctx.sendMessage(Message.raw("§f" + var10001 + "§7: §e" + v));
         }

         ctx.sendMessage(Message.raw("§7Use: §f/amigo stats add <atributo> [qtd]"));
         ctx.sendMessage(Message.raw("§7Reset: §f/amigo stats reset §7(consome 1 ResetPoint; admin bypass)"));
      }
   }

   private static boolean isAdmin(CommandContext ctx) {
      try {
         CommandSender sender = ctx != null ? ctx.sender() : null;
         if (sender == null) {
            return false;
         } else {
            String opNode = HytalePermissions.fromCommand("op");
            return sender.hasPermission(opNode, false) || sender.hasPermission("hytale.command.op", false) || sender.hasPermission("hytale.*", false) || sender.hasPermission("*", false) || sender.hasPermission("OP") || sender.hasPermission("op") || sender.hasPermission("operator") || sender.hasPermission("admin");
         }
      } catch (Throwable var12) {
         try {
            CommandSender sender = ctx != null ? ctx.sender() : null;
            if (sender == null) {
               return false;
            }

            for(String m : new String[]{"isOp", "isOperator", "isAdmin"}) {
               try {
                  Method mm = sender.getClass().getMethod(m);
                  Object r = mm.invoke(sender);
                  if (r instanceof Boolean b) {
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

   private static ParsedArgs parseFallback(CommandContext ctx) {
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

         for(int i = 0; i < t.length; ++i) {
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
               return new ParsedArgs(action, attr, amt);
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

            return new ParsedArgs(action, attr, amt);
         }
      } catch (Throwable var10) {
      }

      return null;
   }

   private static record ParsedArgs(String action, String attr, Integer amount) {
   }
}
