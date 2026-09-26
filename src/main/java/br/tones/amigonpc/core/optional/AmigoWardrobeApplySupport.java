package br.tones.amigonpc.core.optional;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

final class AmigoWardrobeApplySupport {
   private AmigoWardrobeApplySupport() {
   }

   static void clearWardrobeFields(Object wardrobe, Class<?> pwcClass) {
      if (wardrobe != null && pwcClass != null) {
         for (String fieldName : List.of("cosmetics", "cosmeticIdSet", "hiddenCosmeticTypes")) {
            try {
               Field f = pwcClass.getDeclaredField(fieldName);
               f.setAccessible(true);
               Object current = f.get(wardrobe);
               if (current instanceof Map<?, ?> map) {
                  map.clear();
               } else if (current instanceof Collection<?> col) {
                  col.clear();
               } else {
                  f.set(wardrobe, null);
               }
            } catch (Throwable var8) {
            }
         }
      }
   }

   static boolean copyWardrobeFields(Object source, Object target, Class<?> pwcClass) {
      boolean ok = false;

      for (String fieldName : List.of("cosmetics", "cosmeticIdSet", "hiddenCosmeticTypes")) {
         try {
            Field f = pwcClass.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object value = f.get(source);
            Object current = f.get(target);
            if (value instanceof Map<?, ?> src && current instanceof Map<?, ?> dstRaw) {
               Map<Object, Object> dst = (Map<Object, Object>)dstRaw;
               dst.clear();

               for (Entry<?, ?> e : src.entrySet()) {
                  dst.put(e.getKey(), e.getValue());
               }

               ok = true;
            } else if (value instanceof Collection<?> src && current instanceof Collection<?> dstRaw) {
               Collection<Object> dst = (Collection<Object>)dstRaw;
               dst.clear();
               dst.addAll(new ArrayList<>((Collection<? extends Object>)src));
               ok = true;
            } else if (value != null) {
               f.set(target, value);
               ok = true;
            }
         } catch (Throwable var16) {
         }
      }

      return ok;
   }

   static boolean applyStructuredSnapshot(Object wardrobe, Class<?> pwcClass, AmigoWardrobeSnapshotSupport.SnapshotData data) {
      if (wardrobe != null && pwcClass != null && data != null && data.hasAnyData()) {
         clearWardrobeFields(wardrobe, pwcClass);
         Map<Object, Object> cosmeticsMap = AmigoWardrobeValueSupport.asMutableMap(
            AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "cosmetics")
         );
         int equippedCount = 0;

         for (AmigoWardrobeSnapshotSupport.SnapshotCosmetic item : data.cosmetics()) {
            if (item != null && item.slot() != null && !item.slot().isBlank()) {
               Object cosmeticSaveData = AmigoWardrobeCosmeticSupport.tryCreateCosmeticSaveData(item.valueId(), item.optionId(), item.variantId());
               Object fallbackValue = cosmeticSaveData;
               if (fallbackValue == null && item.valueBytes() != null && item.valueBytes().length > 0) {
                  fallbackValue = AmigoWardrobeValueSupport.tryDeserializeJava(item.valueBytes());
               }

               if (cosmeticSaveData != null
                  && AmigoWardrobeCosmeticSupport.trySetCosmetic(wardrobe, item.slot(), cosmeticSaveData)
                  && AmigoWardrobeCosmeticSupport.isCosmeticEquipped(wardrobe, pwcClass, item.slot(), item.valueId())) {
                  equippedCount++;
               } else if (fallbackValue != null
                  && cosmeticsMap != null
                  && AmigoWardrobeCosmeticSupport.putCosmeticFallback(cosmeticsMap, item.slot(), fallbackValue)
                  && AmigoWardrobeCosmeticSupport.isCosmeticEquipped(wardrobe, pwcClass, item.slot(), item.valueId())) {
                  equippedCount++;
               }
            }
         }

         if (equippedCount <= 0) {
            return false;
         }

         Set<Object> cosmeticIdSet = AmigoWardrobeValueSupport.asMutableSet(AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "cosmeticIdSet"));
         if (cosmeticIdSet != null) {
            cosmeticIdSet.clear();
            Map<?, ?> equippedCosmetics = AmigoWardrobeValueSupport.asMap(AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "cosmetics"));
            if (equippedCosmetics != null) {
               for (Object equipped : equippedCosmetics.values()) {
                  String equippedId = AmigoWardrobeValueSupport.tryExtractStringValue(equipped, List.of("getCosmeticId", "getId"), List.of("cosmeticId", "id"));
                  if (equippedId != null && !equippedId.isBlank()) {
                     cosmeticIdSet.add(equippedId);
                  }
               }
            }
         }

         Set<Object> hiddenTypeSet = AmigoWardrobeValueSupport.asMutableSet(
            AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "hiddenCosmeticTypes")
         );
         if (hiddenTypeSet != null) {
            hiddenTypeSet.clear();

            for (String hidden : data.hiddenTypes()) {
               if (hidden != null && !hidden.isBlank()) {
                  hiddenTypeSet.add(hidden);
               }
            }
         }

         AmigoWardrobeModelSupport.forceWardrobeVisualRefresh(wardrobe);
         return true;
      } else {
         return false;
      }
   }
}
