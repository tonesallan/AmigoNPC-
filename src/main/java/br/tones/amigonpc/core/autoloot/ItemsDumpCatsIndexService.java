package br.tones.amigonpc.core.autoloot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.Constants;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

public final class ItemsDumpCatsIndexService {
   private static final Gson GSON = new Gson();
   private static final Path FILE_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("items_dumpcats.json.gz");
   private static volatile ItemsDumpCatsIndexService.Index CACHED;

   private ItemsDumpCatsIndexService() {
   }

   public static ItemsDumpCatsIndexService.Index get() {
      ItemsDumpCatsIndexService.Index idx = CACHED;
      if (idx == null) {
         synchronized (ItemsDumpCatsIndexService.class) {
            idx = CACHED;
            if (idx == null) {
               idx = CACHED = loadNow();
            }
         }
      }

      return idx;
   }

   public static Set<String> getAllCategories() {
      return get().allCategories;
   }

   public static Set<String> getCategoriesForItem(String itemId) {
      if (itemId == null) {
         return Collections.emptySet();
      }

      Set<String> s = get().itemToCategories.get(itemId);
      return s != null ? s : Collections.emptySet();
   }

   private static ItemsDumpCatsIndexService.Index loadNow() {
      Map<String, Set<String>> itemToCats = new LinkedHashMap<>();
      Set<String> allCats = new LinkedHashSet<>();
      InputStream in = null;

      try {
         if (Files.isRegularFile(FILE_PATH)) {
            in = Files.newInputStream(FILE_PATH);
         }

         if (in == null) {
            return new ItemsDumpCatsIndexService.Index(itemToCats, allCats);
         }

         try (
            GZIPInputStream gz = new GZIPInputStream(in);
            InputStreamReader r = new InputStreamReader(gz, StandardCharsets.UTF_8);
         ) {
            JsonObject root = (JsonObject)GSON.fromJson(r, JsonObject.class);
            if (root != null) {
               try {
                  JsonObject byCat = root.getAsJsonObject("itemsByCategory");
                  if (byCat != null) {
                     for (Entry<String, JsonElement> e : byCat.entrySet()) {
                        if (e.getKey() != null) {
                           allCats.add(e.getKey());
                        }
                     }
                  }
               } catch (Throwable var45) {
               }

               JsonObject items = null;

               try {
                  items = root.getAsJsonObject("items");
               } catch (Throwable ignored) {
                  items = null;
               }

               if (items == null) {
                  return new ItemsDumpCatsIndexService.Index(itemToCats, allCats);
               }

               for (Entry<String, JsonElement> e : items.entrySet()) {
                  String id = e.getKey();
                  if (id != null) {
                     JsonObject obj = null;

                     try {
                        obj = e.getValue().getAsJsonObject();
                     } catch (Throwable ignored) {
                        obj = null;
                     }

                     if (obj != null) {
                        Set<String> cats = new LinkedHashSet<>();

                        try {
                           JsonArray resolved = obj.getAsJsonArray("resolvedCategories");
                           if (resolved != null) {
                              for (JsonElement je : resolved) {
                                 JsonObject co = null;

                                 try {
                                    co = je.getAsJsonObject();
                                 } catch (Throwable ignored) {
                                    co = null;
                                 }

                                 if (co != null) {
                                    JsonElement cid = co.get("id");
                                    if (cid != null && cid.isJsonPrimitive()) {
                                       String c = cid.getAsString();
                                       if (c != null && !c.isBlank()) {
                                          cats.add(c);
                                       }
                                    }
                                 }
                              }
                           }
                        } catch (Throwable var43) {
                        }

                        if (cats.isEmpty()) {
                           try {
                              JsonArray ids = obj.getAsJsonArray("categoryIds");
                              if (ids != null) {
                                 for (JsonElement je : ids) {
                                    if (je != null && je.isJsonPrimitive()) {
                                       String c = je.getAsString();
                                       if (c != null && !c.isBlank()) {
                                          cats.add(c);
                                       }
                                    }
                                 }
                              }
                           } catch (Throwable var44) {
                           }
                        }

                        if (!cats.isEmpty()) {
                           itemToCats.put(id, cats);
                           allCats.addAll(cats);
                        }
                     }
                  }
               }

               return new ItemsDumpCatsIndexService.Index(itemToCats, allCats);
            }

            return new ItemsDumpCatsIndexService.Index(itemToCats, allCats);
         }
      } catch (Throwable var48) {
      } finally {
         try {
            if (in != null) {
               in.close();
            }
         } catch (Throwable var37) {
         }
      }

      return new ItemsDumpCatsIndexService.Index(itemToCats, allCats);
   }

   public static final class Index {
      public final Map<String, Set<String>> itemToCategories;
      public final Set<String> allCategories;

      private Index(Map<String, Set<String>> itemToCategories, Set<String> allCategories) {
         this.itemToCategories = itemToCategories != null ? itemToCategories : Collections.emptyMap();
         this.allCategories = allCategories != null ? allCategories : Collections.emptySet();
      }
   }
}
