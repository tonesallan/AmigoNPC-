package br.tones.amigonpc.core.autoloot;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.Constants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AutoLootConfigService {
   private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).setPrettyPrinting().create();
   private static final Path CONFIG_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("Conf-NPC_AutoLoot.json");
   private static final Set<String> KNOWN_CATEGORIES = Collections.unmodifiableSet(
      new LinkedHashSet<>(
         Arrays.asList(
            "Blocks.Deco",
            "Blocks.Fluids",
            "Blocks.Ores",
            "Blocks.Plants",
            "Blocks.Portals",
            "Blocks.Rocks",
            "Blocks.Soils",
            "Blocks.Structural",
            "Blocks.Wood",
            "Fish",
            "Furniture.Beds",
            "Furniture.Benches",
            "Furniture.Containers",
            "Furniture.Doors",
            "Furniture.Furniture",
            "Furniture.Lighting",
            "Furniture.Shelves",
            "Furniture.Signs",
            "Items",
            "Items.Armors",
            "Items.CombatMilestone2",
            "Items.Consumables",
            "Items.Debug",
            "Items.Foods",
            "Items.Ingredients",
            "Items.Potions",
            "Items.Recipes",
            "Items.Tools",
            "Items.Utility",
            "Items.Weapons",
            "Tool",
            "Tool.Block",
            "Tool.BrushFilters",
            "Tool.BuilderTool",
            "Tool.BuilderToolSecondPage",
            "Tool.Machinima",
            "Tool.ScriptedBrushes",
            "Upgrade",
            "uncategorized"
         )
      )
   );
   private static volatile AutoLootConfig CACHED;

   private AutoLootConfigService() {
   }

   public static AutoLootConfig get() {
      AutoLootConfig c = CACHED;
      if (c == null) {
         synchronized (AutoLootConfigService.class) {
            c = CACHED;
            if (c == null) {
               c = reloadNow();
            }
         }
      }

      return c;
   }

   public static synchronized AutoLootConfig reloadNow() {
      AutoLootConfig cfg = null;

      try {
         if (Files.isRegularFile(CONFIG_PATH)) {
            String txt = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            cfg = (AutoLootConfig)GSON.fromJson(txt, AutoLootConfig.class);
         }
      } catch (Throwable var3) {
      }

      if (cfg == null) {
         cfg = AutoLootConfig.defaults();
      }

      if (cfg.Categories == null) {
         cfg.Categories = new LinkedHashMap<>();
      }

      if (cfg.VerticalScanBlocks <= 0) {
         cfg.VerticalScanBlocks = 10;
      }

      boolean changed = ensureCategories(cfg);

      try {
         if (!Files.isRegularFile(CONFIG_PATH) || changed) {
            save(cfg);
         }
      } catch (Throwable var4) {
      }

      CACHED = cfg;
      return cfg;
   }

   private static boolean ensureCategories(AutoLootConfig cfg) {
      if (cfg == null) {
         return false;
      }

      if (cfg.Categories == null) {
         cfg.Categories = new LinkedHashMap<>();
      }

      Set<String> all = ItemsDumpCatsIndexService.getAllCategories();
      if (all == null || all.isEmpty()) {
         all = KNOWN_CATEGORIES;
      }

      if (all != null && !all.isEmpty()) {
         boolean changed = false;
         if (cfg.Categories.isEmpty()) {
            for (String c : all) {
               if (c != null && !c.isBlank()) {
                  cfg.Categories.put(c, Boolean.TRUE);
               }
            }

            return true;
         } else {
            for (String c : all) {
               if (c != null && !c.isBlank() && !cfg.Categories.containsKey(c)) {
                  cfg.Categories.put(c, Boolean.TRUE);
                  changed = true;
               }
            }

            return changed;
         }
      } else {
         return false;
      }
   }

   public static void save(AutoLootConfig cfg) {
      if (cfg != null) {
         try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(cfg), StandardCharsets.UTF_8);
         } catch (Throwable var2) {
         }
      }
   }

   public static boolean isItemAllowed(String itemId) {
      if (itemId != null && !itemId.isBlank()) {
         AutoLootConfig cfg = get();
         if (cfg == null) {
            return true;
         }

         if (!cfg.EnableCategoryFilter) {
            return true;
         }

         Map<String, Boolean> catCfg = cfg.Categories;
         if (catCfg != null && !catCfg.isEmpty()) {
            Set<String> cats = ItemsDumpCatsIndexService.getCategoriesForItem(itemId);
            if (cats != null && !cats.isEmpty()) {
               for (String c : cats) {
                  if (c != null && !c.isBlank()) {
                     Boolean enabled = catCfg.get(c);
                     if (enabled == null) {
                        return true;
                     }

                     if (Boolean.TRUE.equals(enabled)) {
                        return true;
                     }
                  }
               }

               return false;
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }
}
