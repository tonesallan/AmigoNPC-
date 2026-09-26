package br.tones.amigonpc.core.optional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class AmigoWardrobeCosmeticSupport {
   private AmigoWardrobeCosmeticSupport() {
   }

   static Object tryCreateCosmeticSaveData(String cosmeticId, String optionId, String variantId) {
      if (cosmeticId != null && !cosmeticId.isBlank()) {
         Class<?> saveDataClass;
         try {
            saveDataClass = Class.forName("dev.hardaway.wardrobe.impl.player.CosmeticSaveData");
         } catch (Throwable ignored) {
            return null;
         }

         Object instance = tryInstantiateCosmeticSaveData(saveDataClass, cosmeticId, optionId, variantId);
         if (instance == null) {
            return null;
         }

         applyStringValue(instance, List.of("setCosmeticId", "setId"), List.of("cosmeticId", "id"), cosmeticId);
         applyStringValue(instance, List.of("setOptionId", "setOption"), List.of("optionId", "option"), optionId);
         applyStringValue(instance, List.of("setVariantId", "setVariant"), List.of("variantId", "variant"), variantId);
         return instance;
      } else {
         return null;
      }
   }

   static boolean trySetCosmetic(Object wardrobe, String slot, Object cosmeticSaveData) {
      if (wardrobe != null && slot != null && !slot.isBlank() && cosmeticSaveData != null) {
         for (Method m : wardrobe.getClass().getMethods()) {
            try {
               if (m.getName().equals("setCosmetic") && m.getParameterCount() == 2) {
                  Object slotArg = coerceSlotToType(slot, m.getParameterTypes()[0]);
                  if (slotArg != null && (m.getParameterTypes()[1].isInstance(cosmeticSaveData) || Object.class.equals(m.getParameterTypes()[1]))) {
                     m.invoke(wardrobe, slotArg, cosmeticSaveData);
                     return true;
                  }
               }
            } catch (Throwable var8) {
            }
         }

         return false;
      } else {
         return false;
      }
   }

   static boolean putCosmeticFallback(Map<Object, Object> cosmeticsMap, String slot, Object cosmeticValue) {
      if (cosmeticsMap != null && slot != null && !slot.isBlank() && cosmeticValue != null) {
         Object key = slot;
         if (!cosmeticsMap.isEmpty()) {
            Object sampleKey = cosmeticsMap.keySet().iterator().next();
            Object convertedKey = coerceSlotToType(slot, sampleKey != null ? sampleKey.getClass() : String.class);
            if (convertedKey != null) {
               key = convertedKey;
            }
         }

         try {
            cosmeticsMap.put(key, cosmeticValue);
            return true;
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isCosmeticEquipped(Object wardrobe, Class<?> pwcClass, String slot, String expectedCosmeticId) {
      if (wardrobe != null && pwcClass != null && slot != null && !slot.isBlank()) {
         Map<?, ?> cosmeticsMap = AmigoWardrobeValueSupport.asMap(AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "cosmetics"));
         if (cosmeticsMap != null && !cosmeticsMap.isEmpty()) {
            Object equipped = cosmeticsMap.get(slot);
            if (equipped == null) {
               for (Entry<?, ?> entry : cosmeticsMap.entrySet()) {
                  if (entry != null && entry.getKey() != null && slot.equals(String.valueOf(entry.getKey()))) {
                     equipped = entry.getValue();
                     break;
                  }
               }
            }

            if (equipped == null) {
               return false;
            } else if (expectedCosmeticId != null && !expectedCosmeticId.isBlank()) {
               String actualId = AmigoWardrobeValueSupport.tryExtractStringValue(equipped, List.of("getCosmeticId", "getId"), List.of("cosmeticId", "id"));
               return actualId == null || actualId.isBlank() || expectedCosmeticId.equals(actualId);
            } else {
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Object tryInstantiateCosmeticSaveData(Class<?> saveDataClass, String cosmeticId, String optionId, String variantId) {
      try {
         Constructor<?> ctor = saveDataClass.getDeclaredConstructor();
         ctor.setAccessible(true);
         return ctor.newInstance();
      } catch (Throwable var11) {
         for (Constructor<?> ctor : saveDataClass.getDeclaredConstructors()) {
            try {
               ctor.setAccessible(true);
               Object[] args = tryBuildCosmeticSaveDataArgs(ctor.getParameterTypes(), cosmeticId, optionId, variantId);
               if (args != null) {
                  return ctor.newInstance(args);
               }
            } catch (Throwable var9) {
            }
         }

         for (Method m : saveDataClass.getMethods()) {
            try {
               if (Modifier.isStatic(m.getModifiers())
                  && (m.getName().equals("of") || m.getName().equals("create") || m.getName().equals("from") || m.getName().equals("fromIds"))) {
                  Object[] args = tryBuildCosmeticSaveDataArgs(m.getParameterTypes(), cosmeticId, optionId, variantId);
                  if (args != null) {
                     return m.invoke(null, args);
                  }
               }
            } catch (Throwable var10) {
            }
         }

         return null;
      }
   }

   private static Object[] tryBuildCosmeticSaveDataArgs(Class<?>[] parameterTypes, String cosmeticId, String optionId, String variantId) {
      if (parameterTypes != null && parameterTypes.length != 0 && parameterTypes.length <= 3) {
         List<String> rawValues = new ArrayList<>();
         rawValues.add(cosmeticId);
         if (optionId != null && !optionId.isBlank()) {
            rawValues.add(optionId);
         }

         if (variantId != null && !variantId.isBlank()) {
            rawValues.add(variantId);
         }

         while (rawValues.size() < parameterTypes.length) {
            rawValues.add(null);
         }

         if (parameterTypes.length > rawValues.size()) {
            return null;
         }

         Object[] args = new Object[parameterTypes.length];

         for (int i = 0; i < parameterTypes.length; i++) {
            String raw = rawValues.get(i);
            if (raw == null && i == 0) {
               return null;
            }

            Object converted = coerceNullableStringToType(raw, parameterTypes[i]);
            if (converted == null && raw != null) {
               return null;
            }

            args[i] = converted;
         }

         return args;
      } else {
         return null;
      }
   }

   private static Object coerceSlotToType(String slot, Class<?> targetType) {
      return coerceStringToType(slot, targetType);
   }

   private static void applyStringValue(Object target, List<String> setterNames, List<String> fieldNames, String rawValue) {
      if (target != null && rawValue != null && !rawValue.isBlank()) {
         for (String setterName : setterNames) {
            for (Method m : target.getClass().getMethods()) {
               try {
                  if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                     Object converted = coerceStringToType(rawValue, m.getParameterTypes()[0]);
                     if (converted != null) {
                        m.invoke(target, converted);
                        return;
                     }
                  }
               } catch (Throwable var12) {
               }
            }
         }

         for (String fieldName : fieldNames) {
            try {
               Field f = target.getClass().getDeclaredField(fieldName);
               f.setAccessible(true);
               Object converted = coerceStringToType(rawValue, f.getType());
               if (converted != null) {
                  f.set(target, converted);
                  return;
               }
            } catch (Throwable var11) {
            }
         }
      }
   }

   private static Object coerceStringToType(String rawValue, Class<?> targetType) {
      if (rawValue != null && !rawValue.isBlank() && targetType != null) {
         try {
            if (targetType == String.class || CharSequence.class.isAssignableFrom(targetType) || Object.class == targetType) {
               return rawValue;
            }

            if (targetType.isEnum()) {
               return Enum.valueOf(targetType.asSubclass(Enum.class), rawValue);
            }

            try {
               Method valueOf = targetType.getMethod("valueOf", String.class);
               if (Modifier.isStatic(valueOf.getModifiers())) {
                  return valueOf.invoke(null, rawValue);
               }
            } catch (Throwable var6) {
            }

            try {
               return targetType.getConstructor(String.class).newInstance(rawValue);
            } catch (Throwable var7) {
               try {
                  Method getAssetMap = targetType.getMethod("getAssetMap");
                  Object assetMap = getAssetMap.invoke(null);
                  Object asset = findInMap(assetMap, rawValue);
                  if (asset != null && targetType.isInstance(asset)) {
                     return asset;
                  }
               } catch (Throwable var5) {
               }
            }
         } catch (Throwable var8) {
         }

         return null;
      } else {
         return null;
      }
   }

   private static Object coerceNullableStringToType(String rawValue, Class<?> targetType) {
      if (targetType == null) {
         return null;
      }

      if (rawValue != null && !rawValue.isBlank()) {
         return coerceStringToType(rawValue, targetType);
      }

      if (!targetType.isPrimitive()) {
         return null;
      }

      return switch (targetType.getName()) {
         case "boolean" -> false;
         case "byte" -> (byte)0;
         case "short" -> (short)0;
         case "int" -> 0;
         case "long" -> 0L;
         case "float" -> 0.0F;
         case "double" -> 0.0;
         case "char" -> '\u0000';
         default -> null;
      };
   }

   private static Object findInMap(Object mapObj, String key) {
      if (mapObj instanceof Map<?, ?> map) {
         if (map.containsKey(key)) {
            return map.get(key);
         }

         for (Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() != null && key.equals(String.valueOf(e.getKey()))) {
               return e.getValue();
            }
         }
      }

      return null;
   }
}
