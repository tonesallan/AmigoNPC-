package br.tones.amigonpc.core.worldscaling;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.Constants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldMobScalingConfigService {
   private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).setPrettyPrinting().create();
   private static final Path CONFIG_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("Conf-NPC_MobScaling.json");
   private static final Path LEGACY_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("WorldMobScalingConfig.json");
   private static volatile WorldMobScalingConfig CACHED;

   private WorldMobScalingConfigService() {
   }

   public static WorldMobScalingConfig get() {
      WorldMobScalingConfig c = CACHED;
      if (c == null) {
         synchronized (WorldMobScalingConfigService.class) {
            c = CACHED;
            if (c == null) {
               c = reloadNow();
            }
         }
      }

      return c;
   }

   public static synchronized WorldMobScalingConfig reloadNow() {
      WorldMobScalingConfig cfg = null;
      Path loadedFrom = null;

      try {
         if (Files.isRegularFile(CONFIG_PATH)) {
            loadedFrom = CONFIG_PATH;
         } else if (Files.isRegularFile(LEGACY_PATH)) {
            loadedFrom = LEGACY_PATH;
         }

         if (loadedFrom != null) {
            String txt = Files.readString(loadedFrom, StandardCharsets.UTF_8);
            cfg = (WorldMobScalingConfig)GSON.fromJson(txt, WorldMobScalingConfig.class);
         }
      } catch (Throwable var3) {
      }

      if (cfg == null) {
         cfg = WorldMobScalingConfig.defaults();
         save(cfg);
      } else {
         if (cfg.cacheTtlMs < 500L) {
            cfg.cacheTtlMs = 500L;
         }

         if (cfg.cacheTtlMs > 120000L) {
            cfg.cacheTtlMs = 120000L;
         }

         if (loadedFrom == LEGACY_PATH) {
            save(cfg);
         }
      }

      CACHED = cfg;
      return cfg;
   }

   public static void save(WorldMobScalingConfig cfg) {
      if (cfg != null) {
         try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(cfg), StandardCharsets.UTF_8);
         } catch (Throwable var2) {
         }
      }
   }
}
