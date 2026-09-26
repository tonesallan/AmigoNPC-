package br.tones.amigonpc.core.npcstats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.Constants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public final class NpcStatsConfigService {
   private static final NpcStatsConfigService SHARED = new NpcStatsConfigService();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private volatile NpcStatsConfig cfg = new NpcStatsConfig();
   private volatile long nextReloadAtMs = 0L;

   public static NpcStatsConfigService getShared() {
      return SHARED;
   }

   private NpcStatsConfigService() {
   }

   public NpcStatsConfig get() {
      this.reloadIfDue();
      return this.cfg;
   }

   public void reloadNow() {
      this.nextReloadAtMs = 0L;
      this.reloadIfDue();
   }

   private void reloadIfDue() {
      long now = System.currentTimeMillis();
      if (now >= this.nextReloadAtMs) {
         this.nextReloadAtMs = now + 2000L;
         this.loadOrCreate();
      }
   }

   private static Path path() {
      return Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("npc-stats.json");
   }

   private void loadOrCreate() {
      Path p = path();

      try {
         if (!Files.isRegularFile(p)) {
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(new NpcStatsConfig()), StandardCharsets.UTF_8);
         }

         String raw = Files.readString(p, StandardCharsets.UTF_8);
         NpcStatsConfig loaded = (NpcStatsConfig)GSON.fromJson(raw, NpcStatsConfig.class);
         if (loaded != null) {
            if (loaded.MaxPointsPerStat == null) {
               loaded.MaxPointsPerStat = (new NpcStatsConfig()).MaxPointsPerStat;
            }

            if (loaded.BlacklistedStats == null) {
               loaded.BlacklistedStats = new ArrayList<>();
            }

            this.cfg = loaded;
         }
      } catch (Throwable ignored) {
         this.cfg = new NpcStatsConfig();
      }
   }
}
