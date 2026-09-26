package br.tones.amigonpc.core.debug;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.HytaleBridge;
import br.tones.amigonpc.ui.AmigoUiFactory;
import br.tones.amigonpc.ui.UiBridge;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.Constants;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ErrorDumpService {
   private static final ErrorDumpService SHARED = new ErrorDumpService();
   private static final int MAX_ERROR_FILES = 10;
   private static final long SERVER_ERROR_RATE_LIMIT_MS = 30000L;
   private static final Map<String, Long> LAST_SERVER_DUMP_BY_KEY = new ConcurrentHashMap<>();
   private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
   private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

   public static ErrorDumpService getShared() {
      return SHARED;
   }

   private ErrorDumpService() {
   }

   public void dumpOnServerError(UUID playerId, String tag, Throwable t) {
      this.dumpOnServerError(playerId, tag, t, null);
   }

   public void dumpOnServerError(UUID playerId, String tag, Throwable t, Map<String, Object> extra) {
      try {
         if (tag == null) {
            tag = "";
         }

         if (t == null) {
            return;
         }

         String key = tag + "|" + playerId;
         long now = System.currentTimeMillis();
         Long last = LAST_SERVER_DUMP_BY_KEY.get(key);
         if (last != null && now - last < 30000L) {
            return;
         }

         LAST_SERVER_DUMP_BY_KEY.put(key, now);
         Path dir = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("errors");
         this.ensureDir(dir);
         this.enforceFileLimit(dir);
         ErrorDumpService.ErrorReport report = this.buildServerErrorReport(playerId, tag, t, extra);
         String baseName = "error-" + ZonedDateTime.now().format(FILE_TS) + ".json";
         Path out = this.resolveUnique(dir, baseName);

         try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            GSON.toJson(report, w);
         }
      } catch (Throwable var18) {
      }
   }

   public void maybeDumpOnDisconnect(UUID playerId, Object evt) {
      try {
         if (playerId == null) {
            return;
         }

         String reason = this.extractDisconnectReasonBestEffort(evt);
         boolean reasonMatches = this.looksLikeUiSelectorDisconnect(reason);
         boolean heuristicMatches = !reasonMatches && this.looksLikeRecentUiAction(playerId);
         if (!reasonMatches && !heuristicMatches) {
            return;
         }

         Path dir = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("errors");
         this.ensureDir(dir);
         this.enforceFileLimit(dir);
         ErrorDumpService.ErrorReport report = this.buildReport(playerId, evt, reason, reasonMatches, heuristicMatches);
         String fileName = "error-" + ZonedDateTime.now().format(FILE_TS) + ".json";
         Path out = this.resolveUnique(dir, fileName);

         try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            GSON.toJson(report, w);
         }
      } catch (Throwable var15) {
      }
   }

   private Path resolveUnique(Path dir, String fileName) {
      try {
         Path p = dir.resolve(fileName);
         if (!Files.exists(p)) {
            return p;
         }

         String base = fileName;
         String ext = "";
         int dot = fileName.lastIndexOf(46);
         if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
         }

         for (int i = 1; i <= 50; i++) {
            Path candidate = dir.resolve(base + "-" + i + ext);
            if (!Files.exists(candidate)) {
               return candidate;
            }
         }

         return p;
      } catch (Throwable ignored) {
         return dir.resolve(fileName);
      }
   }

   private boolean looksLikeRecentUiAction(UUID playerId) {
      try {
         if (playerId == null) {
            return false;
         }

         long now = System.currentTimeMillis();
         long windowMs = 12000L;
         List<ActionTraceService.TraceEvent> events = ActionTraceService.getShared().snapshotLast20m(playerId);

         for (int i = events.size() - 1; i >= 0; i--) {
            ActionTraceService.TraceEvent e = events.get(i);
            if (e != null) {
               long dt = now - e.epochMillis;
               if (dt > 12000L) {
                  break;
               }

               if (e.type != null && e.type.startsWith("ui_")) {
                  return true;
               }
            }
         }
      } catch (Throwable var11) {
      }

      return false;
   }

   private boolean looksLikeUiSelectorDisconnect(String reason) {
      if (reason == null) {
         return false;
      }

      String r = reason.toLowerCase(Locale.ROOT);
      return !r.contains("customui") || !r.contains("selector") && !r.contains("selected element") && !r.contains("not found")
         ? r.contains("selected element") && r.contains("selector")
         : true;
   }

   private String extractDisconnectReasonBestEffort(Object evt) {
      if (evt == null) {
         return null;
      }

      Object o = this.invokeNoArg(evt, "getReason", "getDisconnectReason", "getKickReason", "getMessage", "reason", "message");
      if (o == null) {
         o = this.invokeNoArg(evt, "getCause", "cause");
      }

      if (o == null) {
         try {
            return evt.toString();
         } catch (Throwable ignored) {
            return null;
         }
      } else {
         try {
            return String.valueOf(o);
         } catch (Throwable ignored) {
            return null;
         }
      }
   }

   private Object invokeNoArg(Object obj, String... names) {
      for (String n : names) {
         try {
            Method m = obj.getClass().getMethod(n);
            return m.invoke(obj);
         } catch (Throwable var9) {
            try {
               Field f = obj.getClass().getField(n);
               return f.get(obj);
            } catch (Throwable var8) {
            }
         }
      }

      return null;
   }

   private void ensureDir(Path dir) throws IOException {
      if (!Files.exists(dir)) {
         Files.createDirectories(dir);
      }
   }

   private void enforceFileLimit(Path dir) {
      try {
         List<Path> files = new ArrayList<>();

         try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "error-*.json")) {
            for (Path p : ds) {
               if (Files.isRegularFile(p)) {
                  files.add(p);
               }
            }
         }

         if (files.size() < 10) {
            return;
         }

         files.sort(Comparator.comparingLong(px -> {
            try {
               return Files.getLastModifiedTime(px).toMillis();
            } catch (Throwable ignored) {
               return Long.MAX_VALUE;
            }
         }));

         while (files.size() >= 10) {
            Path oldest = files.remove(0);

            try {
               Files.deleteIfExists(oldest);
            } catch (Throwable var7) {
            }
         }
      } catch (Throwable var9) {
      }
   }

   private ErrorDumpService.ErrorReport buildReport(UUID playerId, Object evt, String reason, boolean reasonMatches, boolean heuristicMatches) {
      ErrorDumpService.ErrorReport r = new ErrorDumpService.ErrorReport();
      ZoneId zone = ZoneId.systemDefault();
      ZonedDateTime nowLocal = ZonedDateTime.now(zone);
      Instant nowUtc = Instant.now();
      r.schemaVersion = 4;
      r.kind = "disconnect";
      r.createdAtUtc = nowUtc.toString();
      r.createdAtLocal = nowLocal.toString();
      r.timezone = zone.getId();
      r.modVersion = BuildInfo.getModVersion();
      r.buildId = BuildInfo.getBuildId();
      r.buildIdSource = BuildInfo.getBuildIdSource();
      r.buildJarLocation = BuildInfo.getJarLocation();
      r.javaVersion = BuildInfo.getJavaVersion();
      r.playerUuid = String.valueOf(playerId);
      r.playerName = this.extractPlayerNameBestEffort(evt);
      r.disconnectReason = reason;
      r.detectedAsErrorDisconnect = reasonMatches || heuristicMatches;
      r.trigger = reasonMatches ? "disconnectReason" : "heuristicRecentUiAction";
      r.requestedFileNameFormat = "error-data-hora:minuto:segundo.json";
      r.actualFileNameFormat = "error-YYYY-MM-DD-HH-mm-ss.json";
      r.lastErrors = new HashMap<>();
      r.lastErrors.put("HytaleBridge", HytaleBridge.getLastError());
      r.lastErrors.put("UiBridge", UiBridge.getLastError());
      r.lastErrors.put("AmigoUiFactory", AmigoUiFactory.getLastError());

      try {
         r.npcSnapshot = AmigoNpcManager.getShared().getNpcDebugSnapshot(playerId);
      } catch (Throwable ignored) {
         r.npcSnapshot = null;
      }

      r.last20mActions = ActionTraceService.getShared().snapshotLast20m(playerId);
      return r;
   }

   private ErrorDumpService.ErrorReport buildServerErrorReport(UUID playerId, String tag, Throwable t, Map<String, Object> extra) {
      ErrorDumpService.ErrorReport r = new ErrorDumpService.ErrorReport();
      ZoneId zone = ZoneId.systemDefault();
      ZonedDateTime nowLocal = ZonedDateTime.now(zone);
      Instant nowUtc = Instant.now();
      r.schemaVersion = 4;
      r.kind = "serverError";
      r.createdAtUtc = nowUtc.toString();
      r.createdAtLocal = nowLocal.toString();
      r.timezone = zone.getId();
      r.modVersion = BuildInfo.getModVersion();
      r.buildId = BuildInfo.getBuildId();
      r.buildIdSource = BuildInfo.getBuildIdSource();
      r.buildJarLocation = BuildInfo.getJarLocation();
      r.javaVersion = BuildInfo.getJavaVersion();
      r.playerUuid = playerId == null ? null : String.valueOf(playerId);
      r.playerName = null;
      r.disconnectReason = null;
      r.detectedAsErrorDisconnect = false;
      r.trigger = "serverError";
      r.serverErrorTag = tag;
      r.serverThread = Thread.currentThread().getName();
      r.serverErrorClass = t.getClass().getName();
      r.serverErrorMessage = t.getMessage();
      r.serverErrorStack = this.stackTraceToString(t, 180);
      r.extra = extra;
      r.requestedFileNameFormat = "error-data-hora:minuto:segundo.json";
      r.actualFileNameFormat = "error-YYYY-MM-DD-HH-mm-ss.json";
      r.lastErrors = new HashMap<>();
      r.lastErrors.put("HytaleBridge", HytaleBridge.getLastError());
      r.lastErrors.put("UiBridge", UiBridge.getLastError());
      r.lastErrors.put("AmigoUiFactory", AmigoUiFactory.getLastError());
      if (playerId != null) {
         try {
            r.npcSnapshot = AmigoNpcManager.getShared().getNpcDebugSnapshot(playerId);
         } catch (Throwable ignored) {
            r.npcSnapshot = null;
         }

         r.last20mActions = ActionTraceService.getShared().snapshotLast20m(playerId);
      } else {
         r.npcSnapshot = null;
         r.last20mActions = null;

         try {
            r.last20mActionsByPlayer = ActionTraceService.getShared().snapshotLast20mForMostRecentPlayers(5);
         } catch (Throwable ignored) {
            r.last20mActionsByPlayer = null;
         }
      }

      return r;
   }

   private String stackTraceToString(Throwable t, int maxLines) {
      try {
         StringBuilder sb = new StringBuilder();
         int lines = 0;
         Throwable cur = t;

         while (cur != null && lines < maxLines) {
            sb.append(cur.getClass().getName());
            if (cur.getMessage() != null) {
               sb.append(": ").append(cur.getMessage());
            }

            sb.append("\n");
            StackTraceElement[] var6 = cur.getStackTrace();
            int var7 = var6.length;
            int var8 = 0;

            while (true) {
               if (var8 < var7) {
                  StackTraceElement el = var6[var8];
                  if (lines++ < maxLines) {
                     sb.append("  at ").append(el.toString()).append("\n");
                     var8++;
                     continue;
                  }
               }

               cur = cur.getCause();
               if (cur != null && lines < maxLines) {
                  sb.append("Caused by:\n");
               }
               break;
            }
         }

         return sb.toString();
      } catch (Throwable ignored) {
         return null;
      }
   }

   private String extractPlayerNameBestEffort(Object evt) {
      if (evt == null) {
         return null;
      }

      Object pr = this.invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
      if (pr != null) {
         Object name = this.invokeNoArg(pr, "getName", "name", "getUsername", "getDisplayName");
         if (name != null) {
            return String.valueOf(name);
         }
      }

      Object name2 = this.invokeNoArg(evt, "getName", "name", "getUsername", "getDisplayName");
      return name2 != null ? String.valueOf(name2) : null;
   }

   public static final class ErrorReport {
      public int schemaVersion;
      public String modVersion;
      public String buildId;
      public String buildIdSource;
      public String buildJarLocation;
      public String javaVersion;
      public String kind;
      public String createdAtUtc;
      public String createdAtLocal;
      public String timezone;
      public String requestedFileNameFormat;
      public String actualFileNameFormat;
      public String playerUuid;
      public String playerName;
      public boolean detectedAsErrorDisconnect;
      public String disconnectReason;
      public String trigger;
      public String serverErrorTag;
      public String serverThread;
      public String serverErrorClass;
      public String serverErrorMessage;
      public String serverErrorStack;
      public Map<String, Object> extra;
      public Map<String, String> lastErrors;
      public NpcDebugSnapshot npcSnapshot;
      public List<ActionTraceService.TraceEvent> last20mActions;
      public Map<String, List<ActionTraceService.TraceEvent>> last20mActionsByPlayer;
   }
}
