package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.npcstats.AttributeModifierService;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsConfig;
import br.tones.amigonpc.core.npcstats.NpcStatsConfigService;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.npcstats.NpcStatsState;
import br.tones.amigonpc.core.progress.StatScaling;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class NpcLevelDataViewStatsSupport {
   private NpcLevelDataViewStatsSupport() {
   }

   static long loadScaledHp(UUID ownerId, int level) {
      long baseHp = 0L;

      try {
         baseHp = AmigoPersistence.loadBaseHp(ownerId);
      } catch (Throwable var6) {
      }

      try {
         if (baseHp > 0L) {
            return StatScaling.scaledHp(baseHp, level);
         }
      } catch (Throwable var5) {
      }

      return 0L;
   }

   static int populateConfiguredStats(UUID ownerId, int level, long scaledHp, Map<String, Integer> alloc, Map<String, String> values) {
      int available = 0;

      try {
         NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
         if (cfg == null || !cfg.EnableNpcStats) {
            return 0;
         }

         NpcStatsService svc = NpcStatsService.getShared();
         NpcStatsState st = svc.load(ownerId);
         available = svc.availablePoints(level, st);
         alloc.put("Health", st.getAllocated(NpcAttribute.HEALTH));
         alloc.put("Damage", st.getAllocated(NpcAttribute.DAMAGE));
         alloc.put("CriticalDamage", st.getAllocated(NpcAttribute.CRITICAL_DAMAGE));
         alloc.put("Defense", st.getAllocated(NpcAttribute.DEFENSE));
         long extraHp = AttributeModifierService.getShared().extraMaxHealth(st);
         long hpMax = (scaledHp > 0L ? scaledHp : 0L) + Math.max(0L, extraHp);
         values.put("Health", hpMax > 0L ? AmigoText.format("ui.stats.value.health", hpMax) : AmigoText.text("ui.stats.value.zero_hp"));
         double dmgPct = st.getAllocated(NpcAttribute.DAMAGE) * cfg.DamageStatValuePerPoint;
         values.put("Damage", String.format(Locale.ROOT, "+%.1f%%", dmgPct));
         double critPct = st.getAllocated(NpcAttribute.CRITICAL_DAMAGE) * cfg.CriticalDamageStatValuePerPoint;
         values.put("CriticalDamage", String.format(Locale.ROOT, "%.1f%%", critPct));
         int pDef = st.getAllocated(NpcAttribute.DEFENSE);
         double effective = pDef * cfg.DefenseStatValuePerPoint;
         double cap = Math.min(1.0, Math.max(0.0, cfg.DefenseMaxReductionRatio));
         double reduction = pDef <= 0 ? 0.0 : cap * effective / (effective + 50.0);
         reduction = Math.min(1.0, Math.max(0.0, reduction));
         values.put("Defense", String.format(Locale.ROOT, "%.1f%%", reduction * 100.0));

         for (String statName : new String[]{"Oxygen", "Mining", "Woodcutting"}) {
            alloc.put(statName, 0);
            values.put(statName, AmigoText.text("ui.stats.value.zero"));
         }
      } catch (Throwable var29) {
      }

      return available;
   }

   static void ensureFallbackStats(Map<String, Integer> alloc, Map<String, String> values, long scaledHp) {
      alloc.putIfAbsent("Health", 0);
      alloc.putIfAbsent("Damage", 0);
      alloc.putIfAbsent("CriticalDamage", 0);
      alloc.putIfAbsent("Defense", 0);
      values.putIfAbsent("Health", scaledHp > 0L ? AmigoText.format("ui.stats.value.health", scaledHp) : AmigoText.text("ui.stats.value.zero_hp"));
      values.putIfAbsent("Damage", AmigoText.text("ui.stats.value.zero"));
      values.putIfAbsent("CriticalDamage", AmigoText.text("ui.stats.value.zero"));
      values.putIfAbsent("Defense", AmigoText.text("ui.stats.value.zero"));
   }
}
