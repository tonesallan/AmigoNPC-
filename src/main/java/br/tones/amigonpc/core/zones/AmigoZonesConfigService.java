package br.tones.amigonpc.core.zones;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.Constants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class AmigoZonesConfigService {
   private static final Gson GSON;
   private static final Path CONFIG_PATH;
   private static final Path LEGACY_PATH;
   private static volatile AmigoZonesConfig CACHED;
   private static volatile long lastLoadMillis;

   private AmigoZonesConfigService() {
   }

   public static AmigoZonesConfig get() {
      long now = System.currentTimeMillis();
      if (CACHED == null || now - lastLoadMillis > 2000L) {
         reload();
      }

      return CACHED != null ? CACHED : AmigoZonesConfig.defaults();
   }

   public static synchronized AmigoZonesConfig reload() {
      lastLoadMillis = System.currentTimeMillis();
      AmigoZonesConfig cfg = null;
      Path loadedFrom = null;

      try {
         if (Files.isRegularFile(CONFIG_PATH, new LinkOption[0])) {
            loadedFrom = CONFIG_PATH;
         } else if (Files.isRegularFile(LEGACY_PATH, new LinkOption[0])) {
            loadedFrom = LEGACY_PATH;
         }

         if (loadedFrom != null) {
            String txt = Files.readString(loadedFrom, StandardCharsets.UTF_8);
            cfg = (AmigoZonesConfig)GSON.fromJson(txt, AmigoZonesConfig.class);
         }
      } catch (Throwable var3) {
      }

      if (cfg == null) {
         cfg = AmigoZonesConfig.defaults();
         save(cfg);
      } else {
         if (cfg.varianceRange < 0) {
            cfg.varianceRange = 0;
         }

         if (cfg.varianceRange > 20) {
            cfg.varianceRange = 20;
         }

         if (cfg.cacheMinutes < 1) {
            cfg.cacheMinutes = 1;
         }

         if (cfg.cacheMinutes > 60) {
            cfg.cacheMinutes = 60;
         }

         if (cfg.zoneRanges == null || cfg.zoneRanges.isEmpty()) {
            cfg.zoneRanges = AmigoZonesConfig.defaults().zoneRanges;
         }

         if (loadedFrom == LEGACY_PATH) {
            save(cfg);
         }
      }

      CACHED = cfg;
      return cfg;
   }

   public static void save(AmigoZonesConfig cfg) {
      if (cfg != null) {
         try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(cfg), StandardCharsets.UTF_8);
         } catch (Throwable var2) {
         }

      }
   }

   static {
      GSON = (new GsonBuilder()).setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).setPrettyPrinting().create();
      CONFIG_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("Conf-NPC_Zones.json");
      LEGACY_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("ZonesMobLevelConfig.json");
      CACHED = null;
      lastLoadMillis = 0L;
   }
}
