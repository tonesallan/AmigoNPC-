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
      } else {
         NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
         int p = st.getAllocated(NpcAttribute.HEALTH);
         return p <= 0 ? 0L : Math.max(0L, Math.round((double)p * cfg.HealthStatValuePerPoint));
      }
   }

   public float applyOutgoingDamageMods(UUID ownerId, float baseDamage, int npcLevel) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return baseDamage;
      } else {
         NpcStatsState st = NpcStatsService.getShared().load(ownerId);
         int pDmg = st.getAllocated(NpcAttribute.DAMAGE);
         double mult = (double)1.0F;
         if (pDmg > 0) {
            mult = (double)1.0F + (double)pDmg * cfg.DamageStatValuePerPoint / (double)100.0F;
         }

         float dmg = (float)((double)baseDamage * mult);
         int pCrit = st.getAllocated(NpcAttribute.CRITICAL_DAMAGE);
         double critChance = (double)0.0F;
         boolean crit = false;
         double critMul = (double)1.0F;
         if (pCrit > 0) {
            critChance = (double)pCrit * cfg.CriticalDamageStatValuePerPoint / (double)100.0F;
            double r = ThreadLocalRandom.current().nextDouble();
            if (r < critChance) {
               crit = true;
               critMul = ThreadLocalRandom.current().nextDouble((double)1.5F, (double)2.0F);
               dmg = (float)((double)dmg * critMul);
            }
         }

         if (cfg.DebugLogging) {
            System.out.println(String.format(Locale.ROOT, "[AmigoNPC][Stats][DMG] owner=%s lvl=%d base=%.3f dmgPts=%d mult=%.3f critPts=%d chance=%.4f crit=%s critMul=%.3f final=%.3f", ownerId, npcLevel, baseDamage, pDmg, mult, pCrit, critChance, String.valueOf(crit), critMul, dmg));
         }

         return dmg;
      }
   }

   public float applyIncomingDamageMods(UUID ownerId, float baseTakenDamage, int npcLevel) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return baseTakenDamage;
      } else {
         NpcStatsState st = NpcStatsService.getShared().load(ownerId);
         int pDef = st.getAllocated(NpcAttribute.DEFENSE);
         if (pDef <= 0) {
            return baseTakenDamage;
         } else {
            double effective = (double)pDef * cfg.DefenseStatValuePerPoint;
            double cap = Math.min((double)1.0F, Math.max((double)0.0F, cfg.DefenseMaxReductionRatio));
            double reduction = cap * effective / (effective + (double)50.0F);
            reduction = Math.min((double)1.0F, Math.max((double)0.0F, reduction));
            float finalDmg = (float)((double)baseTakenDamage * ((double)1.0F - reduction));
            if (cfg.DebugLogging) {
               System.out.println(String.format(Locale.ROOT, "[AmigoNPC][Stats][DEF] owner=%s lvl=%d base=%.3f defPts=%d eff=%.3f cap=%.3f red=%.4f final=%.3f", ownerId, npcLevel, baseTakenDamage, pDef, effective, cap, reduction, finalDmg));
            }

            return finalDmg;
         }
      }
   }
}
