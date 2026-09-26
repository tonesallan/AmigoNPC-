package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.i18n.AmigoText;
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
   private final OptionalArg actionArg = this.withOptionalArg("action", AmigoText.text("cmd.arg.amigo.rewards.action"), ArgTypes.STRING);
   private final OptionalArg levelArg = this.withOptionalArg("level", AmigoText.text("cmd.arg.amigo.rewards.level"), ArgTypes.INTEGER);
   private final OptionalArg playerNameArg = this.withOptionalArg("player", AmigoText.text("cmd.arg.amigo.rewards.player"), ArgTypes.STRING);
   private final OptionalArg amountArg = this.withOptionalArg("amount", AmigoText.text("cmd.arg.amigo.rewards.amount"), ArgTypes.INTEGER);

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoRewardsSubCommand() {
      super("rewards", AmigoText.text("cmd.desc.amigo.rewards"));
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
      String action = this.actionArg.provided(ctx) ? String.valueOf(this.actionArg.get(ctx)) : "list";
      action = action == null ? "list" : action.trim().toLowerCase(Locale.ROOT);
      UUID ownerId = playerRef.getUuid();
      String ownerName = bestEffortPlayerName(ctx.sender(), ownerId);
      int lvl = AmigoNpcManager.getShared().getNpcLevel(ownerId);
      if (action.equals("reload")) {
         if (!isOp(ctx.sender())) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.only_admin")));
         } else if (action.equals("defaults")) {
            if (!isOp(ctx.sender())) {
               ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.only_admin")));
            } else {
               AmigoLevelRewardsConfigService.getShared().restoreDefaults(true);
               ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.defaults_restored")));
            }
         } else {
            AmigoLevelRewardsConfigService.getShared().reload();
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.reloaded")));
         }
      } else if (action.equals("setreset")) {
         if (!isOp(ctx.sender())) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.only_admin")));
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
               ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.rewards.setreset.value", amount)));
            } else {
               ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.setreset.current_only")));
            }
         } else {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.setreset.usage")));
         }
      } else if (action.equals("claim")) {
         if (!this.levelArg.provided(ctx)) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.claim.usage")));
         } else {
            int rewardLevel = (Integer)this.levelArg.get(ctx);
            RewardsService.ClaimResult res = RewardsService.getShared().claim(ownerId, ownerName, lvl, rewardLevel);
            if (!res.ok()) {
               ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.rewards.claim.failed", res.error())));
            } else {
               ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.rewards.claim.success", rewardLevel)));
            }
         }
      } else if (action.equals("claimall")) {
         RewardsService.ClaimAllResult res = RewardsService.getShared().claimAll(ownerId, ownerName, lvl);
         if (res.applied() <= 0) {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.claimall.none")));
         } else {
            ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.rewards.claimall.result", res.applied(), res.skipped())));
         }
      } else {
         AmigoLevelRewardsConfig cfg = AmigoLevelRewardsConfigService.getShared().get();
         RewardsState st = RewardsStateService.getShared().load(ownerId);
         int resetPoints = st.resetPoints;
         ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.list.header")));
         ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.rewards.list.summary", lvl, resetPoints)));
         if (cfg.Rewards != null && !cfg.Rewards.isEmpty()) {
            for (AmigoLevelRewardsConfig.RewardEntry r : cfg.Rewards) {
               if (r != null && r.Level > 0) {
                  boolean unlocked = lvl >= r.Level;
                  boolean claimed = st.claimedRewardLevels.contains(r.Level);
                  String status = claimed
                     ? AmigoText.text("cmd.amigo.rewards.status.claimed")
                     : (unlocked ? AmigoText.text("cmd.amigo.rewards.status.unclaim") : AmigoText.text("cmd.amigo.rewards.status.locked"));
                  String extra = r.CommandTitle != null && !r.CommandTitle.isBlank() ? r.CommandTitle : (r.Command != null ? r.Command : "");
                  if (extra == null) {
                     extra = "";
                  }

                  if (extra.length() > 40) {
                     extra = extra.substring(0, 40) + "...";
                  }

                  ctx.sendMessage(Message.raw(AmigoText.format("cmd.amigo.rewards.list.row", r.Level, status, r.ResetPoints, extra)));
               }
            }

            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.list.usage")));
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.list.admin_usage")));
         } else {
            ctx.sendMessage(Message.raw(AmigoText.text("cmd.amigo.rewards.list.empty")));
         }
      }
   }

   private static boolean isOp(Object sender) {
      if (sender == null) {
         return false;
      }

      for (String m : new String[]{"isOp", "isOperator", "isAdmin"}) {
         try {
            Method mm = sender.getClass().getMethod(m);
            if (mm.invoke(sender) instanceof Boolean b) {
               return b;
            }
         } catch (Throwable var8) {
         }
      }

      return false;
   }

   private static String bestEffortPlayerName(Object sender, UUID fallbackId) {
      if (sender != null) {
         for (String m : new String[]{"getName", "getUsername", "getUserName"}) {
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
