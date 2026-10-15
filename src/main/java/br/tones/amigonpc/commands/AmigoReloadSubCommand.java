package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.autoloot.AutoLootConfigService;
import br.tones.amigonpc.core.npcstats.NpcStatsConfigService;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksConfigService;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksService;
import br.tones.amigonpc.core.rewards.AmigoLevelRewardsConfigService;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingConfigService;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingService;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfigService;
import br.tones.amigonpc.core.zones.AmigoZonesConfigService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoReloadSubCommand extends AbstractPlayerCommand {
   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoReloadSubCommand() {
      super("reload", "Recarrega configs do AmigoNPC (namespaced)");
      this.setAllowsExtraArguments(true);
   }

   protected void execute(CommandContext ctx, Store store, Ref playerEntityRef, PlayerRef playerRef, World world) {
      if (!isAdmin(ctx)) {
         ctx.sendMessage(Message.raw("§c[AmigoNPC] Apenas OP."));
      } else {
         try {
            AmigoZonesConfigService.reload();
         } catch (Throwable var15) {
         }

         try {
            NpcStatsConfigService.getShared().reloadNow();
         } catch (Throwable var14) {
         }

         try {
            AmigoLevelRewardsConfigService.getShared().reload();
         } catch (Throwable var13) {
         }

         try {
            NpcXpSourcesConfigService.reloadNow();
         } catch (Throwable var12) {
         }

         try {
            PlayerStatTweaksConfigService.reloadNow();
         } catch (Throwable var11) {
         }

         try {
            WorldMobScalingConfigService.reloadNow();
         } catch (Throwable var10) {
         }

         try {
            AutoLootConfigService.reloadNow();
         } catch (Throwable var9) {
         }

         try {
            WorldMobScalingService.getShared().clearCache();
         } catch (Throwable var8) {
         }

         try {
            Player p = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
            if (p != null) {
               PlayerStatTweaksService.getShared().applyOrRemove(p, playerRef.getUuid(), true);
            }
         } catch (Throwable var7) {
         }

         ctx.sendMessage(Message.raw("§a[AmigoNPC] Reload OK."));
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
      } catch (Throwable var3) {
         return false;
      }
   }
}
