package br.tones.amigonpc.core.debug;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionTraceService {
   public static final long WINDOW_MILLIS = 1200000L;
   private static final ActionTraceService SHARED = new ActionTraceService();
   private final Map eventsByPlayer = new ConcurrentHashMap();
   private static final int MAX_EVENTS_PER_PLAYER = 2500;

   public static ActionTraceService getShared() {
      return SHARED;
   }

   private ActionTraceService() {
   }

   public void record(UUID playerId, String type, String detail) {
      if (playerId != null) {
         if (type == null) {
            type = "";
         }

         if (detail == null) {
            detail = "";
         }

         long now = System.currentTimeMillis();
         Deque<TraceEvent> q = (Deque)this.eventsByPlayer.computeIfAbsent(playerId, (k) -> new ArrayDeque());
         synchronized(q) {
            q.addLast(new TraceEvent(now, type, detail));
            this.pruneLocked(q, now);

            while(q.size() > 2500) {
               q.removeFirst();
            }

         }
      }
   }

   public List snapshotLast20m(UUID playerId) {
      if (playerId == null) {
         return List.of();
      } else {
         Deque<TraceEvent> q = (Deque)this.eventsByPlayer.get(playerId);
         if (q == null) {
            return List.of();
         } else {
            long now = System.currentTimeMillis();
            List<TraceEvent> out = new ArrayList();
            synchronized(q) {
               this.pruneLocked(q, now);
               out.addAll(q);
               return out;
            }
         }
      }
   }

   public Map snapshotLast20mForMostRecentPlayers(int maxPlayers) {
      if (maxPlayers <= 0) {
         return Map.of();
      } else {
         long now = System.currentTimeMillis();
         List<PlayerLastEvent> candidates = new ArrayList();

         for(Map.Entry e : this.eventsByPlayer.entrySet()) {
            UUID pid = (UUID)e.getKey();
            Deque<TraceEvent> q = (Deque)e.getValue();
            if (pid != null && q != null) {
               long last = -1L;
               synchronized(q) {
                  this.pruneLocked(q, now);
                  TraceEvent tail = (TraceEvent)q.peekLast();
                  if (tail != null) {
                     last = tail.epochMillis;
                  }
               }

               if (last > 0L) {
                  candidates.add(new PlayerLastEvent(pid, last));
               }
            }
         }

         if (candidates.isEmpty()) {
            return Map.of();
         } else {
            candidates.sort(Comparator.comparingLong((p) -> p.lastEpochMillis).reversed());
            if (candidates.size() > maxPlayers) {
               candidates = candidates.subList(0, maxPlayers);
            }

            Map<String, List<TraceEvent>> out = new HashMap();

            for(PlayerLastEvent c : candidates) {
               out.put(String.valueOf(c.playerId), this.snapshotLast20m(c.playerId));
            }

            return out;
         }
      }
   }

   private void pruneLocked(Deque q, long nowMillis) {
      long cutoff = nowMillis - 1200000L;

      while(!q.isEmpty()) {
         TraceEvent first = (TraceEvent)q.peekFirst();
         if (first == null || first.epochMillis >= cutoff) {
            break;
         }

         q.removeFirst();
      }

   }

   private static final class PlayerLastEvent {
      final UUID playerId;
      final long lastEpochMillis;

      PlayerLastEvent(UUID playerId, long lastEpochMillis) {
         this.playerId = playerId;
         this.lastEpochMillis = lastEpochMillis;
      }
   }

   public static final class TraceEvent {
      public long epochMillis;
      public String isoUtc;
      public String type;
      public String detail;

      public TraceEvent(long epochMillis, String type, String detail) {
         this.epochMillis = epochMillis;
         this.isoUtc = Instant.ofEpochMilli(epochMillis).toString();
         this.type = type;
         this.detail = detail;
      }
   }
}
