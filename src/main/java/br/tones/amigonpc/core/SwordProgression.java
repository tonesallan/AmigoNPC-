package br.tones.amigonpc.core;

public final class SwordProgression {

    // Marcos onde TROCA a arma (quando chega nesse level, troca)
    public static final int[] MILESTONES = { 10, 15, 20, 25, 30, 35, 40, 55, 75 };

    // IDs (conforme suas imagens)
    public static final String CRUDE_SWORD          = "Weapon_Sword_Crude";
    public static final String ONYXIUM_SWORD        = "Weapon_Sword_Onyxium";
    public static final String RUSTY_STEEL_SWORD    = "Weapon_Sword_Steel_Rusty";
    public static final String THORIUM_SWORD        = "Weapon_Sword_Thorium";
    public static final String IRON_LONGSWORD       = "Weapon_Longsword_Iron";
    public static final String MITHRIL_SWORD        = "Weapon_Sword_Mithril";
    public static final String THORIUM_LONGSWORD    = "Weapon_Longsword_Thorium";
    public static final String KATANA              = "Weapon_Longsword_Katana";
    public static final String FLAME_LONGSWORD      = "Weapon_Longsword_Flame";
    public static final String ONYXIUM_LONGSWORD    = "Weapon_Longsword_Onyxium";

    private SwordProgression() {}

    public static String weaponIdForLevel(int lvl) {
        if (lvl < 1) lvl = 1;

        // Regras (igual você descreveu):
        // 1-9: crude, 10-14: onyxium, 15-19: rusty, 20-24: thorium, 25-29: iron long,
        // 30-34: mithril, 35-39: thorium long, 40-54: katana, 55-74: flame long, 75+: onyxium long
        if (lvl < 10) return CRUDE_SWORD;
        if (lvl < 15) return ONYXIUM_SWORD;
        if (lvl < 20) return RUSTY_STEEL_SWORD;
        if (lvl < 25) return THORIUM_SWORD;
        if (lvl < 30) return IRON_LONGSWORD;
        if (lvl < 35) return MITHRIL_SWORD;
        if (lvl < 40) return THORIUM_LONGSWORD;
        if (lvl < 55) return KATANA;
        if (lvl < 75) return FLAME_LONGSWORD;
        return ONYXIUM_LONGSWORD;
    }

    /** Se subiu vários níveis (por qualquer motivo), retorna o ÚLTIMO marco atravessado; senão -1. */
    public static int lastMilestoneCrossed(int oldLvl, int newLvl) {
        if (newLvl <= oldLvl) return -1;
        int last = -1;
        for (int m : MILESTONES) {
            if (oldLvl < m && newLvl >= m) last = m;
        }
        return last;
    }
}
