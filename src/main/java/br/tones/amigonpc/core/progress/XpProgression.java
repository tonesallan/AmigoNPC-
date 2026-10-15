package br.tones.amigonpc.core.progress;

public final class XpProgression {
   public static final int MAX_LEVEL = 100;
   private static final double LEVEL_BASE_XP = (double)150.0F;
   private static final double LEVEL_OFFSET = (double)0.0F;
   private static final double LEVEL_POWER = (double)2.5F;

   private XpProgression() {
   }

   public static void init() {
   }

   public static double xpRequiredForLevelD(int level) {
      int L = Math.max(1, level);
      return L <= 1 ? (double)0.0F : (double)150.0F * Math.pow((double)L, (double)2.5F) + (double)0.0F;
   }

   public static double xpNeededForNextLevelD(int level) {
      int L = Math.max(1, level);
      if (L >= 100) {
         return (double)0.0F;
      } else {
         double a = xpRequiredForLevelD(L);
         double b = xpRequiredForLevelD(L + 1);
         return b - a;
      }
   }

   public static long xpToNext(int level) {
      int L = Math.max(1, level);
      double d = xpNeededForNextLevelD(L);
      return !(d > (double)0.0F) ? 0L : (long)Math.floor(d);
   }

   public static long xpStartOfLevel(int level) {
      int L = Math.max(1, level);
      double d = xpRequiredForLevelD(L);
      return d >= (double)0.0F && !Double.isNaN(d) && !Double.isInfinite(d) ? (long)Math.floor(d) : 0L;
   }

   public static int levelFromTotalXp(long totalXp) {
      long xpL = Math.max(0L, totalXp);
      double xp = (double)xpL;
      int lo = 1;
      int hi = 100;

      while(lo < hi) {
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
         return (double)0.0F;
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
      long rawLoss = (long)Math.floor((double)xp * rate);
      if (rawLoss <= 0L) {
         return xp;
      } else {
         long minThisLevel = xpStartOfLevel(level);
         if (minThisLevel == Long.MAX_VALUE) {
            return xp;
         } else {
            long xpIntoLevel = Math.max(0L, xp - minThisLevel);
            long loss = Math.min(rawLoss, xpIntoLevel);
            long out = xp - loss;
            return Math.max(0L, out);
         }
      }
   }
}
