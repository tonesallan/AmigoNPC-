package br.tones.amigonpc.core.npcstats;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AttributeModifierService {
   private static final AttributeModifierService SHARED = new AttributeModifierService();

   public static AttributeModifierService getShared() {
      return SHARED;
   }

   private AttributeModifierService() {
   }

   public long extraMaxHealth(NpcStatsState st) {
      if (st == null) {
         return 0L;
      }

      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      int p = st.getAllocated(NpcAttribute.HEALTH);
      return p <= 0 ? 0L : Math.max(0L, Math.round(p * cfg.HealthStatValuePerPoint));
   }

   public float applyOutgoingDamageMods(UUID ownerId, float baseDamage, int npcLevel) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return baseDamage;
      }

      NpcStatsState st = NpcStatsService.getShared().load(ownerId);
      float dmg = baseDamage;
      int pDmg = st.getAllocated(NpcAttribute.DAMAGE);
      double mult = 1.0;
      if (pDmg > 0) {
         mult = 1.0 + pDmg * cfg.DamageStatValuePerPoint / 100.0;
      }

      dmg = (float)(dmg * mult);
      int pCrit = st.getAllocated(NpcAttribute.CRITICAL_DAMAGE);
      double critChance = 0.0;
      boolean crit = false;
      double critMul = 1.0;
      if (pCrit > 0) {
         critChance = pCrit * cfg.CriticalDamageStatValuePerPoint / 100.0;
         double r = ThreadLocalRandom.current().nextDouble();
         if (r < critChance) {
            crit = true;
            critMul = ThreadLocalRandom.current().nextDouble(1.5, 2.0);
            dmg = (float)(dmg * critMul);
         }
      }

      if (cfg.DebugLogging) {
         System.out
            .println(
               String.format(
                  Locale.ROOT,
                  "[AmigoNPC][Stats][DMG] owner=%s lvl=%d base=%.3f dmgPts=%d mult=%.3f critPts=%d chance=%.4f crit=%s critMul=%.3f final=%.3f",
                  ownerId,
                  npcLevel,
                  baseDamage,
                  pDmg,
                  mult,
                  pCrit,
                  critChance,
                  String.valueOf(crit),
                  critMul,
                  dmg
               )
            );
      }

      return dmg;
   }

   public float applyIncomingDamageMods(UUID ownerId, float baseTakenDamage, int npcLevel) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return baseTakenDamage;
      }

      NpcStatsState st = NpcStatsService.getShared().load(ownerId);
      int pDef = st.getAllocated(NpcAttribute.DEFENSE);
      if (pDef <= 0) {
         return baseTakenDamage;
      }

      double effective = pDef * cfg.DefenseStatValuePerPoint;
      double cap = Math.min(1.0, Math.max(0.0, cfg.DefenseMaxReductionRatio));
      double reduction = cap * effective / (effective + 50.0);
      reduction = Math.min(1.0, Math.max(0.0, reduction));
      float finalDmg = (float)(baseTakenDamage * (1.0 - reduction));
      if (cfg.DebugLogging) {
         System.out
            .println(
               String.format(
                  Locale.ROOT,
                  "[AmigoNPC][Stats][DEF] owner=%s lvl=%d base=%.3f defPts=%d eff=%.3f cap=%.3f red=%.4f final=%.3f",
                  ownerId,
                  npcLevel,
                  baseTakenDamage,
                  pDef,
                  effective,
                  cap,
                  reduction,
                  finalDmg
               )
            );
      }

      return finalDmg;
   }
}
