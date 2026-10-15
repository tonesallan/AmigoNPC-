package br.tones.amigonpc.core.xp.sources;

import br.tones.amigonpc.api.AmigoNPCApi;
import br.tones.amigonpc.api.NpcXpContext;
import br.tones.amigonpc.api.NpcXpSource;
import br.tones.amigonpc.core.AmigoNpcManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcXpAwardService {
   private static final NpcXpAwardService SHARED = new NpcXpAwardService();
   private final Map nextAllowed = new ConcurrentHashMap();

   public static NpcXpAwardService getShared() {
      return SHARED;
   }

   private NpcXpAwardService() {
   }

   private static String key(UUID ownerId, NpcXpSource src) {
      String var10000 = String.valueOf(ownerId);
      return var10000 + ":" + src.name();
   }

   public boolean awardFixed(UUID ownerId, long amount, NpcXpSource source, NpcXpContext ctx) {
      if (ownerId != null && amount > 0L && source != null) {
         NpcXpSourcesConfig cfg = NpcXpSourcesConfigService.get();
         if (source == NpcXpSource.COMMAND && !cfg.enableCommandXP) {
            return false;
         } else {
            long now = System.currentTimeMillis();
            long throttle = source == NpcXpSource.COMMAND ? 50L : Math.max(50L, (long)cfg.collectThrottleMs);
            String k = key(ownerId, source);
            long na = (Long)this.nextAllowed.getOrDefault(k, 0L);
            if (now < na) {
               return false;
            } else {
               this.nextAllowed.put(k, now + throttle);
               return AmigoNPCApi.addNpcXp(ownerId, amount, source, ctx);
            }
         }
      } else {
         return false;
      }
   }

   public boolean awardCollect(UUID ownerId, NpcXpSource source, NpcXpContext ctx) {
      if (ownerId != null && source != null) {
         NpcXpSourcesConfig cfg = NpcXpSourcesConfigService.get();
         if (!cfg.enableCollectXP) {
            return false;
         } else {
            long now = System.currentTimeMillis();
            String k = key(ownerId, source);
            long na = (Long)this.nextAllowed.getOrDefault(k, 0L);
            if (now < na) {
               return false;
            } else {
               this.nextAllowed.put(k, now + Math.max(50L, (long)cfg.collectThrottleMs));
               if (cfg.disableDuringCombat && AmigoNpcManager.getShared().isInCombat(ownerId, now)) {
                  return false;
               } else {
                  int lvl = AmigoNpcManager.getShared().getNpcLevel(ownerId);
                  int base;
                  switch (source) {
                     case COLLECT_MINING -> base = cfg.miningBaseXP;
                     case COLLECT_WOOD -> base = cfg.woodBaseXP;
                     case COLLECT_PICKUP -> base = cfg.pickupBaseXP;
                     default -> base = 0;
                  }

                  long gain = (long)base + Math.round((double)lvl * cfg.levelFactor);
                  return gain <= 0L ? false : AmigoNPCApi.addNpcXp(ownerId, gain, source, ctx);
               }
            }
         }
      } else {
         return false;
      }
   }
}
