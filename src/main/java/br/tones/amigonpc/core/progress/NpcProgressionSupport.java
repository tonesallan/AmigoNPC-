package br.tones.amigonpc.core.progress;

import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.api.events.NpcExperienceGainedEvent;
import br.tones.amigonpc.api.events.NpcLevelUpEvent;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.events.AmigoEventBus;
import java.util.UUID;

public final class NpcProgressionSupport {
   private NpcProgressionSupport() {
   }

   public static NpcLevelProgressSnapshot emptySnapshot() {
      return new NpcLevelProgressSnapshot(1, 0L, 0L);
   }

   public static long sanitizeTotalXp(long totalXp) {
      return Math.max(0L, totalXp);
   }

   public static int resolveLevel(long totalXp) {
      return Math.max(1, XpProgression.levelFromTotalXp(sanitizeTotalXp(totalXp)));
   }

   public static NpcLevelProgressSnapshot buildSnapshot(long totalXp) {
      XpProgression.init();
      long safeTotalXp = sanitizeTotalXp(totalXp);
      int level = resolveLevel(safeTotalXp);
      long start = XpProgression.xpStartOfLevel(level);
      long need = Math.max(0L, XpProgression.xpToNext(level));
      long into = start == Long.MAX_VALUE ? 0L : Math.max(0L, safeTotalXp - start);
      if (need > 0L && into > need) {
         into = need;
      }

      return new NpcLevelProgressSnapshot(level, into, need);
   }

   public static boolean isRecentlyInCombat(boolean wasInCombat, long lastCombatTagMillis, long nowMillis) {
      return wasInCombat ? true : lastCombatTagMillis > 0L && nowMillis - lastCombatTagMillis < 5000L;
   }

   public static boolean awardOfflineXp(UUID ownerId, long amount, NpcXpSource source, NpcXpContext ctx) {
      if (ownerId != null && amount > 0L) {
         try {
            XpProgression.init();
            long before = sanitizeTotalXp(AmigoPersistence.loadTotalXp(ownerId));
            long after = before + amount;
            if (after < before) {
               after = Long.MAX_VALUE;
            }

            AmigoPersistence.saveTotalXp(ownerId, after);
            int oldLevel = resolveLevel(before);
            int newLevel = resolveLevel(after);
            NpcXpSource src = source != null ? source : NpcXpSource.COMMAND;
            NpcXpContext c = ctx != null ? ctx : NpcXpContext.now();
            AmigoEventBus.post(new NpcExperienceGainedEvent(ownerId, amount, before, after, oldLevel, newLevel, src, c));
            if (newLevel > oldLevel) {
               AmigoEventBus.post(new NpcLevelUpEvent(ownerId, oldLevel, newLevel, after, src, c));
            }

            return true;
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static float mitigateIncomingDamage(UUID ownerId, long totalXp, float incomingAmount) {
      // Survival scaling is represented by the NPC's real HP/defense stats.
      // Those values are capped against the owner when scaling is applied, so
      // no hidden level multiplier may make the companion tougher than its player.
      return Math.max(0.0F, incomingAmount);
   }
}
