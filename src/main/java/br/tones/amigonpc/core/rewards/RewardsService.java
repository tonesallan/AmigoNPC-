package br.tones.amigonpc.core.rewards;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.progress.XpProgression;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class RewardsService {
   private static final RewardsService SHARED = new RewardsService();

   public static RewardsService getShared() {
      return SHARED;
   }

   private RewardsService() {
   }

   public List<AmigoLevelRewardsConfig.RewardEntry> getEligibleUnlocked(UUID ownerId, int level, RewardsState state) {
      AmigoLevelRewardsConfig cfg = AmigoLevelRewardsConfigService.getShared().get();
      if (cfg.Rewards != null && !cfg.Rewards.isEmpty()) {
         if (state == null) {
            state = RewardsStateService.getShared().load(ownerId);
         }

         List<AmigoLevelRewardsConfig.RewardEntry> out = new ArrayList<>();

         for (AmigoLevelRewardsConfig.RewardEntry r : cfg.Rewards) {
            if (r != null && r.Level > 0 && level >= r.Level && !state.claimedRewardLevels.contains(r.Level)) {
               out.add(r);
            }
         }

         out.sort(Comparator.comparingInt(a -> a.Level));
         return out;
      } else {
         return List.of();
      }
   }

   public boolean isClaimed(UUID ownerId, int rewardLevel) {
      RewardsState st = RewardsStateService.getShared().load(ownerId);
      return st.claimedRewardLevels.contains(rewardLevel);
   }

   public RewardsService.ClaimResult claim(UUID ownerId, String ownerName, int currentLevel, int rewardLevel) {
      AmigoLevelRewardsConfig cfg = AmigoLevelRewardsConfigService.getShared().get();
      AmigoLevelRewardsConfig.RewardEntry entry = findReward(cfg, rewardLevel);
      if (entry == null) {
         return RewardsService.ClaimResult.error("not_found");
      }

      RewardsState st = RewardsStateService.getShared().load(ownerId);
      if (st.claimedRewardLevels.contains(entry.Level)) {
         return RewardsService.ClaimResult.error("already_claimed");
      }

      if (currentLevel < entry.Level) {
         return RewardsService.ClaimResult.error("locked");
      }

      boolean ok = applyReward(ownerId, ownerName, entry, st);
      return ok ? RewardsService.ClaimResult.success() : RewardsService.ClaimResult.error("failed");
   }

   public RewardsService.ClaimAllResult claimAll(UUID ownerId, String ownerName, int currentLevel) {
      RewardsState st = RewardsStateService.getShared().load(ownerId);
      List<AmigoLevelRewardsConfig.RewardEntry> eligible = this.getEligibleUnlocked(ownerId, currentLevel, st);
      if (eligible.isEmpty()) {
         return new RewardsService.ClaimAllResult(0, 0);
      }

      int applied = 0;
      int skipped = 0;

      for (AmigoLevelRewardsConfig.RewardEntry r : eligible) {
         boolean ok = applyReward(ownerId, ownerName, r, st);
         if (ok) {
            applied++;
         } else {
            skipped++;
         }
      }

      return new RewardsService.ClaimAllResult(applied, skipped);
   }

   private static AmigoLevelRewardsConfig.RewardEntry findReward(AmigoLevelRewardsConfig cfg, int level) {
      if (cfg != null && cfg.Rewards != null) {
         for (AmigoLevelRewardsConfig.RewardEntry r : cfg.Rewards) {
            if (r != null && r.Level == level) {
               return r;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean applyReward(UUID ownerId, String ownerName, AmigoLevelRewardsConfig.RewardEntry r, RewardsState st) {
      if (ownerId != null && r != null && st != null) {
         AmigoLevelRewardsConfig cfg = AmigoLevelRewardsConfigService.getShared().get();
         boolean debug = cfg.DebugRewardsLogging;
         if (r.Items != null && !r.Items.isEmpty()) {
            boolean allOk = grantItems(ownerId, r.Items, debug);
            if (!allOk && debug) {
               System.out.println("[AmigoNPC][Rewards] grantItems teve falhas (alguns itens ignorados ou sem espaço)");
            }
         }

         int before = st.resetPoints;
         if (r.ResetPoints > 0) {
            st.resetPoints = Math.max(0, st.resetPoints + r.ResetPoints);
         }

         if (r.Command != null && !r.Command.isBlank()) {
            String cmd = r.Command;
            if (ownerName != null && !ownerName.isBlank()) {
               cmd = cmd.replace("{player}", ownerName);
            }

            RewardCommandRunner.run(ownerId, ownerName, cmd, debug);
         }

         st.claimedRewardLevels.add(r.Level);
         RewardsStateService.getShared().save(ownerId, st);
         if (debug) {
            System.out
               .println(
                  String.format(Locale.ROOT, "[AmigoNPC][Rewards] claimed level=%d owner=%s resetPoints %d -> %d", r.Level, ownerId, before, st.resetPoints)
               );
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean grantItems(UUID ownerId, List<AmigoLevelRewardsConfig.ItemEntry> items, boolean debug) {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      SimpleItemContainer bag = mgr.getOrLoadBackpack(ownerId);
      boolean okAll = true;
      Iterator var6 = items.iterator();

      while (true) {
         String itemId;
         ItemStack stack;
         while (true) {
            if (!var6.hasNext()) {
               try {
                  AmigoPersistence.saveBackpack(ownerId, bag);
               } catch (Throwable var15) {
               }

               return okAll;
            }

            AmigoLevelRewardsConfig.ItemEntry it = (AmigoLevelRewardsConfig.ItemEntry)var6.next();
            if (it != null) {
               itemId = it.ItemId;
               int qty = it.Quantity;
               if (itemId != null && !itemId.isBlank() && qty > 0) {
                  try {
                     stack = new ItemStack(itemId, qty);
                     if (!stack.isEmpty()) {
                        break;
                     }

                     okAll = false;
                     System.out.println("[AmigoNPC][Rewards] ItemId inválido: " + itemId);
                  } catch (Throwable t) {
                     okAll = false;
                     System.out.println("[AmigoNPC][Rewards] Falha criando ItemStack: " + itemId);
                  }
               }
            }
         }

         try {
            ItemStackTransaction tx = bag.addItemStack(stack);
            ItemStack rem = tx.getRemainder();
            int remQty = 0;

            try {
               if (rem != null) {
                  remQty = rem.getQuantity();
               }
            } catch (Throwable var16) {
            }

            if (rem != null && remQty > 0) {
               okAll = false;
               if (debug) {
                  System.out.println("[AmigoNPC][Rewards] Mochila cheia/sem espaço p/ " + itemId + " rem=" + remQty);
               }
            }
         } catch (Throwable t) {
            okAll = false;
            System.out.println("[AmigoNPC][Rewards] Falha adicionando item na mochila: " + itemId);
         }
      }
   }

   public void adjustClaimedOnLevelDecrease(UUID ownerId, int newLevel) {
      if (ownerId != null) {
         RewardsState st = RewardsStateService.getShared().load(ownerId);
         if (!st.claimedRewardLevels.isEmpty()) {
            st.claimedRewardLevels.removeIf(lvl -> lvl == null || lvl > newLevel);
            RewardsStateService.getShared().save(ownerId, st);
         }
      }
   }

   public static void setNpcLevelInternal(UUID ownerId, int level, boolean debug) {
      int L = Math.max(1, Math.min(100, level));
      long totalXp = XpProgression.xpStartOfLevel(L);
      AmigoPersistence.saveTotalXp(ownerId, totalXp);
      if (debug) {
         System.out.println("[AmigoNPC][Rewards] built-in setlevel owner=" + ownerId + " level=" + L + " totalXp=" + totalXp);
      }

      try {
         AmigoNpcManager.getShared().requestRescale(ownerId);
      } catch (Throwable var7) {
      }

      getShared().adjustClaimedOnLevelDecrease(ownerId, L);
   }

   public record ClaimAllResult(int applied, int skipped) {
   }

   public record ClaimResult(boolean ok, String error) {
      public static RewardsService.ClaimResult success() {
         return new RewardsService.ClaimResult(true, null);
      }

      public static RewardsService.ClaimResult error(String e) {
         return new RewardsService.ClaimResult(false, e);
      }
   }
}
