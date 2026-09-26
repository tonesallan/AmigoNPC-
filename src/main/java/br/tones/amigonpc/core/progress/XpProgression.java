package br.tones.amigonpc.core.progress;

public final class XpProgression {
   private static final double LEVEL_BASE_XP = 150.0;
   private static final double LEVEL_OFFSET = 0.0;
   private static final double LEVEL_POWER = 2.5;

   private XpProgression() {
   }

   public static void init() {
   }

   public static double xpRequiredForLevelD(int level) {
      int safeLevel = Math.max(1, level);
      return safeLevel <= 1 ? 0.0 : LEVEL_BASE_XP * Math.pow((double)safeLevel, LEVEL_POWER) + LEVEL_OFFSET;
   }

   public static double xpNeededForNextLevelD(int level) {
      int safeLevel = Math.max(1, level);
      if (safeLevel == Integer.MAX_VALUE) {
         return Double.POSITIVE_INFINITY;
      }

      return xpRequiredForLevelD(safeLevel + 1) - xpRequiredForLevelD(safeLevel);
   }

   public static long xpToNext(int level) {
      double value = xpNeededForNextLevelD(level);
      if (!(value > 0.0) || Double.isNaN(value)) {
         return 0L;
      }

      if (Double.isInfinite(value) || value >= Long.MAX_VALUE) {
         return Long.MAX_VALUE;
      }

      return Math.max(1L, (long)Math.floor(value));
   }

   public static long xpStartOfLevel(int level) {
      double value = xpRequiredForLevelD(level);
      if (Double.isNaN(value) || value < 0.0) {
         return 0L;
      }

      if (Double.isInfinite(value) || value >= Long.MAX_VALUE) {
         return Long.MAX_VALUE;
      }

      return (long)Math.floor(value);
   }

   public static int levelFromTotalXp(long totalXp) {
      long xp = Math.max(0L, totalXp);
      if (xp == 0L) {
         return 1;
      }

      int low = 1;
      int high = 2;
      while (high < Integer.MAX_VALUE && xpStartOfLevel(high) <= xp) {
         low = high;
         if (high > Integer.MAX_VALUE / 2) {
            high = Integer.MAX_VALUE;
            break;
         }

         high *= 2;
      }

      while (low < high) {
         int mid = low + (high - low + 1) / 2;
         long required = xpStartOfLevel(mid);
         if (required != Long.MAX_VALUE && required <= xp) {
            low = mid;
         } else {
            high = mid - 1;
         }
      }

      return Math.max(1, low);
   }

   public static long xpIntoLevel(long totalXp) {
      long xp = Math.max(0L, totalXp);
      long start = xpStartOfLevel(levelFromTotalXp(xp));
      return start == Long.MAX_VALUE ? 0L : Math.max(0L, xp - start);
   }

   public static long xpNeededThisLevel(long totalXp) {
      return xpToNext(levelFromTotalXp(Math.max(0L, totalXp)));
   }

   public static long applyCurrentLevelPenalty(long totalXp, double rate) {
      long xp = Math.max(0L, totalXp);
      double safeRate = Math.max(0.0, Math.min(1.0, rate));
      if (xp == 0L || safeRate <= 0.0) {
         return xp;
      }

      int level = levelFromTotalXp(xp);
      long start = xpStartOfLevel(level);
      if (start == Long.MAX_VALUE) {
         return xp;
      }

      long intoLevel = Math.max(0L, xp - start);
      long loss = (long)Math.floor(intoLevel * safeRate);
      return loss <= 0L ? xp : Math.max(start, xp - loss);
   }

   public static long applyDeathPenalty(long totalXp) {
      return applyCurrentLevelPenalty(totalXp, 0.40);
   }
}
