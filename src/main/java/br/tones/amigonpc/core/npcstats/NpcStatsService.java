package br.tones.amigonpc.core.npcstats;

import br.tones.amigonpc.core.AmigoPersistence;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
      } else {
         BsonDocument doc = AmigoPersistence.loadNpcStats(ownerId);
         if (doc != null && !doc.isEmpty()) {
            if (doc.containsKey("alloc") && doc.get("alloc").isDocument()) {
               BsonDocument alloc = doc.getDocument("alloc");

               for(String k : alloc.keySet()) {
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
   }

   public void save(UUID ownerId, NpcStatsState st) {
      if (ownerId != null && st != null) {
         BsonDocument root = new BsonDocument();
         BsonDocument alloc = new BsonDocument();

         for(Map.Entry e : st.allocations.entrySet()) {
            if (e.getKey() != null) {
               int v = e.getValue() == null ? 0 : Math.max(0, (Integer)e.getValue());
               if (v > 0) {
                  alloc.put(((NpcAttribute)e.getKey()).name(), new BsonInt32(v));
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
      } else {
         NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();

         for(String s : cfg.BlacklistedStats) {
            if (s != null && s.trim().equalsIgnoreCase(a.name())) {
               return true;
            }
         }

         return false;
      }
   }

   public int maxPointsFor(NpcAttribute a) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (cfg.MaxPointsPerStat == null) {
         return 0;
      } else {
         Integer v = (Integer)cfg.MaxPointsPerStat.get(a);
         return v == null ? 0 : Math.max(0, v);
      }
   }

   public AddResult add(UUID ownerId, int npcLevel, NpcAttribute attr, int amount) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return NpcStatsService.AddResult.disabled();
      } else if (ownerId != null && attr != null) {
         if (this.isBlacklisted(attr)) {
            return NpcStatsService.AddResult.error("blacklisted");
         } else {
            int amt = Math.max(1, amount);
            NpcStatsState st = this.load(ownerId);
            int available = this.availablePoints(npcLevel, st);
            if (available <= 0) {
               return NpcStatsService.AddResult.error("no_points");
            } else {
               int cap = this.maxPointsFor(attr);
               int cur = st.getAllocated(attr);
               if (cap > 0 && cur >= cap) {
                  return NpcStatsService.AddResult.error("cap");
               } else {
                  int allow = Math.min(amt, available);
                  if (cap > 0) {
                     allow = Math.min(allow, cap - cur);
                  }

                  if (allow <= 0) {
                     return NpcStatsService.AddResult.error("cap");
                  } else {
                     st.allocations.put(attr, cur + allow);
                     this.save(ownerId, st);
                     if (cfg.DebugLogging) {
                        int earned = this.totalEarnedPoints(npcLevel);
                        int spent = st.spentPoints();
                        int left = Math.max(0, earned - spent);
                        System.out.println(String.format(Locale.ROOT, "[AmigoNPC][Stats] owner=%s lvl=%d earned=%d spent=%d avail=%d add %s +%d => %d", ownerId, npcLevel, earned, spent, left, attr.name(), allow, cur + allow));
                     }

                     return NpcStatsService.AddResult.ok(allow, st);
                  }
               }
            }
         }
      } else {
         return NpcStatsService.AddResult.error("invalid");
      }
   }

   public AddResult addForce(UUID ownerId, int npcLevel, NpcAttribute attr, int amount) {
      NpcStatsConfig cfg = NpcStatsConfigService.getShared().get();
      if (!cfg.EnableNpcStats) {
         return NpcStatsService.AddResult.disabled();
      } else if (ownerId != null && attr != null) {
         if (this.isBlacklisted(attr)) {
            return NpcStatsService.AddResult.error("blacklisted");
         } else {
            int amt = Math.max(1, amount);
            NpcStatsState st = this.load(ownerId);
            int cap = this.maxPointsFor(attr);
            int cur = st.getAllocated(attr);
            if (cap > 0 && cur >= cap) {
               return NpcStatsService.AddResult.error("cap");
            } else {
               int allow = amt;
               if (cap > 0) {
                  allow = Math.min(amt, cap - cur);
               }

               if (allow <= 0) {
                  return NpcStatsService.AddResult.error("cap");
               } else {
                  st.allocations.put(attr, cur + allow);
                  this.save(ownerId, st);
                  if (cfg.DebugLogging) {
                     int earned = this.totalEarnedPoints(npcLevel);
                     int spent = st.spentPoints();
                     int left = Math.max(0, earned - spent);
                     System.out.println(String.format(Locale.ROOT, "[AmigoNPC][Stats][FORCE] owner=%s lvl=%d earned=%d spent=%d avail=%d add %s +%d => %d", ownerId, npcLevel, earned, spent, left, attr.name(), allow, cur + allow));
                  }

                  return NpcStatsService.AddResult.ok(allow, st);
               }
            }
         }
      } else {
         return NpcStatsService.AddResult.error("invalid");
      }
   }

   public void resetAll(UUID ownerId) {
      if (ownerId != null) {
         NpcStatsState st = new NpcStatsState();
         this.save(ownerId, st);
      }
   }

   public static record AddResult(boolean ok, String error, int applied, NpcStatsState state) {
      static AddResult ok(int applied, NpcStatsState st) {
         return new AddResult(true, (String)null, applied, st);
      }

      static AddResult error(String e) {
         return new AddResult(false, e, 0, (NpcStatsState)null);
      }

      static AddResult disabled() {
         return new AddResult(false, "disabled", 0, (NpcStatsState)null);
      }
   }
}
