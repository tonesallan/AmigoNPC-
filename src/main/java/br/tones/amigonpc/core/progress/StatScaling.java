package br.tones.amigonpc.core.progress;

/**
 * Scaling de HP e DEF do NPC até o level 100.
 *
 * - effectiveLevel = clamp(level, 1..100)
 * - multiplicador: lvl 1 => 1.0, lvl 100 => 10.0
 *   mult = 1.0 + 9.0 * (effectiveLevel - 1) / 99.0
 */
public final class StatScaling {

    private StatScaling() {}

    public static int effectiveLevel(int level) {
        if (level < 1) return 1;
        if (level > 100) return 100;
        return level;
    }

    public static double multiplier(int level) {
        int e = effectiveLevel(level);
        return 1.0 + 9.0 * (double) (e - 1) / 99.0;
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
