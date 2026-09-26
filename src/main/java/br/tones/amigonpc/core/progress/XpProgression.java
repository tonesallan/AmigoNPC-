package br.tones.amigonpc.core.progress;

public final class XpProgression {
   public static final int MAX_LEVEL = 100;
   private static final double LEVEL_BASE_XP = 150.0;
   private static final double LEVEL_OFFSET = 0.0;
   private static final double LEVEL_POWER = 2.5;

   private XpProgression() {
   }

   public static void init() {
   }

   public static double xpRequiredForLevelD(int level) {
      int L = Math.max(1, level);
      return L <= 1 ? 0.0 : 150.0 * Math.pow(L, 2.5) + 0.0;
   }

   public static double xpNeededForNextLevelD(int level) {
      int L = Math.max(1, level);
      if (L >= 100) {
         return 0.0;
      }

      double a = xpRequiredForLevelD(L);
      double b = xpRequiredForLevelD(L + 1);
      return b - a;
   }

   public static long xpToNext(int level) {
      int L = Math.max(1, level);
      double d = xpNeededForNextLevelD(L);
      return !(d > 0.0) ? 0L : (long)Math.floor(d);
   }

   public static long xpStartOfLevel(int level) {
      int L = Math.max(1, level);
      double d = xpRequiredForLevelD(L);
      return d >= 0.0 && !Double.isNaN(d) && !Double.isInfinite(d) ? (long)Math.floor(d) : 0L;
   }

   public static int levelFromTotalXp(long totalXp) {
      long xpL = Math.max(0L, totalXp);
      double xp = xpL;
      int lo = 1;
      int hi = 100;

      while (lo < hi) {
         int mid = lo + hi + 1 >>> 1;
         double req = xpRequiredForLevelD(mid);
         if (req <= xp) {
            lo = mid;
         } else {
            hi = mid - 1;
         }
      }

      return lo;
   }

   public static long xpIntoLevel(long totalXp) {
      long xp = Math.max(0L, totalXp);
      int level = levelFromTotalXp(xp);
      long minThisLevel = xpStartOfLevel(level);
      return Math.max(0L, xp - minThisLevel);
   }

   public static long xpNeededThisLevel(long totalXp) {
      long xp = Math.max(0L, totalXp);
      int level = levelFromTotalXp(xp);
      return xpToNext(level);
   }

   public static double deathRateForLevel(int level) {
      int L = Math.max(1, level);
      if (L <= 10) {
         return 0.0;
      } else if (L <= 50) {
         return 0.002;
      } else if (L <= 90) {
         return 0.004;
      } else {
         return L <= 500 ? 0.008 : 0.01;
      }
   }

   public static long applyDeathPenalty(long totalXp) {
      long xp = Math.max(0L, totalXp);
      int level = levelFromTotalXp(xp);
      double rate = deathRateForLevel(level);
      long rawLoss = (long)Math.floor(xp * rate);
      if (rawLoss <= 0L) {
         return xp;
      }

      long minThisLevel = xpStartOfLevel(level);
      if (minThisLevel == Long.MAX_VALUE) {
         return xp;
      }

      long xpIntoLevel = Math.max(0L, xp - minThisLevel);
      long loss = Math.min(rawLoss, xpIntoLevel);
      long out = xp - loss;
      return Math.max(0L, out);
   }
}
