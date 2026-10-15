package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.npcstats.AttributeModifierService;
import br.tones.amigonpc.core.npcstats.NpcAttribute;
import br.tones.amigonpc.core.npcstats.NpcStatsConfig;
import br.tones.amigonpc.core.npcstats.NpcStatsConfigService;
import br.tones.amigonpc.core.npcstats.NpcStatsService;
import br.tones.amigonpc.core.npcstats.NpcStatsState;
import br.tones.amigonpc.core.progress.StatScaling;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class NpcLevelDataView {
   public final int level;
   public final long xp;
   public final long xpNeeded;
   public final int availablePoints;
   public final Map allocatedPoints;
   public final Map statValueText;

   private NpcLevelDataView(int level, long xp, long xpNeeded, int availablePoints, Map allocatedPoints, Map statValueText) {
      this.level = Math.max(1, level);
      this.xp = Math.max(0L, xp);
      this.xpNeeded = Math.max(0L, xpNeeded);
      this.availablePoints = Math.max(0, availablePoints);
      this.allocatedPoints = allocatedPoints;
      this.statValueText = statValueText;
   }

   public static NpcLevelDataView fromOwner(UUID ownerId) {
      AmigoNpcManager.LevelProgressSnapshot snap = AmigoNpcManager.getShared().getLevelProgressSnapshot(ownerId);
      long baseHp = 0L;
      long baseDef = 0L;

      try {
         baseHp = AmigoPersistence.loadBaseHp(ownerId);
      } catch (Throwable var37) {
      }

      try {
         baseDef = AmigoPersistence.loadBaseDef(ownerId);
      } catch (Throwable var36) {
      }

      long scaledHp = 0L;
      long scaledDef = 0L;

      try {
         if (baseHp > 0L) {
            scaledHp = StatScaling.scaledHp(baseHp, snap.level());
         }

         if (baseDef >= 0L) {
            StatScaling.scaledDef(baseDef, snap.level());
         }
      } catch (Throwable var35) {
      }

      Map<String, Integer> alloc = new HashMap();
      Map<String, String> values = new HashMap();
      int available = 0;

      try {
         NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
         if (cfg != null && cfg.EnableNpcStats) {
            NpcStatsService svc = NpcStatsService.getShared();
            NpcStatsState st = svc.load(ownerId);
            available = svc.availablePoints(snap.level(), st);
            alloc.put("Health", st.getAllocated(NpcAttribute.HEALTH));
            alloc.put("Damage", st.getAllocated(NpcAttribute.DAMAGE));
            alloc.put("CriticalDamage", st.getAllocated(NpcAttribute.CRITICAL_DAMAGE));
            alloc.put("Defense", st.getAllocated(NpcAttribute.DEFENSE));
            long extraHp = AttributeModifierService.getShared().extraMaxHealth(st);
            long hpMax = (scaledHp > 0L ? scaledHp : 0L) + Math.max(0L, extraHp);
            values.put("Health", hpMax > 0L ? hpMax + " HP" : "0 HP");
            double dmgPct = (double)st.getAllocated(NpcAttribute.DAMAGE) * cfg.DamageStatValuePerPoint;
            values.put("Damage", String.format(Locale.ROOT, "+%.1f%%", dmgPct));
            double critPct = (double)st.getAllocated(NpcAttribute.CRITICAL_DAMAGE) * cfg.CriticalDamageStatValuePerPoint;
            values.put("CriticalDamage", String.format(Locale.ROOT, "%.1f%%", critPct));
            int pDef = st.getAllocated(NpcAttribute.DEFENSE);
            double effective = (double)pDef * cfg.DefenseStatValuePerPoint;
            double cap = Math.min((double)1.0F, Math.max((double)0.0F, cfg.DefenseMaxReductionRatio));
            double reduction = pDef <= 0 ? (double)0.0F : cap * effective / (effective + (double)50.0F);
            reduction = Math.min((double)1.0F, Math.max((double)0.0F, reduction));
            values.put("Defense", String.format(Locale.ROOT, "%.1f%%", reduction * (double)100.0F));

            for(String s : new String[]{"Oxygen", "Mining", "Woodcutting"}) {
               alloc.put(s, 0);
               values.put(s, "0");
            }
         }
      } catch (Throwable var38) {
      }

      alloc.putIfAbsent("Health", 0);
      alloc.putIfAbsent("Damage", 0);
      alloc.putIfAbsent("CriticalDamage", 0);
      alloc.putIfAbsent("Defense", 0);
      values.putIfAbsent("Health", scaledHp > 0L ? scaledHp + " HP" : "0 HP");
      values.putIfAbsent("Damage", "0");
      values.putIfAbsent("CriticalDamage", "0");
      values.putIfAbsent("Defense", "0");
      return new NpcLevelDataView(snap.level(), snap.xp(), snap.xpNeeded(), available, alloc, values);
   }

   public float progress01() {
      if (this.xpNeeded <= 0L) {
         return 0.0F;
      } else {
         double p = (double)this.xp / (double)this.xpNeeded;
         if (p < (double)0.0F) {
            p = (double)0.0F;
         }

         if (p > (double)1.0F) {
            p = (double)1.0F;
         }

         return (float)p;
      }
   }
}
