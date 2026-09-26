package br.tones.amigonpc.core.rewards;

import br.tones.amigonpc.core.AmigoPersistence;
import java.util.UUID;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;

public final class RewardsStateService {
   private static final RewardsStateService SHARED = new RewardsStateService();
   private static final String K_CLAIMED = "claimed";
   private static final String K_RESET = "resetPoints";

   public static RewardsStateService getShared() {
      return SHARED;
   }

   private RewardsStateService() {
   }

   public RewardsState load(UUID ownerId) {
      RewardsState st = new RewardsState();
      if (ownerId == null) {
         return st;
      }

      BsonDocument doc = AmigoPersistence.loadAmigoRewards(ownerId);
      if (doc != null && !doc.isEmpty()) {
         try {
            st.resetPoints = Math.max(0, doc.getInt32("resetPoints", new BsonInt32(0)).getValue());
         } catch (Throwable var8) {
         }

         try {
            if (doc.containsKey("claimed") && doc.get("claimed").isArray()) {
               for (BsonValue v : doc.getArray("claimed")) {
                  if (v != null && v.isInt32()) {
                     int lvl = v.asInt32().getValue();
                     if (lvl > 0) {
                        st.claimedRewardLevels.add(lvl);
                     }
                  }
               }
            }
         } catch (Throwable var9) {
         }

         return st;
      } else {
         return st;
      }
   }

   public void save(UUID ownerId, RewardsState st) {
      if (ownerId != null && st != null) {
         BsonDocument doc = new BsonDocument();
         doc.put("resetPoints", new BsonInt32(Math.max(0, st.resetPoints)));
         BsonArray arr = new BsonArray();

         for (Integer lvl : st.claimedRewardLevels) {
            if (lvl != null && lvl > 0) {
               arr.add(new BsonInt32(lvl));
            }
         }

         doc.put("claimed", arr);
         AmigoPersistence.saveAmigoRewards(ownerId, doc);
      }
   }

   public int getResetPoints(UUID ownerId) {
      return this.load(ownerId).resetPoints;
   }

   public boolean consumeResetPoint(UUID ownerId) {
      if (ownerId == null) {
         return false;
      }

      RewardsState st = this.load(ownerId);
      if (st.resetPoints <= 0) {
         return false;
      }

      st.resetPoints--;
      this.save(ownerId, st);
      return true;
   }
}
