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
   private final Map<String, Long> nextAllowed = new ConcurrentHashMap<>();

   public static NpcXpAwardService getShared() {
      return SHARED;
   }

   private NpcXpAwardService() {
   }

   private static String key(UUID ownerId, NpcXpSource src) {
      return ownerId + ":" + src.name();
   }

   public boolean awardFixed(UUID ownerId, long amount, NpcXpSource source, NpcXpContext ctx) {
      if (ownerId != null && amount > 0L && source != null) {
         NpcXpSourcesConfig cfg = NpcXpSourcesConfigService.get();
         if (source == NpcXpSource.COMMAND && !cfg.enableCommandXP) {
            return false;
         }

         long now = System.currentTimeMillis();
         long throttle = source == NpcXpSource.COMMAND ? 50L : Math.max(50L, cfg.collectThrottleMs);
         String k = key(ownerId, source);
         long na = this.nextAllowed.getOrDefault(k, 0L);
         if (now < na) {
            return false;
         }

         this.nextAllowed.put(k, now + throttle);
         return AmigoNPCApi.addNpcXp(ownerId, amount, source, ctx);
      } else {
         return false;
      }
   }

   public boolean awardCollect(UUID ownerId, NpcXpSource source, NpcXpContext ctx) {
      if (ownerId != null && source != null) {
         NpcXpSourcesConfig cfg = NpcXpSourcesConfigService.get();
         if (!cfg.enableCollectXP) {
            return false;
         }

         long now = System.currentTimeMillis();
         String k = key(ownerId, source);
         long na = this.nextAllowed.getOrDefault(k, 0L);
         if (now < na) {
            return false;
         }

         this.nextAllowed.put(k, now + Math.max(50L, cfg.collectThrottleMs));
         if (cfg.disableDuringCombat && AmigoNpcManager.getShared().isInCombat(ownerId, now)) {
            return false;
         }

         int lvl = AmigoNpcManager.getShared().getNpcLevel(ownerId);

         long gain = switch (source) {
            case COLLECT_MINING -> cfg.miningBaseXP;
            case COLLECT_WOOD -> cfg.woodBaseXP;
            case COLLECT_PICKUP -> cfg.pickupBaseXP;
            default -> 0;
         } + Math.round(lvl * cfg.levelFactor);
         return gain <= 0L ? false : AmigoNPCApi.addNpcXp(ownerId, gain, source, ctx);
      } else {
         return false;
      }
   }
}
