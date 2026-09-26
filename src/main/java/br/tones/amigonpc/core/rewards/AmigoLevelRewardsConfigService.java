package br.tones.amigonpc.core.rewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.Constants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AmigoLevelRewardsConfigService {
   private static final AmigoLevelRewardsConfigService SHARED = new AmigoLevelRewardsConfigService();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private volatile AmigoLevelRewardsConfig config = new AmigoLevelRewardsConfig();

   public static AmigoLevelRewardsConfigService getShared() {
      return SHARED;
   }

   private AmigoLevelRewardsConfigService() {
      this.loadOrCreate();
   }

   public AmigoLevelRewardsConfig get() {
      return this.config;
   }

   public synchronized void reload() {
      this.loadOrCreate();
   }

   public synchronized void restoreDefaults(boolean backupExisting) {
      Path p = this.path();

      try {
         Files.createDirectories(p.getParent());
      } catch (Throwable var6) {
      }

      if (backupExisting) {
         try {
            if (Files.isRegularFile(p)) {
               Path bak = p.resolveSibling(p.getFileName().toString() + ".bak");

               for (int i = 1; Files.exists(bak); i++) {
                  bak = p.resolveSibling(p.getFileName().toString() + ".bak" + i);
               }

               Files.copy(p, bak);
            }
         } catch (Throwable var7) {
         }
      }

      try {
         AmigoLevelRewardsConfig def = defaultConfig();
         Files.writeString(p, GSON.toJson(def), StandardCharsets.UTF_8);
         this.config = def;
      } catch (Throwable var5) {
      }
   }

   public Path path() {
      return Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("AmigoLevelRewardsConfig.json");
   }

   private void loadOrCreate() {
      Path p = this.path();

      try {
         if (!Files.isRegularFile(p)) {
            Files.createDirectories(p.getParent());
            AmigoLevelRewardsConfig def = defaultConfig();
            Files.writeString(p, GSON.toJson(def), StandardCharsets.UTF_8);
            this.config = def;
            return;
         }

         String raw = Files.readString(p, StandardCharsets.UTF_8);
         JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
         if (root == null) {
            this.config = defaultConfig();
            return;
         }

         boolean keepEmpty = false;

         try {
            JsonElement ke = root.get("KeepEmpty");
            if (ke != null && ke.isJsonPrimitive()) {
               keepEmpty = ke.getAsBoolean();
            }
         } catch (Throwable var9) {
         }

         try {
            JsonElement rewardsEl = root.get("Rewards");
            boolean empty = rewardsEl == null || rewardsEl.isJsonArray() && rewardsEl.getAsJsonArray().size() == 0;
            if (empty && !keepEmpty) {
               AmigoLevelRewardsConfig def = defaultConfig();
               root.remove("Rewards");
               root.add("Rewards", GSON.toJsonTree(def.Rewards));
            }
         } catch (Throwable var10) {
         }

         root.addProperty("Version", "amigonpc-1");
         AmigoLevelRewardsConfig cfg = (AmigoLevelRewardsConfig)GSON.fromJson(root, AmigoLevelRewardsConfig.class);
         if (cfg == null) {
            cfg = defaultConfig();
         }

         validateAndNormalize(cfg);

         try {
            Files.writeString(p, GSON.toJson(cfg), StandardCharsets.UTF_8);
         } catch (Throwable var8) {
         }

         this.config = cfg;
      } catch (Throwable t) {
         this.config = defaultConfig();
      }
   }

   private static AmigoLevelRewardsConfig defaultConfig() {
      AmigoLevelRewardsConfig cfg = new AmigoLevelRewardsConfig();
      cfg.Version = "amigonpc-1";
      cfg.RewardTarget = "NPC";
      cfg.DebugRewardsLogging = false;
      cfg.Rewards = new ArrayList<>();
      cfg.Rewards.add(rewardItems(10, item("Ore_Copper", 10), item("Rock_Gem_Emerald", 4), item("Ingredient_Life_Essence", 10)));
      cfg.Rewards.add(rewardReset(15, 1));
      cfg.Rewards.add(rewardItems(20, item("Ore_Iron", 10), item("Rock_Gem_Emerald", 4), item("Ingredient_Void_Essence", 10)));
      cfg.Rewards.add(rewardItemsAndReset(30, 1, item("Ore_Gold", 10), item("Rock_Gem_Diamond", 4), item("Ingredient_Void_Essence", 10)));
      cfg.Rewards.add(rewardItems(40, item("Ore_Silver", 10), item("Rock_Gem_Diamond", 4), item("Ingredient_Void_Essence", 10)));
      cfg.Rewards.add(rewardReset(45, 1));
      cfg.Rewards.add(rewardItems(50, item("Ore_Thorium", 10), item("Rock_Gem_Topaz", 4), item("Ingredient_Water_Essence", 10)));
      cfg.Rewards.add(rewardItemsAndReset(60, 1, item("Ore_Cobalt", 10), item("Rock_Gem_Topaz", 4), item("Ingredient_Water_Essence", 10)));
      cfg.Rewards.add(rewardItems(70, item("Ore_Cobalt", 10), item("Rock_Gem_Topaz", 4), item("Ingredient_Water_Essence", 10)));
      cfg.Rewards.add(rewardReset(75, 1));
      cfg.Rewards.add(rewardItems(80, item("Ore_Adamantite", 10), item("Rock_Gem_Ruby", 4), item("Ingredient_Fire_Essence", 10)));
      cfg.Rewards.add(rewardItemsAndReset(90, 1, item("Ore_Mithril", 10), item("Rock_Gem_Ruby", 4), item("Ingredient_Fire_Essence", 10)));
      cfg.Rewards.add(rewardItems(100, item("Ore_Onyxium", 10), item("Rock_Gem_Ruby", 4), item("Ingredient_Fire_Essence", 10)));
      return cfg;
   }

   private static AmigoLevelRewardsConfig.ItemEntry item(String id, int qty) {
      AmigoLevelRewardsConfig.ItemEntry it = new AmigoLevelRewardsConfig.ItemEntry();
      it.ItemId = id;
      it.Quantity = qty;
      return it;
   }

   private static AmigoLevelRewardsConfig.RewardEntry rewardReset(int level, int resetPoints) {
      AmigoLevelRewardsConfig.RewardEntry r = new AmigoLevelRewardsConfig.RewardEntry();
      r.Level = level;
      r.ResetPoints = resetPoints;
      r.Items = new ArrayList<>();
      r.Command = "";
      r.CommandTitle = "";
      return r;
   }

   private static AmigoLevelRewardsConfig.RewardEntry rewardItems(int level, AmigoLevelRewardsConfig.ItemEntry... items) {
      AmigoLevelRewardsConfig.RewardEntry r = new AmigoLevelRewardsConfig.RewardEntry();
      r.Level = level;
      r.ResetPoints = 0;
      r.Items = new ArrayList<>();
      if (items != null) {
         for (AmigoLevelRewardsConfig.ItemEntry it : items) {
            if (it != null) {
               r.Items.add(it);
            }
         }
      }

      r.Command = "";
      r.CommandTitle = "";
      return r;
   }

   private static AmigoLevelRewardsConfig.RewardEntry rewardItemsAndReset(int level, int resetPoints, AmigoLevelRewardsConfig.ItemEntry... items) {
      AmigoLevelRewardsConfig.RewardEntry r = rewardItems(level, items);
      r.ResetPoints = resetPoints;
      return r;
   }

   private static void validateAndNormalize(AmigoLevelRewardsConfig cfg) {
      if (cfg.RewardTarget == null || cfg.RewardTarget.isBlank()) {
         cfg.RewardTarget = "NPC";
      }

      String rt = cfg.RewardTarget.trim().toUpperCase();
      if (!rt.equals("NPC") && !rt.equals("PLAYER")) {
         rt = "NPC";
      }

      cfg.RewardTarget = rt;
      if (cfg.Rewards == null) {
         cfg.Rewards = new ArrayList<>();
      }

      List<AmigoLevelRewardsConfig.RewardEntry> out = new ArrayList<>();

      for (AmigoLevelRewardsConfig.RewardEntry r : cfg.Rewards) {
         if (r != null) {
            if (r.Level <= 0) {
               System.out.println("[AmigoNPC][Rewards] Ignorando reward com Level invalido: " + r.Level);
            } else {
               if (r.Items == null) {
                  r.Items = new ArrayList<>();
               }

               if (r.Command == null) {
                  r.Command = "";
               }

               if (r.CommandTitle == null) {
                  r.CommandTitle = "";
               }

               if (r.ResetPoints < 0) {
                  r.ResetPoints = 0;
               }

               List<AmigoLevelRewardsConfig.ItemEntry> items = new ArrayList<>();

               for (AmigoLevelRewardsConfig.ItemEntry it : r.Items) {
                  if (it != null && it.Quantity > 0 && it.ItemId != null && !it.ItemId.isBlank()) {
                     items.add(it);
                  }
               }

               r.Items = items;
               out.add(r);
            }
         }
      }

      out.sort(Comparator.comparingInt(a -> a.Level));
      cfg.Rewards = out;
   }
}
