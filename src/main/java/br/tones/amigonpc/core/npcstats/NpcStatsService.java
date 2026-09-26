package br.tones.amigonpc.core.npcstats;

import br.tones.amigonpc.core.AmigoPersistence;
import java.util.Locale;
import java.util.UUID;
import java.util.Map.Entry;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;

public final class NpcStatsService {
   private static final NpcStatsService SHARED = new NpcStatsService();
   private static final String K_ALLOC = "alloc";

   public static NpcStatsService getShared() {
      return SHARED;
   }

   private NpcStatsService() {
   }

   public int totalEarnedPoints(int npcLevel) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      int lvl = Math.max(1, npcLevel);
      int per = Math.max(0, cfg.StatPointsPerLevel);
      return (lvl - 1) * per;
   }

   public int availablePoints(int npcLevel, NpcStatsState st) {
      int earned = this.totalEarnedPoints(npcLevel);
      int spent = st == null ? 0 : st.spentPoints();
      return Math.max(0, earned - spent);
   }

   public NpcStatsState load(UUID ownerId) {
      NpcStatsState st = new NpcStatsState();
      if (ownerId == null) {
         return st;
      }

      BsonDocument doc = AmigoPersistence.loadNpcStats(ownerId);
      if (doc != null && !doc.isEmpty()) {
         if (doc.containsKey("alloc") && doc.get("alloc").isDocument()) {
            BsonDocument alloc = doc.getDocument("alloc");

            for (String k : alloc.keySet()) {
               NpcAttribute a = NpcAttribute.parse(k);
               if (a != null) {
                  BsonValue v = alloc.get(k);
                  if (v != null && v.isInt32()) {
                     st.allocations.put(a, Math.max(0, v.asInt32().getValue()));
                  }
               }
            }

            return st;
         } else {
            return st;
         }
      } else {
         return st;
      }
   }

   public void save(UUID ownerId, NpcStatsState st) {
      if (ownerId != null && st != null) {
         BsonDocument root = new BsonDocument();
         BsonDocument alloc = new BsonDocument();

         for (Entry<NpcAttribute, Integer> e : st.allocations.entrySet()) {
            if (e.getKey() != null) {
               int v = e.getValue() == null ? 0 : Math.max(0, e.getValue());
               if (v > 0) {
                  alloc.put(e.getKey().name(), new BsonInt32(v));
               }
            }
         }

         root.put("alloc", alloc);
         AmigoPersistence.saveNpcStats(ownerId, root);
      }
   }

   public boolean isBlacklisted(NpcAttribute a) {
      if (a == null) {
         return true;
      }

      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();

      for (String s : cfg.BlacklistedStats) {
         if (s != null && s.trim().equalsIgnoreCase(a.name())) {
            return true;
         }
      }

      return false;
   }

   public int maxPointsFor(NpcAttribute a) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (cfg.MaxPointsPerStat == null) {
         return 0;
      }

      Integer v = cfg.MaxPointsPerStat.get(a);
      return v == null ? 0 : Math.max(0, v);
   }

   public NpcStatsService.AddResult add(UUID ownerId, int npcLevel, NpcAttribute attr, int amount) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return NpcStatsService.AddResult.disabled();
      }

      if (ownerId == null || attr == null) {
         return NpcStatsService.AddResult.error("invalid");
      }

      if (this.isBlacklisted(attr)) {
         return NpcStatsService.AddResult.error("blacklisted");
      }

      int amt = Math.max(1, amount);
      NpcStatsState st = this.load(ownerId);
      int available = this.availablePoints(npcLevel, st);
      if (available <= 0) {
         return NpcStatsService.AddResult.error("no_points");
      }

      int cap = this.maxPointsFor(attr);
      int cur = st.getAllocated(attr);
      if (cap > 0 && cur >= cap) {
         return NpcStatsService.AddResult.error("cap");
      }

      int allow = Math.min(amt, available);
      if (cap > 0) {
         allow = Math.min(allow, cap - cur);
      }

      if (allow <= 0) {
         return NpcStatsService.AddResult.error("cap");
      }

      st.allocations.put(attr, cur + allow);
      this.save(ownerId, st);
      if (cfg.DebugLogging) {
         int earned = this.totalEarnedPoints(npcLevel);
         int spent = st.spentPoints();
         int left = Math.max(0, earned - spent);
         System.out
            .println(
               String.format(
                  Locale.ROOT,
                  "[AmigoNPC][Stats] owner=%s lvl=%d earned=%d spent=%d avail=%d add %s +%d => %d",
                  ownerId,
                  npcLevel,
                  earned,
                  spent,
                  left,
                  attr.name(),
                  allow,
                  cur + allow
               )
            );
      }

      return NpcStatsService.AddResult.ok(allow, st);
   }

   public NpcStatsService.AddResult addForce(UUID ownerId, int npcLevel, NpcAttribute attr, int amount) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return NpcStatsService.AddResult.disabled();
      }

      if (ownerId == null || attr == null) {
         return NpcStatsService.AddResult.error("invalid");
      }

      if (this.isBlacklisted(attr)) {
         return NpcStatsService.AddResult.error("blacklisted");
      }

      int amt = Math.max(1, amount);
      NpcStatsState st = this.load(ownerId);
      int cap = this.maxPointsFor(attr);
      int cur = st.getAllocated(attr);
      if (cap > 0 && cur >= cap) {
         return NpcStatsService.AddResult.error("cap");
      }

      int allow = amt;
      if (cap > 0) {
         allow = Math.min(allow, cap - cur);
      }

      if (allow <= 0) {
         return NpcStatsService.AddResult.error("cap");
      }

      st.allocations.put(attr, cur + allow);
      this.save(ownerId, st);
      if (cfg.DebugLogging) {
         int earned = this.totalEarnedPoints(npcLevel);
         int spent = st.spentPoints();
         int left = Math.max(0, earned - spent);
         System.out
            .println(
               String.format(
                  Locale.ROOT,
                  "[AmigoNPC][Stats][FORCE] owner=%s lvl=%d earned=%d spent=%d avail=%d add %s +%d => %d",
                  ownerId,
                  npcLevel,
                  earned,
                  spent,
                  left,
                  attr.name(),
                  allow,
                  cur + allow
               )
            );
      }

      return NpcStatsService.AddResult.ok(allow, st);
   }

   public void resetAll(UUID ownerId) {
      if (ownerId != null) {
         NpcStatsState st = new NpcStatsState();
         this.save(ownerId, st);
      }
   }

   public record AddResult(boolean ok, String error, int applied, NpcStatsState state) {
      static NpcStatsService.AddResult ok(int applied, NpcStatsState st) {
         return new NpcStatsService.AddResult(true, null, applied, st);
      }

      static NpcStatsService.AddResult error(String e) {
         return new NpcStatsService.AddResult(false, e, 0, null);
      }

      static NpcStatsService.AddResult disabled() {
         return new NpcStatsService.AddResult(false, "disabled", 0, null);
      }
   }
}
