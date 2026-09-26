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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionTraceService {
   public static final long WINDOW_MILLIS = 1200000L;
   private static final ActionTraceService SHARED = new ActionTraceService();
   private final Map<UUID, Deque<ActionTraceService.TraceEvent>> eventsByPlayer = new ConcurrentHashMap<>();
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
         Deque<ActionTraceService.TraceEvent> q = this.eventsByPlayer.computeIfAbsent(playerId, k -> new ArrayDeque<>());
         synchronized (q) {
            q.addLast(new ActionTraceService.TraceEvent(now, type, detail));
            this.pruneLocked(q, now);

            while (q.size() > 2500) {
               q.removeFirst();
            }
         }
      }
   }

   public List<ActionTraceService.TraceEvent> snapshotLast20m(UUID playerId) {
      if (playerId == null) {
         return List.of();
      }

      Deque<ActionTraceService.TraceEvent> q = this.eventsByPlayer.get(playerId);
      if (q == null) {
         return List.of();
      }

      long now = System.currentTimeMillis();
      List<ActionTraceService.TraceEvent> out = new ArrayList<>();
      synchronized (q) {
         this.pruneLocked(q, now);
         out.addAll(q);
         return out;
      }
   }

   public Map<String, List<ActionTraceService.TraceEvent>> snapshotLast20mForMostRecentPlayers(int maxPlayers) {
      if (maxPlayers <= 0) {
         return Map.of();
      }

      long now = System.currentTimeMillis();
      List<ActionTraceService.PlayerLastEvent> candidates = new ArrayList<>();

      for (Entry<UUID, Deque<ActionTraceService.TraceEvent>> e : this.eventsByPlayer.entrySet()) {
         UUID pid = e.getKey();
         Deque<ActionTraceService.TraceEvent> q = e.getValue();
         if (pid != null && q != null) {
            long last = -1L;
            synchronized (q) {
               this.pruneLocked(q, now);
               ActionTraceService.TraceEvent tail = q.peekLast();
               if (tail != null) {
                  last = tail.epochMillis;
               }
            }

            if (last > 0L) {
               candidates.add(new ActionTraceService.PlayerLastEvent(pid, last));
            }
         }
      }

      if (candidates.isEmpty()) {
         return Map.of();
      }

      candidates.sort(Comparator.<ActionTraceService.PlayerLastEvent>comparingLong(p -> p.lastEpochMillis).reversed());
      if (candidates.size() > maxPlayers) {
         candidates = candidates.subList(0, maxPlayers);
      }

      Map<String, List<ActionTraceService.TraceEvent>> out = new HashMap<>();

      for (ActionTraceService.PlayerLastEvent c : candidates) {
         out.put(String.valueOf(c.playerId), this.snapshotLast20m(c.playerId));
      }

      return out;
   }

   private void pruneLocked(Deque<ActionTraceService.TraceEvent> q, long nowMillis) {
      long cutoff = nowMillis - 1200000L;

      while (!q.isEmpty()) {
         ActionTraceService.TraceEvent first = q.peekFirst();
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
