package br.tones.amigonpc.core.progress;

public final class StatScaling {
   private StatScaling() {
   }

   public static int effectiveLevel(int level) {
      return Math.max(1, level);
   }

   public static double multiplier(int level) {
      int e = effectiveLevel(level);
      return 1.0 + 9.0 * (e - 1) / 99.0;
   }

   public static long scaledHp(long baseHp, int level) {
      long base = Math.max(1L, baseHp);
      long out = Math.round(base * multiplier(level));
      return Math.max(1L, out);
   }

   public static long scaledDef(long baseDef, int level) {
      long base = Math.max(0L, baseDef);
      long out = Math.round(base * multiplier(level));
      return Math.max(0L, out);
   }
}
