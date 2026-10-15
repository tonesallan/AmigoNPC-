package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfig;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfigService;
import br.tones.amigonpc.core.rewards.RewardsService;
import br.tones.amigonpc.core.rewards.RewardsState;
import br.tones.amigonpc.core.rewards.RewardsStateService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class AmigoRewardsSubCommand extends AbstractPlayerCommand {
   private final OptionalArg actionArg;
   private final OptionalArg levelArg;
   private final OptionalArg playerNameArg;
   private final OptionalArg amountArg;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoRewardsSubCommand() {
      super("rewards", "Gerencia Level Rewards do AmigoNPC");
      this.actionArg = this.withOptionalArg("action", "claim|claimall|setreset|reload|defaults", ArgTypes.STRING);
      this.levelArg = this.withOptionalArg("level", "Level do reward", ArgTypes.INTEGER);
      this.playerNameArg = this.withOptionalArg("player", "Player (admin)", ArgTypes.STRING);
      this.amountArg = this.withOptionalArg("amount", "Amount (admin)", ArgTypes.INTEGER);
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store store, Ref playerEntityRef, PlayerRef playerRef, World world) {
      String action = this.actionArg.provided(ctx) ? String.valueOf(this.actionArg.get(ctx)) : "list";
      action = action == null ? "list" : action.trim().toLowerCase(Locale.ROOT);
      UUID ownerId = playerRef.getUuid();
      String ownerName = bestEffortPlayerName(ctx.sender(), ownerId);
      int lvl = AmigoNpcManager.getShared().getNpcLevel(ownerId);
      if (action.equals("reload")) {
         if (!isOp(ctx.sender())) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas OP/Admin."));
         } else if (action.equals("defaults")) {
            if (!isOp(ctx.sender())) {
               ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas OP/Admin."));
            } else {
               AmigoLevelRewardsConfigService.getShared().restoreDefaults(true);
               ctx.sendMessage(Message.raw("§a[AmigoNPC] Defaults de rewards restaurados (backup .bak criado)."));
            }
         } else {
            AmigoLevelRewardsConfigService.getShared().reload();
            ctx.sendMessage(Message.raw("§a[AmigoNPC] Rewards config recarregada."));
         }
      } else if (action.equals("setreset")) {
         if (!isOp(ctx.sender())) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas OP/Admin."));
         } else if (this.playerNameArg.provided(ctx) && this.amountArg.provided(ctx)) {
            String targetName = String.valueOf(this.playerNameArg.get(ctx));
            int amount = (Integer)this.amountArg.get(ctx);
            if (amount < 0) {
               amount = 0;
            }

            if (targetName != null && targetName.equalsIgnoreCase(ownerName)) {
               RewardsState st = RewardsStateService.getShared().load(ownerId);
               st.resetPoints = amount;
               RewardsStateService.getShared().save(ownerId, st);
               ctx.sendMessage(Message.raw("§e[AmigoNPC] resetPoints = " + amount));
            } else {
               ctx.sendMessage(Message.raw("§c[AmigoNPC] Nesta versão, setreset só suporta o player atual."));
            }
         } else {
            ctx.sendMessage(Message.raw("§cUso: /amigo rewards setreset <player> <amount>"));
         }
      } else if (action.equals("claim")) {
         if (!this.levelArg.provided(ctx)) {
            ctx.sendMessage(Message.raw("§cUso: /amigo rewards claim <level>"));
         } else {
            int rewardLevel = (Integer)this.levelArg.get(ctx);
            RewardsService.ClaimResult res = RewardsService.getShared().claim(ownerId, ownerName, lvl, rewardLevel);
            if (!res.ok()) {
               ctx.sendMessage(Message.raw("§c[AmigoNPC] Não foi possível claimar: " + res.error()));
            } else {
               ctx.sendMessage(Message.raw("§a[AmigoNPC] Reward claimado (Level " + rewardLevel + ")."));
            }
         }
      } else if (action.equals("claimall")) {
         RewardsService.ClaimAllResult res = RewardsService.getShared().claimAll(ownerId, ownerName, lvl);
         if (res.applied() <= 0) {
            ctx.sendMessage(Message.raw("§e[AmigoNPC] Nenhum reward elegível."));
         } else {
            int var10001 = res.applied();
            ctx.sendMessage(Message.raw("§a[AmigoNPC] ClaimAll: " + var10001 + " aplicado(s), " + res.skipped() + " ignorado(s)."));
         }
      } else {
         AmigoLevelRewardsConfig cfg = AmigoLevelRewardsConfigService.getShared().get();
         RewardsState st = RewardsStateService.getShared().load(ownerId);
         int resetPoints = st.resetPoints;
         ctx.sendMessage(Message.raw("§7--- §fAmigoNPC Rewards §7---"));
         ctx.sendMessage(Message.raw("§fNPC Level: §e" + lvl + " §7| §fResetPoints: §e" + resetPoints));
         if (cfg.Rewards != null && !cfg.Rewards.isEmpty()) {
            for(AmigoLevelRewardsConfig.RewardEntry r : cfg.Rewards) {
               if (r != null && r.Level > 0) {
                  boolean unlocked = lvl >= r.Level;
                  boolean claimed = st.claimedRewardLevels.contains(r.Level);
                  String status = claimed ? "§aClaimed" : (unlocked ? "§eUnclaim" : "§8Locked");
                  String extra = r.CommandTitle != null && !r.CommandTitle.isBlank() ? r.CommandTitle : (r.Command != null ? r.Command : "");
                  if (extra == null) {
                     extra = "";
                  }

                  if (extra.length() > 40) {
                     extra = extra.substring(0, 40) + "...";
                  }

                  ctx.sendMessage(Message.raw("§fLvl " + r.Level + "§7: " + status + " §7| reset=" + r.ResetPoints + " §7| " + extra));
               }
            }

            ctx.sendMessage(Message.raw("§7Use: §f/amigo rewards claim <level> §7| §f/amigo rewards claimall"));
            ctx.sendMessage(Message.raw("§7Admin: §f/amigo rewards reload §7| §f/amigo rewards setreset <player> <amount>"));
         } else {
            ctx.sendMessage(Message.raw("§7Nenhum reward configurado."));
         }
      }
   }

   private static boolean isOp(Object sender) {
      if (sender == null) {
         return false;
      } else {
         for(String m : new String[]{"isOp", "isOperator", "isAdmin"}) {
            try {
               Method mm = sender.getClass().getMethod(m);
               Object r = mm.invoke(sender);
               if (r instanceof Boolean) {
                  Boolean b = (Boolean)r;
                  return b;
               }
            } catch (Throwable var8) {
            }
         }

         return false;
      }
   }

   private static String bestEffortPlayerName(Object sender, UUID fallbackId) {
      if (sender != null) {
         for(String m : new String[]{"getName", "getUsername", "getUserName"}) {
            try {
               Method mm = sender.getClass().getMethod(m);
               Object r = mm.invoke(sender);
               if (r != null) {
                  String s = String.valueOf(r);
                  if (!s.isBlank()) {
                     return s;
                  }
               }
            } catch (Throwable var9) {
            }
         }
      }

      return fallbackId != null ? fallbackId.toString() : "player";
   }
}
