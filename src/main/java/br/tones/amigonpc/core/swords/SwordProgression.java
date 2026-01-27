package br.tones.amigonpc.core.swords;

/**
 * Progressão de armas (corpo a corpo) baseada no nível de espadas do NPC.
 *
 * Regras (sem overlap): ao atingir o nível de marco, troca para a próxima arma.
 * Ex.: 9 -> 10 troca de Crude -> Onyxium.
 */
public final class SwordProgression {

    private SwordProgression() {}

    // IDs exatos conforme tooltip do jogo (imagens do usuário)
    public static final String CRUDE_SWORD = "Weapon_Sword_Crude";
    public static final String ONYXIUM_SWORD = "Weapon_Sword_Onyxium";
    public static final String RUSTY_STEEL_SWORD = "Weapon_Sword_Steel_Rusty";
    public static final String THORIUM_SWORD = "Weapon_Sword_Thorium";

    public static final String IRON_LONGSWORD = "Weapon_Longsword_Iron";
    public static final String MITHRIL_SWORD = "Weapon_Sword_Mithril";
    public static final String THORIUM_LONGSWORD = "Weapon_Longsword_Thorium";
    public static final String KATANA = "Weapon_Longsword_Katana";
    public static final String FLAME_LONGSWORD = "Weapon_Longsword_Flame";
    public static final String ONYXIUM_LONGSWORD = "Weapon_Longsword_Onyxium";

    // Marcos de troca (inclusive)
    public static final int M10 = 10;
    public static final int M15 = 15;
    public static final int M20 = 20;
    public static final int M25 = 25;
    public static final int M30 = 30;
    public static final int M35 = 35;
    public static final int M40 = 40;
    public static final int M55 = 55;
    public static final int M75 = 75;

    public static int clampLevel(int lvl) {
        return Math.max(1, lvl);
    }

    /** Retorna o itemId da arma que deve estar equipada no nível informado. */
    public static String weaponIdForLevel(int level) {
        int lvl = clampLevel(level);

        if (lvl >= M75) return ONYXIUM_LONGSWORD;
        if (lvl >= M55) return FLAME_LONGSWORD;
        if (lvl >= M40) return KATANA;
        if (lvl >= M35) return THORIUM_LONGSWORD;
        if (lvl >= M30) return MITHRIL_SWORD;
        if (lvl >= M25) return IRON_LONGSWORD;
        if (lvl >= M20) return THORIUM_SWORD;
        if (lvl >= M15) return RUSTY_STEEL_SWORD;
        if (lvl >= M10) return ONYXIUM_SWORD;

        return CRUDE_SWORD;
    }

    /** true se mudar de arma ao passar de oldLevel para newLevel. */
    public static boolean weaponChanges(int oldLevel, int newLevel) {
        return !weaponIdForLevel(oldLevel).equals(weaponIdForLevel(newLevel));
    }

    /** true se o nível for um marco de troca (usado para mensagem épica). */
    public static boolean isMilestoneLevel(int level) {
        int lvl = clampLevel(level);
        return lvl == M10 || lvl == M15 || lvl == M20 || lvl == M25 || lvl == M30 ||
               lvl == M35 || lvl == M40 || lvl == M55 || lvl == M75;
    }

    /**
     * Se houve ganho de vários níveis, checa se cruzou ao menos um marco.
     * Ex.: 9 -> 12 cruza o marco 10.
     */
    public static boolean crossedAnyMilestone(int oldLevel, int newLevel) {
        int a = clampLevel(oldLevel);
        int b = clampLevel(newLevel);
        if (b <= a) return false;

        int[] ms = { M10, M15, M20, M25, M30, M35, M40, M55, M75 };
        for (int m : ms) {
            if (a < m && b >= m) return true;
        }
        return false;
    }
}
