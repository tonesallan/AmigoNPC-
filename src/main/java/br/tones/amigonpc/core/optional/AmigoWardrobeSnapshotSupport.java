package br.tones.amigonpc.core.optional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

final class AmigoWardrobeSnapshotSupport {
   private static final String KEY_COSMETICS = "cosmetics";
   private static final String KEY_COSMETIC_IDS = "cosmeticIds";
   private static final String KEY_HIDDEN_TYPES = "hiddenTypes";
   private static final String KEY_HIDDEN_COSMETIC_TYPES = "hiddenCosmeticTypes";
   private static final String KEY_OPTION_ID = "optionId";
   private static final String KEY_VARIANT_ID = "variantId";
   private static final String KEY_SLOT = "slot";
   private static final String KEY_VALUE_BYTES = "valueBytes";
   private static final String KEY_VALUE_ID = "valueId";
   private static final String KEY_PLAYER_WARDROBE = "PlayerWardrobe";
   private static final String KEY_PLAYER_WARDROBE_ALT = "playerWardrobe";
   private static final String KEY_PLAYER_WARDROBE_COSMETICS = "Cosmetics";
   private static final String KEY_PLAYER_WARDROBE_COSMETICS_ALT = "cosmetics";
   private static final String KEY_PLAYER_WARDROBE_HIDDEN_TYPES = "HiddenCosmeticTypes";
   private static final String KEY_PLAYER_WARDROBE_HIDDEN_TYPES_ALT = "hiddenCosmeticTypes";
   private static final String KEY_COSMETIC_ID_SET = "cosmeticIdSet";
   private static final String KEY_ID = "Id";
   private static final String KEY_OPTION = "Option";
   private static final String KEY_VARIANT = "Variant";
   private static final String KEY_SLOT_PASCAL = "Slot";

   private AmigoWardrobeSnapshotSupport() {
   }

   static boolean hasSnapshotCosmetics(BsonDocument snapshot) {
      if (snapshot != null && !snapshot.isEmpty()) {
         AmigoWardrobeSnapshotSupport.SnapshotData data = extractSnapshotData(snapshot);
         return data != null && data.hasAnyData();
      } else {
         return false;
      }
   }

   static AmigoWardrobeSnapshotSupport.SnapshotData extractSnapshotData(BsonDocument snapshot) {
      LinkedHashMap<String, AmigoWardrobeSnapshotSupport.SnapshotCosmetic> cosmeticsBySlot = new LinkedHashMap<>();
      LinkedHashSet<String> cosmeticIds = new LinkedHashSet<>();
      LinkedHashSet<String> hiddenTypes = new LinkedHashSet<>();
      collectCosmeticsFromArray(cosmeticsBySlot, getArray(snapshot, "cosmetics"));
      collectStringArray(cosmeticIds, getArray(snapshot, "cosmeticIds"));
      collectStringArray(cosmeticIds, getArray(snapshot, "cosmeticIdSet"));
      collectStringArray(hiddenTypes, getArray(snapshot, "hiddenCosmeticTypes"));
      collectStringArray(hiddenTypes, getArray(snapshot, "hiddenTypes"));
      BsonDocument nestedWardrobe = getDocument(snapshot, "PlayerWardrobe");
      if (nestedWardrobe == null) {
         nestedWardrobe = getDocument(snapshot, "playerWardrobe");
      }

      if (nestedWardrobe != null) {
         collectCosmeticsFromStructuredContainer(cosmeticsBySlot, nestedWardrobe);
         collectStringArray(hiddenTypes, getArray(nestedWardrobe, "HiddenCosmeticTypes"));
         collectStringArray(hiddenTypes, getArray(nestedWardrobe, "hiddenCosmeticTypes"));
      }

      collectCosmeticsFromStructuredContainer(cosmeticsBySlot, snapshot);

      for (AmigoWardrobeSnapshotSupport.SnapshotCosmetic cosmetic : cosmeticsBySlot.values()) {
         if (cosmetic != null && cosmetic.valueId() != null && !cosmetic.valueId().isBlank()) {
            cosmeticIds.add(cosmetic.valueId());
         }
      }

      return new AmigoWardrobeSnapshotSupport.SnapshotData(new ArrayList<>(cosmeticsBySlot.values()), cosmeticIds, hiddenTypes);
   }

   private static void collectCosmeticsFromStructuredContainer(Map<String, AmigoWardrobeSnapshotSupport.SnapshotCosmetic> out, BsonDocument container) {
      if (out != null && container != null) {
         BsonValue cosmeticsValue = firstValue(container, "Cosmetics", "cosmetics");
         if (cosmeticsValue != null) {
            if (cosmeticsValue.isDocument()) {
               for (Entry<String, BsonValue> e : cosmeticsValue.asDocument().entrySet()) {
                  if (e != null && e.getKey() != null) {
                     AmigoWardrobeSnapshotSupport.SnapshotCosmetic cosmetic = cosmeticFromSlotValue(e.getKey(), e.getValue());
                     mergeCosmetic(out, cosmetic);
                  }
               }
            } else {
               if (cosmeticsValue.isArray()) {
                  collectCosmeticsFromArray(out, cosmeticsValue.asArray());
               }
            }
         }
      }
   }

   private static void collectCosmeticsFromArray(Map<String, AmigoWardrobeSnapshotSupport.SnapshotCosmetic> out, BsonArray cosmetics) {
      if (out != null && cosmetics != null) {
         for (BsonValue v : cosmetics) {
            if (v != null && v.isDocument()) {
               AmigoWardrobeSnapshotSupport.SnapshotCosmetic cosmetic = cosmeticFromDocument(v.asDocument(), getStringAny(v.asDocument(), "slot", "Slot"));
               mergeCosmetic(out, cosmetic);
            }
         }
      }
   }

