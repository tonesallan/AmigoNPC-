package br.tones.amigonpc.core.progress;

public final class StatScaling {
   private StatScaling() {
   }

   public static int effectiveLevel(int level) {
      if (level < 1) {
         return 1;
      } else {
         return level > 100 ? 100 : level;
      }
   }

   public static double multiplier(int level) {
      int e = effectiveLevel(level);
      return (double)1.0F + (double)9.0F * (double)(e - 1) / (double)99.0F;
   }

   public static long scaledHp(long baseHp, int level) {
      long base = Math.max(1L, baseHp);
      long out = Math.round((double)base * multiplier(level));
      return Math.max(1L, out);
   }

   public static long scaledDef(long baseDef, int level) {
      long base = Math.max(0L, baseDef);
      long out = Math.round((double)base * multiplier(level));
      return Math.max(0L, out);
   }
}
