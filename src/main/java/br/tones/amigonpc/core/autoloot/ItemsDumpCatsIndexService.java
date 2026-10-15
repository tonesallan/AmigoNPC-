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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public final class ItemsDumpCatsIndexService {
   private static final Gson GSON = new Gson();
   private static final Path FILE_PATH;
   private static volatile Index CACHED;

   private ItemsDumpCatsIndexService() {
   }

   public static Index get() {
      Index idx = CACHED;
      if (idx == null) {
         synchronized(ItemsDumpCatsIndexService.class) {
            idx = CACHED;
            if (idx == null) {
               idx = CACHED = loadNow();
            }
         }
      }

      return idx;
   }

   public static Set getAllCategories() {
      return get().allCategories;
   }

   public static Set getCategoriesForItem(String itemId) {
      if (itemId == null) {
         return Collections.emptySet();
      } else {
         Set<String> s = (Set)get().itemToCategories.get(itemId);
         return s != null ? s : Collections.emptySet();
      }
   }

   private static Index loadNow() {
      Map<String, Set<String>> itemToCats = new LinkedHashMap();
      Set<String> allCats = new LinkedHashSet();
      InputStream in = null;

      Index var3;
      try {
         if (Files.isRegularFile(FILE_PATH, new LinkOption[0])) {
            in = Files.newInputStream(FILE_PATH);
         }

         if (in != null) {
            GZIPInputStream gz = new GZIPInputStream(in);

            Index var6;
            label480: {
               try {
                  InputStreamReader r = new InputStreamReader(gz, StandardCharsets.UTF_8);

                  label428: {
                     try {
                        JsonObject root = (JsonObject)GSON.fromJson(r, JsonObject.class);
                        if (root != null) {
                           try {
                              JsonObject byCat = root.getAsJsonObject("itemsByCategory");
                              if (byCat != null) {
                                 for(Map.Entry e : byCat.entrySet()) {
                                    if (e.getKey() != null) {
                                       allCats.add((String)e.getKey());
                                    }
                                 }
                              }
                           } catch (Throwable var45) {
                           }

                           JsonObject items = null;

                           try {
                              items = root.getAsJsonObject("items");
                           } catch (Throwable var42) {
                              items = null;
                           }

                           if (items == null) {
                              break label428;
                           }

                           Iterator var54 = items.entrySet().iterator();

                           while(true) {
                              if (!var54.hasNext()) {
                                 break label428;
                              }

                              Map.Entry<String, JsonElement> e = (Map.Entry)var54.next();
                              String id = (String)e.getKey();
                              if (id != null) {
                                 JsonObject obj = null;

                                 try {
                                    obj = ((JsonElement)e.getValue()).getAsJsonObject();
                                 } catch (Throwable var41) {
                                    obj = null;
                                 }

                                 if (obj != null) {
                                    Set<String> cats = new LinkedHashSet();

                                    try {
                                       JsonArray resolved = obj.getAsJsonArray("resolvedCategories");
                                       if (resolved != null) {
                                          for(JsonElement je : resolved) {
                                             JsonObject co = null;

                                             try {
                                                co = je.getAsJsonObject();
                                             } catch (Throwable var40) {
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
                                             for(JsonElement je : ids) {
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
                        }

                        var6 = new Index(itemToCats, allCats);
                     } catch (Throwable var46) {
                        try {
                           r.close();
                        } catch (Throwable var39) {
                           var46.addSuppressed(var39);
                        }

                        throw var46;
                     }

                     r.close();
                     break label480;
                  }

                  r.close();
               } catch (Throwable var47) {
                  try {
                     gz.close();
                  } catch (Throwable var38) {
                     var47.addSuppressed(var38);
                  }

                  throw var47;
               }

               gz.close();
               return new Index(itemToCats, allCats);
            }

            gz.close();
            return var6;
         }

         var3 = new Index(itemToCats, allCats);
      } catch (Throwable var48) {
         return new Index(itemToCats, allCats);
      } finally {
         try {
            if (in != null) {
               in.close();
            }
         } catch (Throwable var37) {
         }

      }

      return var3;
   }

   static {
      FILE_PATH = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("items_dumpcats.json.gz");
   }

   public static final class Index {
      public final Map itemToCategories;
      public final Set allCategories;

      private Index(Map itemToCategories, Set allCategories) {
         this.itemToCategories = itemToCategories != null ? itemToCategories : Collections.emptyMap();
         this.allCategories = allCategories != null ? allCategories : Collections.emptySet();
      }
   }
}
