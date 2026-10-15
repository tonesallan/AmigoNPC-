package br.tones.amigonpc.core.xp.sources;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.Constants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class NpcXpSourcesConfigService {
   private static final Gson GSON;
   private static final Path CONFIG_PATH;
   private static volatile NpcXpSourcesConfig CACHED;

   private NpcXpSourcesConfigService() {
   }

   public static NpcXpSourcesConfig get() {
      NpcXpSourcesConfig c = CACHED;
      if (c == null) {
         synchronized(NpcXpSourcesConfigService.class) {
            c = CACHED;
            if (c == null) {
               c = reloadNow();
            }
         }
      }

      return c;
   }

   public static synchronized NpcXpSourcesConfig reloadNow() {
      NpcXpSourcesConfig cfg = null;

      try {
         if (Files.isRegularFile(CONFIG_PATH, new LinkOption[0])) {
            String txt = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            cfg = (NpcXpSourcesConfig)GSON.fromJson(txt, NpcXpSourcesConfig.class);
         }
      } catch (Throwable var2) {
      }

      if (cfg == null) {
         cfg = NpcXpSourcesConfig.defaults();
         save(cfg);
      } else {
         if (cfg.collectThrottleMs < 50) {
            cfg.collectThrottleMs = 50;
         }

         if (cfg.collectThrottleMs > 10000) {
            cfg.collectThrottleMs = 10000;
         }

         if (cfg.miningBaseXP < 0) {
            cfg.miningBaseXP = 0;
         }

         if (cfg.woodBaseXP < 0) {
            cfg.woodBaseXP = 0;
         }

         if (cfg.pickupBaseXP < 0) {
            cfg.pickupBaseXP = 0;
         }

         if (cfg.levelFactor < (double)0.0F) {
            cfg.levelFactor = (double)0.0F;
         }

         if (cfg.levelFactor > (double)10.0F) {
            cfg.levelFactor = (double)10.0F;
         }
      }

      CACHED = cfg;
      return cfg;
   }

   public static void save(NpcXpSourcesConfig cfg) {
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
      CONFIG_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("NpcXpSourcesConfig.json");
   }
}
