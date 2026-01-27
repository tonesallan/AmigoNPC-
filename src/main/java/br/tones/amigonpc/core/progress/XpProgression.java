package br.tones.amigonpc.core.progress;

/**
 * Progressão de XP acumulativa (totalXp) com level calculado.
 *
 * Preset NORMAL:
 * XP_next(L) = 80 + 14*(L-1) + 1.2*(L-1)^2
 *
 * Tabelas pré-calculadas (init 1x):
 * - xpToNext[level] = XP necessária para ir do level ao próximo
 * - xpStart[level]  = XP total mínima para estar no início do level (piso)
 */
public final class XpProgression {

    public static final int MAX_LEVEL = 10_000;

    private static volatile boolean INITED = false;
    private static long[] XP_TO_NEXT; // 1..MAX_LEVEL
    private static long[] XP_START;   // 1..MAX_LEVEL+1 (início do level)

    private XpProgression() {}

    /** Pré-calcula tabelas uma vez. Pode ser chamado no onEnable; é seguro chamar várias vezes. */
    public static void init() {
        if (INITED) return;
        synchronized (XpProgression.class) {
            if (INITED) return;

            XP_TO_NEXT = new long[MAX_LEVEL + 2];
            XP_START = new long[MAX_LEVEL + 2];

            XP_START[1] = 0L;
            for (int level = 1; level <= MAX_LEVEL; level++) {
                long need = xpNextLevel(level);
                XP_TO_NEXT[level] = need;
                XP_START[level + 1] = safeAdd(XP_START[level], need);
            }

            INITED = true;
        }
    }

    /** XP necessária para ir do level L para L+1 (Preset NORMAL). */
    public static long xpNextLevel(int level) {
        int L = Math.max(1, level);
        long x = L - 1L;
        double d = 80.0 + 14.0 * x + 1.2 * (double) (x * x);
        long v = Math.round(d);
        return Math.max(1L, v);
    }

    /** XP necessária para sair do level informado. */
    public static long xpToNext(int level) {
        init();
        int L = Math.max(1, level);
        if (L <= MAX_LEVEL) return XP_TO_NEXT[L];
        return xpNextLevel(L);
    }

    /** XP total mínima para estar no INÍCIO do level (piso). */
    public static long xpStartOfLevel(int level) {
        init();
        int L = Math.max(1, level);

        if (L <= MAX_LEVEL + 1) return XP_START[L];

        // Fallback incremental acima do limite da tabela (best-effort)
        long start = XP_START[MAX_LEVEL + 1];
        for (int cur = MAX_LEVEL + 1; cur < L; cur++) {
            start = safeAdd(start, xpNextLevel(cur));
            if (start == Long.MAX_VALUE) return Long.MAX_VALUE;
        }
        return start;
    }

    /**
     * Calcula level a partir do totalXp usando busca binária na xpStart.
     * Retorna o maior level tal que xpStart[level] <= totalXp.
     */
    public static int levelFromTotalXp(long totalXp) {
        init();
        long xp = Math.max(0L, totalXp);

        // Dentro da tabela (até o início do level MAX_LEVEL+1)
        if (xp < XP_START[MAX_LEVEL + 1]) {
            int lo = 1;
            int hi = MAX_LEVEL;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (XP_START[mid] <= xp) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            return lo;
        }

        // Fallback acima do limite: caminha nível a nível a partir de MAX_LEVEL+1
        int level = MAX_LEVEL + 1;
        long start = XP_START[MAX_LEVEL + 1];
        long remaining = xp - start;

        int guard = 0;
        while (guard++ < 200_000) {
            long need = xpNextLevel(level);
            if (remaining < need) return level;
            remaining -= need;
            level++;
        }
        return level; // safety
    }

    public static long xpIntoLevel(long totalXp) {
        long xp = Math.max(0L, totalXp);
        int level = levelFromTotalXp(xp);
        long minThisLevel = xpStartOfLevel(level);
        if (minThisLevel == Long.MAX_VALUE) return 0L;
        return Math.max(0L, xp - minThisLevel);
    }

    public static long xpNeededThisLevel(long totalXp) {
        long xp = Math.max(0L, totalXp);
        int level = levelFromTotalXp(xp);
        return xpToNext(level);
    }

    /** Faixas de perda por morte (sem overlap). */
    public static double deathRateForLevel(int level) {
        int L = Math.max(1, level);
        if (L <= 10) return 0.0;
        if (L <= 50) return 0.002;
        if (L <= 90) return 0.004;
        if (L <= 500) return 0.008;
        return 0.01;
    }

    /**
     * Aplica punição por morte ao totalXp, mas NÃO permite deslevelar:
     * a perda é limitada ao progresso dentro do nível atual.
     */
    public static long applyDeathPenalty(long totalXp) {
        long xp = Math.max(0L, totalXp);
        int level = levelFromTotalXp(xp);
        double rate = deathRateForLevel(level);

        long rawLoss = (long) Math.floor(xp * rate);
        if (rawLoss <= 0) return xp;

        long minThisLevel = xpStartOfLevel(level);
        if (minThisLevel == Long.MAX_VALUE) return xp;

        long xpIntoLevel = Math.max(0L, xp - minThisLevel);
        long loss = Math.min(rawLoss, xpIntoLevel);

        long out = xp - loss;
        return Math.max(0L, out);
    }

    private static long safeAdd(long a, long b) {
        long r = a + b;
        // overflow detection: if a and b have same sign and r has different sign
        if (((a ^ r) & (b ^ r)) < 0) return Long.MAX_VALUE;
        return r;
    }
}