   private static AmigoWardrobeSnapshotSupport.SnapshotCosmetic cosmeticFromSlotValue(String slot, BsonValue value) {
      if (value == null) {
         return null;
      } else if (value.isDocument()) {
         return cosmeticFromDocument(value.asDocument(), slot);
      } else {
         return value.isString() ? new AmigoWardrobeSnapshotSupport.SnapshotCosmetic(slot, value.asString().getValue(), null, null, null) : null;
      }
   }

   private static AmigoWardrobeSnapshotSupport.SnapshotCosmetic cosmeticFromDocument(BsonDocument item, String fallbackSlot) {
      if (item == null) {
         return null;
      }

      String slot = fallbackSlot != null && !fallbackSlot.isBlank() ? fallbackSlot : getStringAny(item, "slot", "Slot");
      if (slot != null && !slot.isBlank()) {
         byte[] valueBytes = null;
         if (item.containsKey("valueBytes") && item.get("valueBytes").isBinary()) {
            valueBytes = item.getBinary("valueBytes").getData();
         }

         String valueId = getStringAny(item, "valueId", "Id", "cosmeticId", "id");
         String optionId = getStringAny(item, "optionId", "Option", "option");
         String variantId = getStringAny(item, "variantId", "Variant", "variant");
         return (valueId == null || valueId.isBlank()) && valueBytes == null
            ? null
            : new AmigoWardrobeSnapshotSupport.SnapshotCosmetic(slot, valueId, optionId, variantId, valueBytes);
      } else {
         return null;
      }
   }

   private static void mergeCosmetic(Map<String, AmigoWardrobeSnapshotSupport.SnapshotCosmetic> out, AmigoWardrobeSnapshotSupport.SnapshotCosmetic incoming) {
      if (out != null && incoming != null && incoming.slot() != null && !incoming.slot().isBlank()) {
         AmigoWardrobeSnapshotSupport.SnapshotCosmetic existing = out.get(incoming.slot());
         if (existing == null || snapshotCosmeticScore(incoming) >= snapshotCosmeticScore(existing)) {
            out.put(incoming.slot(), incoming);
         }
      }
   }

   private static int snapshotCosmeticScore(AmigoWardrobeSnapshotSupport.SnapshotCosmetic cosmetic) {
      if (cosmetic == null) {
         return -1;
      }

      int score = 0;
      if (cosmetic.valueId() != null && !cosmetic.valueId().isBlank()) {
         score += 2;
      }

      if (cosmetic.optionId() != null && !cosmetic.optionId().isBlank()) {
         score++;
      }

      if (cosmetic.variantId() != null && !cosmetic.variantId().isBlank()) {
         score++;
      }

      if (cosmetic.valueBytes() != null && cosmetic.valueBytes().length > 0) {
         score += 3;
      }

      return score;
   }

   private static void collectStringArray(Set<String> out, BsonArray arr) {
      if (out != null && arr != null) {
         for (BsonValue v : arr) {
            String s = normalizeBsonStringLike(v);
            if (s != null && !s.isBlank()) {
               out.add(s);
            }
         }
      }
   }

   private static String normalizeBsonStringLike(BsonValue value) {
      if (value == null) {
         return null;
      } else {
         return value.isString() ? value.asString().getValue() : normalizeStringLike(value);
      }
   }

   private static String normalizeStringLike(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof CharSequence seq) {
         return seq.toString();
      } else {
         try {
            Method m = value.getClass().getMethod("getId");
            Object v = m.invoke(value);
            if (v != null) {
               return String.valueOf(v);
            }
         } catch (Throwable var4) {
         }

         try {
            Field f = value.getClass().getDeclaredField("id");
            f.setAccessible(true);
            Object v = f.get(value);
            if (v != null) {
               return String.valueOf(v);
            }
         } catch (Throwable var3) {
         }

         return String.valueOf(value);
      }
   }

   private static BsonDocument getDocument(BsonDocument doc, String key) {
      try {
         return doc.containsKey(key) && doc.get(key).isDocument() ? doc.getDocument(key) : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static BsonValue firstValue(BsonDocument doc, String... keys) {
      if (doc != null && keys != null) {
         for (String key : keys) {
            try {
               if (key != null && doc.containsKey(key)) {
                  return doc.get(key);
               }
            } catch (Throwable var7) {
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String getStringAny(BsonDocument doc, String... keys) {
      if (doc != null && keys != null) {
         for (String key : keys) {
            String value = getString(doc, key);
            if (value != null && !value.isBlank()) {
               return value;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static BsonArray getArray(BsonDocument doc, String key) {
      try {
         return doc.containsKey(key) && doc.get(key).isArray() ? doc.getArray(key) : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static String getString(BsonDocument doc, String key) {
      try {
         return doc.containsKey(key) && doc.get(key).isString() ? doc.getString(key).getValue() : null;
      } catch (Throwable ignored) {
         return null;
      }
   }

   record SnapshotCosmetic(String slot, String valueId, String optionId, String variantId, byte[] valueBytes) {
   }

   record SnapshotData(List<AmigoWardrobeSnapshotSupport.SnapshotCosmetic> cosmetics, LinkedHashSet<String> cosmeticIds, LinkedHashSet<String> hiddenTypes) {
      boolean hasAnyData() {
         return this.cosmetics != null && !this.cosmetics.isEmpty()
            || this.cosmeticIds != null && !this.cosmeticIds.isEmpty()
            || this.hiddenTypes != null && !this.hiddenTypes.isEmpty();
      }
   }
}
