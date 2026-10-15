package br.tones.amigonpc.core.optional;

import br.tones.amigonpc.core.AmigoPersistence;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSettings;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

public final class AmigoWardrobePersistence {
   private static final String CLS_PLAYER_WARDROBE_COMPONENT = "dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent";
   private static final String CLS_PLAYER_WARDROBE_API = "dev.hardaway.wardrobe.api.player.PlayerWardrobe";
   private static final String CLS_WARDROBE_SYSTEM_TICK = "dev.hardaway.wardrobe.impl.player.PlayerWardrobeSystems$Tick";
   private static final String KEY_COMPONENT_BYTES = "componentBytes";
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

   private AmigoWardrobePersistence() {
   }

   public static boolean saveNpcWardrobe(UUID ownerId, Store store, Ref npcRef) {
      if (ownerId != null && store != null && npcRef != null) {
         try {
            Class<?> pwcClass = tryLoadWardrobeClass();
            if (pwcClass == null) {
               return false;
            } else {
               Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke((Object)null);
               Object wardrobe = invokeStoreComponentMethod(store, "getComponent", npcRef, wardrobeComponentType);
               if (wardrobe == null) {
                  return false;
               } else {
                  BsonDocument snapshot = snapshotWardrobe(wardrobe, pwcClass);
                  if (snapshot != null && !snapshot.isEmpty()) {
                     AmigoPersistence.saveWardrobeState(ownerId, snapshot);
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         } catch (Throwable var7) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean hasSavedCosmetics(UUID ownerId) {
      if (ownerId == null) {
         return false;
      } else {
         try {
            BsonDocument snapshot = AmigoPersistence.loadWardrobeState(ownerId);
            return hasSnapshotCosmetics(snapshot);
         } catch (Throwable var2) {
            return false;
         }
      }
   }

   public static boolean restoreNpcWardrobe(UUID ownerId, Store store, Ref ownerRef, Ref npcRef) {
      if (ownerId != null && store != null && npcRef != null) {
         try {
            Class<?> pwcClass = tryLoadWardrobeClass();
            if (pwcClass == null) {
               return false;
            } else {
               BsonDocument snapshot = AmigoPersistence.loadWardrobeState(ownerId);
               if (!hasSnapshotCosmetics(snapshot)) {
                  forceDefaultWardrobe(store, npcRef);
                  return false;
               } else {
                  Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke((Object)null);
                  Object wardrobe = invokeStoreComponentMethod(store, "ensureAndGetComponent", npcRef, wardrobeComponentType);
                  if (wardrobe == null) {
                     forceDefaultWardrobe(store, npcRef);
                     return false;
                  } else {
                     boolean applied = applySnapshot(wardrobe, snapshot, pwcClass);
                     if (!applied) {
                        clearWardrobeFields(wardrobe, pwcClass);
                        forceWardrobeVisualRefresh(wardrobe);
                        putOrSetComponent(store, npcRef, wardrobeComponentType, wardrobe);
                        return false;
                     } else {
                        forceWardrobeVisualRefresh(wardrobe);
                        putOrSetComponent(store, npcRef, wardrobeComponentType, wardrobe);
                        tryApplyWardrobeModelTo(store, ownerRef, npcRef, wardrobe);
                        return true;
                     }
                  }
               }
            }
         } catch (Throwable var9) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean restoreNpcWardrobe(UUID ownerId, Store store, Ref npcRef) {
      return restoreNpcWardrobe(ownerId, store, (Ref)null, npcRef);
   }

   public static void forceDefaultWardrobe(Store store, Ref npcRef) {
      if (store != null && npcRef != null) {
         try {
            Class<?> pwcClass = tryLoadWardrobeClass();
            if (pwcClass == null) {
               return;
            }

            Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke((Object)null);
            Object wardrobe = invokeStoreComponentMethod(store, "ensureAndGetComponent", npcRef, wardrobeComponentType);
            if (wardrobe == null) {
               return;
            }

            clearWardrobeFields(wardrobe, pwcClass);
            forceWardrobeVisualRefresh(wardrobe);
            putOrSetComponent(store, npcRef, wardrobeComponentType, wardrobe);
         } catch (Throwable var5) {
         }

      }
   }

   public static Object buildOwnerWardrobeModel(Store store, Ref ownerRef) {
      if (store != null && ownerRef != null) {
         try {
            Class<?> pwcClass = tryLoadWardrobeClass();
            if (pwcClass == null) {
               return null;
            } else {
               Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke((Object)null);
               Object wardrobe = invokeStoreComponentMethod(store, "ensureAndGetComponent", ownerRef, wardrobeComponentType);
               if (wardrobe == null) {
                  return null;
               } else {
                  Player ownerPlayer = (Player)store.getComponent(ownerRef, Player.getComponentType());
                  PlayerSettings settings = (PlayerSettings)store.getComponent(ownerRef, PlayerSettings.getComponentType());
                  PlayerSkinComponent skin = (PlayerSkinComponent)store.getComponent(ownerRef, PlayerSkinComponent.getComponentType());
                  PlayerRef ownerPlayerRef = (PlayerRef)store.getComponent(ownerRef, PlayerRef.getComponentType());
                  if (ownerPlayer != null && settings != null && skin != null && ownerPlayerRef != null) {
                     Class<?> tickSystemClass = Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeSystems$Tick");
                     Class<?> playerWardrobeApiClass = Class.forName("dev.hardaway.wardrobe.api.player.PlayerWardrobe");
                     Method buildWardrobeModel = findBuildWardrobeModel(tickSystemClass, playerWardrobeApiClass);
                     return buildWardrobeModel == null ? null : buildWardrobeModel.invoke((Object)null, ownerPlayer, settings, skin.getPlayerSkin(), wardrobe, ownerPlayerRef);
                  } else {
                     return null;
                  }
               }
            }
         } catch (Throwable var12) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Class tryLoadWardrobeClass() {
      try {
         return Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent");
      } catch (Throwable var3) {
         try {
            return Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent", true, Thread.currentThread().getContextClassLoader());
         } catch (Throwable var2) {
            return null;
         }
      }
   }

   private static boolean hasSnapshotCosmetics(BsonDocument snapshot) {
      if (snapshot != null && !snapshot.isEmpty()) {
         SnapshotData data = extractSnapshotData(snapshot);
         return data != null && data.hasAnyData();
      } else {
         return false;
      }
   }

   private static BsonDocument snapshotWardrobe(Object wardrobe, Class pwcClass) {
      if (wardrobe == null) {
         return null;
      } else {
         BsonDocument out = new BsonDocument();
         byte[] fullBytes = trySerializeJava(safeClone(wardrobe));
         if (fullBytes == null) {
            fullBytes = trySerializeJava(wardrobe);
         }

         if (fullBytes != null && fullBytes.length > 0) {
            out.put("componentBytes", new BsonBinary(fullBytes));
         }

         Map<?, ?> cosmetics = asMap(readFieldBestEffort(pwcClass, wardrobe, "cosmetics"));
         BsonArray cosmeticsArray = new BsonArray();
         BsonDocument wardrobeCosmeticsMap = new BsonDocument();
         LinkedHashSet<String> cosmeticIds = toStringSet(readFieldBestEffort(pwcClass, wardrobe, "cosmeticIdSet"));
         if (cosmetics != null && !cosmetics.isEmpty()) {
            for(Map.Entry e : cosmetics.entrySet()) {
               if (e != null && e.getKey() != null && e.getValue() != null) {
                  String slot = String.valueOf(e.getKey());
                  Object value = e.getValue();
                  byte[] valueBytes = trySerializeJava(value);
                  String valueId = tryExtractId(value);
                  String optionId = tryExtractOptionId(value);
                  String variantId = tryExtractVariantId(value);
                  BsonDocument item = new BsonDocument();
                  item.put("slot", new BsonString(slot));
                  item.put("Slot", new BsonString(slot));
                  if (valueBytes != null && valueBytes.length > 0) {
                     item.put("valueBytes", new BsonBinary(valueBytes));
                  }

                  if (valueId != null && !valueId.isBlank()) {
                     item.put("valueId", new BsonString(valueId));
                     item.put("Id", new BsonString(valueId));
                     cosmeticIds.add(valueId);
                  }

                  if (optionId != null && !optionId.isBlank()) {
                     item.put("optionId", new BsonString(optionId));
                     item.put("Option", new BsonString(optionId));
                  }

                  if (variantId != null && !variantId.isBlank()) {
                     item.put("variantId", new BsonString(variantId));
                     item.put("Variant", new BsonString(variantId));
                  }

                  if (!item.isEmpty()) {
                     cosmeticsArray.add(item);
                  }

                  BsonDocument wardrobeCosmetic = new BsonDocument();
                  if (valueId != null && !valueId.isBlank()) {
                     wardrobeCosmetic.put("Id", new BsonString(valueId));
                  }

                  if (optionId != null && !optionId.isBlank()) {
                     wardrobeCosmetic.put("Option", new BsonString(optionId));
                  }

                  if (variantId != null && !variantId.isBlank()) {
                     wardrobeCosmetic.put("Variant", new BsonString(variantId));
                  }

                  if (!wardrobeCosmetic.isEmpty()) {
                     wardrobeCosmeticsMap.put(slot, wardrobeCosmetic);
                  }
               }
            }
         }

         if (!cosmeticsArray.isEmpty()) {
            out.put("cosmetics", cosmeticsArray);
         }

         if (!cosmeticIds.isEmpty()) {
            BsonArray arr = new BsonArray();

            for(String s : cosmeticIds) {
               arr.add(new BsonString(s));
            }

            out.put("cosmeticIds", arr);
         }

         LinkedHashSet<String> hiddenTypes = toStringSet(readFieldBestEffort(pwcClass, wardrobe, "hiddenCosmeticTypes"));
         BsonArray hiddenArr = new BsonArray();

         for(String s : hiddenTypes) {
            hiddenArr.add(new BsonString(s));
         }

         out.put("hiddenCosmeticTypes", hiddenArr);
         if (!hiddenTypes.isEmpty()) {
            out.put("hiddenTypes", hiddenArr);
         }

         BsonDocument playerWardrobe = new BsonDocument();
         playerWardrobe.put("Cosmetics", wardrobeCosmeticsMap);
         playerWardrobe.put("HiddenCosmeticTypes", hiddenArr);
         out.put("PlayerWardrobe", playerWardrobe);
         return out;
      }
   }

   private static boolean applySnapshot(Object wardrobe, BsonDocument snapshot, Class pwcClass) {
      if (wardrobe != null && snapshot != null && !snapshot.isEmpty()) {
         SnapshotData data = extractSnapshotData(snapshot);
         if (data != null && data.hasAnyData()) {
            boolean applied = applyStructuredSnapshot(wardrobe, pwcClass, data);
            if (applied) {
               return true;
            }
         }

         if (snapshot.containsKey("componentBytes") && snapshot.get("componentBytes").isBinary()) {
            Object restored = tryDeserializeJava(snapshot.getBinary("componentBytes").getData());
            if (restored != null && pwcClass.isInstance(restored) && copyWardrobeFields(restored, wardrobe, pwcClass)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static void clearWardrobeFields(Object wardrobe, Class pwcClass) {
      if (wardrobe != null && pwcClass != null) {
         for(String fieldName : List.of("cosmetics", "cosmeticIdSet", "hiddenCosmeticTypes")) {
            try {
               Field f = pwcClass.getDeclaredField(fieldName);
               f.setAccessible(true);
               Object current = f.get(wardrobe);
               if (current instanceof Map) {
                  Map<?, ?> map = (Map)current;
                  map.clear();
               } else if (current instanceof Collection) {
                  Collection<?> col = (Collection)current;
                  col.clear();
               } else {
                  f.set(wardrobe, (Object)null);
               }
            } catch (Throwable var8) {
            }
         }

      }
   }

   private static boolean copyWardrobeFields(Object source, Object target, Class pwcClass) {
      boolean ok = false;

      for(String fieldName : List.of("cosmetics", "cosmeticIdSet", "hiddenCosmeticTypes")) {
         try {
            Field f = pwcClass.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object value = f.get(source);
            Object current = f.get(target);
            if (value instanceof Map src) {
               if (current instanceof Map dstRaw) {
                  Map<Object, Object> dst = dstRaw;
                  dstRaw.clear();

                  for(Map.Entry e : src.entrySet()) {
                     dst.put(e.getKey(), e.getValue());
                  }

                  ok = true;
                  continue;
               }
            }

            if (value instanceof Collection src) {
               if (current instanceof Collection dstRaw) {
                  dstRaw.clear();
                  dstRaw.addAll(new ArrayList(src));
                  ok = true;
                  continue;
               }
            }

            if (value != null) {
               f.set(target, value);
               ok = true;
            }
         } catch (Throwable var16) {
         }
      }

      return ok;
   }

   private static Object safeClone(Object component) {
      if (component == null) {
         return null;
      } else {
         for(String name : List.of("cloneSerializable", "clone")) {
            try {
               Method m = component.getClass().getMethod(name);
               m.setAccessible(true);
               return m.invoke(component);
            } catch (Throwable var4) {
            }
         }

         return null;
      }
   }

   private static Object readFieldBestEffort(Class type, Object instance, String fieldName) {
      try {
         Field f = type.getDeclaredField(fieldName);
         f.setAccessible(true);
         return f.get(instance);
      } catch (Throwable var4) {
         return null;
      }
   }

   private static Map asMap(Object value) {
      Map var10000;
      if (value instanceof Map m) {
         var10000 = m;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   private static Map asMutableMap(Object value) {
      Map var10000;
      if (value instanceof Map m) {
         var10000 = m;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   private static Set asMutableSet(Object value) {
      Set var10000;
      if (value instanceof Set s) {
         var10000 = s;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   private static LinkedHashSet toStringSet(Object value) {
      LinkedHashSet<String> out = new LinkedHashSet();
      if (value instanceof Iterable) {
         for(Object v : (Iterable)value) {
            String s = normalizeStringLike(v);
            if (s != null && !s.isBlank()) {
               out.add(s);
            }
         }
      }

      return out;
   }

   private static byte[] trySerializeJava(Object value) {
      if (value == null) {
         return null;
      } else {
         try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            try {
               oos.writeObject(value);
            } catch (Throwable var6) {
               try {
                  oos.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }

               throw var6;
            }

            oos.close();
            return bos.toByteArray();
         } catch (Throwable var7) {
            return null;
         }
      }
   }

   private static Object tryDeserializeJava(byte[] bytes) {
      if (bytes != null && bytes.length != 0) {
         try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));

            Object var2;
            try {
               var2 = ois.readObject();
            } catch (Throwable var5) {
               try {
                  ois.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }

               throw var5;
            }

            ois.close();
            return var2;
         } catch (Throwable var6) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String tryExtractId(Object value) {
      if (value == null) {
         return null;
      } else {
         for(String name : List.of("getId", "getCosmeticId")) {
            try {
               Method m = value.getClass().getMethod(name);
               Object v = m.invoke(value);
               if (v != null) {
                  return String.valueOf(v);
               }
            } catch (Throwable var6) {
            }
         }

         for(String name : List.of("id", "cosmeticId")) {
            try {
               Field f = value.getClass().getDeclaredField(name);
               f.setAccessible(true);
               Object v = f.get(value);
               if (v != null) {
                  return String.valueOf(v);
               }
            } catch (Throwable var5) {
            }
         }

         return null;
      }
   }

   private static String tryExtractOptionId(Object value) {
      return tryExtractStringValue(value, List.of("getOptionId", "getOption"), List.of("optionId", "option"));
   }

   private static String tryExtractVariantId(Object value) {
      return tryExtractStringValue(value, List.of("getVariantId", "getVariant"), List.of("variantId", "variant"));
   }

   private static String tryExtractStringValue(Object value, List methodNames, List fieldNames) {
      if (value == null) {
         return null;
      } else {
         for(String name : methodNames) {
            try {
               Method m = value.getClass().getMethod(name);
               Object v = m.invoke(value);
               String resolved = normalizeStringLike(v);
               if (resolved != null && !resolved.isBlank()) {
                  return resolved;
               }
            } catch (Throwable var9) {
            }
         }

         for(String name : fieldNames) {
            try {
               Field f = value.getClass().getDeclaredField(name);
               f.setAccessible(true);
               Object v = f.get(value);
               String resolved = normalizeStringLike(v);
               if (resolved != null && !resolved.isBlank()) {
                  return resolved;
               }
            } catch (Throwable var8) {
            }
         }

         return null;
      }
   }

   private static String normalizeStringLike(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof CharSequence) {
         CharSequence seq = (CharSequence)value;
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

   private static Object tryBuildPlayerCosmeticFromId(String valueId, String optionId, String variantId) {
      if (valueId != null && !valueId.isBlank()) {
         try {
            Class<?> cosmeticAssetClass = Class.forName("dev.hardaway.wardrobe.impl.cosmetic.CosmeticAsset");
            Object assetMap = cosmeticAssetClass.getMethod("getAssetMap").invoke((Object)null);
            Object asset = findInMap(assetMap, valueId);
            if (asset == null) {
               return null;
            }

            for(String clsName : List.of("dev.hardaway.wardrobe.impl.player.CosmeticSaveData", "dev.hardaway.wardrobe.impl.player.PlayerCosmeticImpl", "dev.hardaway.wardrobe.api.player.PlayerCosmetic")) {
               try {
                  Class<?> c = Class.forName(clsName);
                  Object cosmetic = tryInstantiateCosmetic(c, asset, optionId, variantId);
                  if (cosmetic != null) {
                     return cosmetic;
                  }
               } catch (Throwable var10) {
               }
            }
         } catch (Throwable var11) {
         }

         return null;
      } else {
         return null;
      }
   }

   private static Object tryCreateCosmeticSaveData(String cosmeticId, String optionId, String variantId) {
      if (cosmeticId != null && !cosmeticId.isBlank()) {
         Class<?> saveDataClass;
         try {
            saveDataClass = Class.forName("dev.hardaway.wardrobe.impl.player.CosmeticSaveData");
         } catch (Throwable var5) {
            return null;
         }

         Object instance = tryInstantiateCosmeticSaveData(saveDataClass, cosmeticId, optionId, variantId);
         if (instance == null) {
            return null;
         } else {
            applyStringValue(instance, List.of("setCosmeticId", "setId"), List.of("cosmeticId", "id"), cosmeticId);
            applyStringValue(instance, List.of("setOptionId", "setOption"), List.of("optionId", "option"), optionId);
            applyStringValue(instance, List.of("setVariantId", "setVariant"), List.of("variantId", "variant"), variantId);
            return instance;
         }
      } else {
         return null;
      }
   }

   private static Object tryInstantiateCosmeticSaveData(Class saveDataClass, String cosmeticId, String optionId, String variantId) {
      try {
         Constructor<?> ctor = saveDataClass.getDeclaredConstructor();
         ctor.setAccessible(true);
         return ctor.newInstance();
      } catch (Throwable var11) {
         for(Constructor ctor : saveDataClass.getDeclaredConstructors()) {
            try {
               ctor.setAccessible(true);
               Object[] args = tryBuildCosmeticSaveDataArgs(ctor.getParameterTypes(), cosmeticId, optionId, variantId);
               if (args != null) {
                  return ctor.newInstance(args);
               }
            } catch (Throwable var9) {
            }
         }

         for(Method m : saveDataClass.getMethods()) {
            try {
               if (Modifier.isStatic(m.getModifiers()) && (m.getName().equals("of") || m.getName().equals("create") || m.getName().equals("from") || m.getName().equals("fromIds"))) {
                  Object[] args = tryBuildCosmeticSaveDataArgs(m.getParameterTypes(), cosmeticId, optionId, variantId);
                  if (args != null) {
                     return m.invoke((Object)null, args);
                  }
               }
            } catch (Throwable var10) {
            }
         }

         return null;
      }
   }

   private static Object[] tryBuildCosmeticSaveDataArgs(Class[] parameterTypes, String cosmeticId, String optionId, String variantId) {
      if (parameterTypes != null && parameterTypes.length != 0 && parameterTypes.length <= 3) {
         List<String> rawValues = new ArrayList();
         rawValues.add(cosmeticId);
         if (optionId != null && !optionId.isBlank()) {
            rawValues.add(optionId);
         }

         if (variantId != null && !variantId.isBlank()) {
            rawValues.add(variantId);
         }

         while(rawValues.size() < parameterTypes.length) {
            rawValues.add((Object)null);
         }

         if (parameterTypes.length > rawValues.size()) {
            return null;
         } else {
            Object[] args = new Object[parameterTypes.length];

            for(int i = 0; i < parameterTypes.length; ++i) {
               String raw = (String)rawValues.get(i);
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
         }
      } else {
         return null;
      }
   }

   private static boolean trySetCosmetic(Object wardrobe, String slot, Object cosmeticSaveData) {
      if (wardrobe != null && slot != null && !slot.isBlank() && cosmeticSaveData != null) {
         for(Method m : wardrobe.getClass().getMethods()) {
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

   private static Object coerceSlotToType(String slot, Class targetType) {
      return coerceStringToType(slot, targetType);
   }

   private static boolean putCosmeticFallback(Map cosmeticsMap, String slot, Object cosmeticValue) {
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
         } catch (Throwable var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean isCosmeticEquipped(Object wardrobe, Class pwcClass, String slot, String expectedCosmeticId) {
      if (wardrobe != null && pwcClass != null && slot != null && !slot.isBlank()) {
         Map<?, ?> cosmeticsMap = asMap(readFieldBestEffort(pwcClass, wardrobe, "cosmetics"));
         if (cosmeticsMap != null && !cosmeticsMap.isEmpty()) {
            Object equipped = cosmeticsMap.get(slot);
            if (equipped == null) {
               for(Map.Entry entry : cosmeticsMap.entrySet()) {
                  if (entry != null && entry.getKey() != null && slot.equals(String.valueOf(entry.getKey()))) {
                     equipped = entry.getValue();
                     break;
                  }
               }
            }

            if (equipped == null) {
               return false;
            } else if (expectedCosmeticId != null && !expectedCosmeticId.isBlank()) {
               String actualId = tryExtractStringValue(equipped, List.of("getCosmeticId", "getId"), List.of("cosmeticId", "id"));
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

   private static Object tryInstantiateCosmetic(Class cosmeticClass, Object asset, String optionId, String variantId) {
      Object cosmetic = tryInstantiateCosmeticViaFactories(cosmeticClass, asset, optionId, variantId);
      if (cosmetic == null) {
         cosmetic = tryInstantiateCosmeticViaFactories(cosmeticClass, asset, (String)null, (String)null);
      }

      if (cosmetic == null) {
         return null;
      } else {
         applyCosmeticMetadata(cosmetic, optionId, variantId);
         return cosmetic;
      }
   }

   private static Object tryInstantiateCosmeticViaFactories(Class cosmeticClass, Object asset, String optionId, String variantId) {
      for(Method m : cosmeticClass.getMethods()) {
         try {
            if (Modifier.isStatic(m.getModifiers()) && (m.getName().equals("fromAsset") || m.getName().equals("of") || m.getName().equals("create"))) {
               Object[] args = tryBuildFactoryArgs(m.getParameterTypes(), asset, optionId, variantId);
               if (args != null) {
                  return m.invoke((Object)null, args);
               }
            }
         } catch (Throwable var10) {
         }
      }

      for(Constructor ctor : cosmeticClass.getConstructors()) {
         try {
            Object[] args = tryBuildFactoryArgs(ctor.getParameterTypes(), asset, optionId, variantId);
            if (args != null) {
               return ctor.newInstance(args);
            }
         } catch (Throwable var9) {
         }
      }

      return null;
   }

   private static Object[] tryBuildFactoryArgs(Class[] parameterTypes, Object asset, String optionId, String variantId) {
      if (parameterTypes != null && parameterTypes.length != 0 && parameterTypes.length <= 3) {
         Object[] args = new Object[parameterTypes.length];
         boolean assetUsed = false;
         int stringOrdinal = 0;

         for(int i = 0; i < parameterTypes.length; ++i) {
            Class<?> pt = parameterTypes[i];
            if (!assetUsed && asset != null && pt.isInstance(asset)) {
               args[i] = asset;
               assetUsed = true;
            } else {
               String raw = stringOrdinal == 0 ? optionId : variantId;
               Object converted = coerceStringToType(raw, pt);
               if (converted == null) {
                  raw = stringOrdinal == 0 ? variantId : optionId;
                  converted = coerceStringToType(raw, pt);
               }

               if (converted == null) {
                  return null;
               }

               args[i] = converted;
               ++stringOrdinal;
            }
         }

         return assetUsed ? args : null;
      } else {
         return null;
      }
   }

   private static void applyCosmeticMetadata(Object cosmetic, String optionId, String variantId) {
      if (cosmetic != null) {
         applyStringValue(cosmetic, List.of("setOptionId", "setOption"), List.of("optionId", "option"), optionId);
         applyStringValue(cosmetic, List.of("setVariantId", "setVariant"), List.of("variantId", "variant"), variantId);
      }
   }

   private static void applyStringValue(Object target, List setterNames, List fieldNames, String rawValue) {
      if (target != null && rawValue != null && !rawValue.isBlank()) {
         for(String setterName : setterNames) {
            for(Method m : target.getClass().getMethods()) {
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

         for(String fieldName : fieldNames) {
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

   private static Object coerceStringToType(String rawValue, Class targetType) {
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
                  return valueOf.invoke((Object)null, rawValue);
               }
            } catch (Throwable var6) {
            }

            try {
               return targetType.getConstructor(String.class).newInstance(rawValue);
            } catch (Throwable var7) {
               try {
                  Method getAssetMap = targetType.getMethod("getAssetMap");
                  Object assetMap = getAssetMap.invoke((Object)null);
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

   private static Object coerceNullableStringToType(String rawValue, Class targetType) {
      if (targetType == null) {
         return null;
      } else if (rawValue != null && !rawValue.isBlank()) {
         return coerceStringToType(rawValue, targetType);
      } else if (!targetType.isPrimitive()) {
         return null;
      } else {
         Object var10000;
         switch (targetType.getName()) {
            case "boolean" -> var10000 = false;
            case "byte" -> var10000 = 0;
            case "short" -> var10000 = Short.valueOf((short)0);
            case "int" -> var10000 = 0;
            case "long" -> var10000 = 0L;
            case "float" -> var10000 = 0.0F;
            case "double" -> var10000 = (double)0.0F;
            case "char" -> var10000 = '\u0000';
            default -> var10000 = null;
         }

         return var10000;
      }
   }

   private static Object findInMap(Object mapObj, String key) {
      if (mapObj instanceof Map map) {
         if (map.containsKey(key)) {
            return map.get(key);
         }

         for(Map.Entry e : map.entrySet()) {
            if (e.getKey() != null && key.equals(String.valueOf(e.getKey()))) {
               return e.getValue();
            }
         }
      }

      return null;
   }

   private static SnapshotData extractSnapshotData(BsonDocument snapshot) {
      LinkedHashMap<String, SnapshotCosmetic> cosmeticsBySlot = new LinkedHashMap();
      LinkedHashSet<String> cosmeticIds = new LinkedHashSet();
      LinkedHashSet<String> hiddenTypes = new LinkedHashSet();
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

      for(SnapshotCosmetic cosmetic : cosmeticsBySlot.values()) {
         if (cosmetic != null && cosmetic.valueId() != null && !cosmetic.valueId().isBlank()) {
            cosmeticIds.add(cosmetic.valueId());
         }
      }

      return new SnapshotData(new ArrayList(cosmeticsBySlot.values()), cosmeticIds, hiddenTypes);
   }

   private static boolean applyStructuredSnapshot(Object wardrobe, Class pwcClass, SnapshotData data) {
      if (wardrobe != null && pwcClass != null && data != null && data.hasAnyData()) {
         clearWardrobeFields(wardrobe, pwcClass);
         Map<Object, Object> cosmeticsMap = asMutableMap(readFieldBestEffort(pwcClass, wardrobe, "cosmetics"));
         int equippedCount = 0;

         for(SnapshotCosmetic item : data.cosmetics()) {
            if (item != null && item.slot() != null && !item.slot().isBlank()) {
               Object cosmeticSaveData = tryCreateCosmeticSaveData(item.valueId(), item.optionId(), item.variantId());
               Object fallbackValue = cosmeticSaveData;
               if (cosmeticSaveData == null && item.valueBytes() != null && item.valueBytes().length > 0) {
                  fallbackValue = tryDeserializeJava(item.valueBytes());
               }

               if (cosmeticSaveData != null && trySetCosmetic(wardrobe, item.slot(), cosmeticSaveData) && isCosmeticEquipped(wardrobe, pwcClass, item.slot(), item.valueId())) {
                  ++equippedCount;
               } else if (fallbackValue != null && cosmeticsMap != null && putCosmeticFallback(cosmeticsMap, item.slot(), fallbackValue) && isCosmeticEquipped(wardrobe, pwcClass, item.slot(), item.valueId())) {
                  ++equippedCount;
               }
            }
         }

         if (equippedCount <= 0) {
            return false;
         } else {
            Set<Object> cosmeticIdSet = asMutableSet(readFieldBestEffort(pwcClass, wardrobe, "cosmeticIdSet"));
            if (cosmeticIdSet != null) {
               cosmeticIdSet.clear();
               Map<?, ?> equippedCosmetics = asMap(readFieldBestEffort(pwcClass, wardrobe, "cosmetics"));
               if (equippedCosmetics != null) {
                  for(Object equipped : equippedCosmetics.values()) {
                     String equippedId = tryExtractStringValue(equipped, List.of("getCosmeticId", "getId"), List.of("cosmeticId", "id"));
                     if (equippedId != null && !equippedId.isBlank()) {
                        cosmeticIdSet.add(equippedId);
                     }
                  }
               }
            }

            Set<Object> hiddenTypeSet = asMutableSet(readFieldBestEffort(pwcClass, wardrobe, "hiddenCosmeticTypes"));
            if (hiddenTypeSet != null) {
               hiddenTypeSet.clear();

               for(String hidden : data.hiddenTypes()) {
                  if (hidden != null && !hidden.isBlank()) {
                     hiddenTypeSet.add(hidden);
                  }
               }
            }

            forceWardrobeVisualRefresh(wardrobe);
            return true;
         }
      } else {
         return false;
      }
   }

   private static void collectCosmeticsFromStructuredContainer(Map out, BsonDocument container) {
      if (out != null && container != null) {
         BsonValue cosmeticsValue = firstValue(container, "Cosmetics", "cosmetics");
         if (cosmeticsValue != null) {
            if (cosmeticsValue.isDocument()) {
               for(Map.Entry e : cosmeticsValue.asDocument().entrySet()) {
                  if (e != null && e.getKey() != null) {
                     SnapshotCosmetic cosmetic = cosmeticFromSlotValue((String)e.getKey(), (BsonValue)e.getValue());
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

   private static void collectCosmeticsFromArray(Map out, BsonArray cosmetics) {
      if (out != null && cosmetics != null) {
         for(BsonValue v : cosmetics) {
            if (v != null && v.isDocument()) {
               SnapshotCosmetic cosmetic = cosmeticFromDocument(v.asDocument(), getStringAny(v.asDocument(), "slot", "Slot"));
               mergeCosmetic(out, cosmetic);
            }
         }

      }
   }

   private static SnapshotCosmetic cosmeticFromSlotValue(String slot, BsonValue value) {
      if (value == null) {
         return null;
      } else if (value.isDocument()) {
         return cosmeticFromDocument(value.asDocument(), slot);
      } else {
         return value.isString() ? new SnapshotCosmetic(slot, value.asString().getValue(), (String)null, (String)null, (byte[])null) : null;
      }
   }

   private static SnapshotCosmetic cosmeticFromDocument(BsonDocument item, String fallbackSlot) {
      if (item == null) {
         return null;
      } else {
         String slot = fallbackSlot != null && !fallbackSlot.isBlank() ? fallbackSlot : getStringAny(item, "slot", "Slot");
         if (slot != null && !slot.isBlank()) {
            byte[] valueBytes = null;
            if (item.containsKey("valueBytes") && item.get("valueBytes").isBinary()) {
               valueBytes = item.getBinary("valueBytes").getData();
            }

            String valueId = getStringAny(item, "valueId", "Id", "cosmeticId", "id");
            String optionId = getStringAny(item, "optionId", "Option", "option");
            String variantId = getStringAny(item, "variantId", "Variant", "variant");
            return (valueId == null || valueId.isBlank()) && valueBytes == null ? null : new SnapshotCosmetic(slot, valueId, optionId, variantId, valueBytes);
         } else {
            return null;
         }
      }
   }

   private static void mergeCosmetic(Map out, SnapshotCosmetic incoming) {
      if (out != null && incoming != null && incoming.slot() != null && !incoming.slot().isBlank()) {
         SnapshotCosmetic existing = (SnapshotCosmetic)out.get(incoming.slot());
         if (existing == null || snapshotCosmeticScore(incoming) >= snapshotCosmeticScore(existing)) {
            out.put(incoming.slot(), incoming);
         }

      }
   }

   private static int snapshotCosmeticScore(SnapshotCosmetic cosmetic) {
      if (cosmetic == null) {
         return -1;
      } else {
         int score = 0;
         if (cosmetic.valueId() != null && !cosmetic.valueId().isBlank()) {
            score += 2;
         }

         if (cosmetic.optionId() != null && !cosmetic.optionId().isBlank()) {
            ++score;
         }

         if (cosmetic.variantId() != null && !cosmetic.variantId().isBlank()) {
            ++score;
         }

         if (cosmetic.valueBytes() != null && cosmetic.valueBytes().length > 0) {
            score += 3;
         }

         return score;
      }
   }

   private static void collectStringArray(Set out, BsonArray arr) {
      if (out != null && arr != null) {
         for(BsonValue v : arr) {
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

   private static BsonDocument getDocument(BsonDocument doc, String key) {
      try {
         return doc.containsKey(key) && doc.get(key).isDocument() ? doc.getDocument(key) : null;
      } catch (Throwable var3) {
         return null;
      }
   }

   private static BsonValue firstValue(BsonDocument doc, String... keys) {
      if (doc != null && keys != null) {
         for(String key : keys) {
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
         for(String key : keys) {
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

   private static void forceWardrobeVisualRefresh(Object wardrobe) {
      for(String methodName : List.of("rebuild", "update", "markDirty", "refresh", "sync")) {
         tryInvokeNoArg(wardrobe, methodName);
      }

   }

   private static void tryApplyWardrobeModelTo(Store store, Ref ownerRef, Ref npcRef, Object wardrobe) {
      if (store != null && ownerRef != null && npcRef != null && wardrobe != null) {
         try {
            Player ownerPlayer = (Player)store.getComponent(ownerRef, Player.getComponentType());
            PlayerSettings settings = (PlayerSettings)store.getComponent(ownerRef, PlayerSettings.getComponentType());
            PlayerSkinComponent skin = (PlayerSkinComponent)store.getComponent(ownerRef, PlayerSkinComponent.getComponentType());
            PlayerRef ownerPlayerRef = (PlayerRef)store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (ownerPlayer == null || settings == null || skin == null || ownerPlayerRef == null) {
               return;
            }

            Class<?> tickSystemClass = Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeSystems$Tick");
            Class<?> playerWardrobeApiClass = Class.forName("dev.hardaway.wardrobe.api.player.PlayerWardrobe");
            Method buildWardrobeModel = findBuildWardrobeModel(tickSystemClass, playerWardrobeApiClass);
            if (buildWardrobeModel == null) {
               return;
            }

            Object modelObj = buildWardrobeModel.invoke((Object)null, ownerPlayer, settings, skin.getPlayerSkin(), wardrobe, ownerPlayerRef);
            if (!(modelObj instanceof Model)) {
               return;
            }

            Model model = (Model)modelObj;
            putOrSetComponent(store, npcRef, ModelComponent.getComponentType(), new ModelComponent(model));
            tryApplyModelViaNpcPlugin(store, npcRef, model);
         } catch (Throwable var13) {
         }

      }
   }

   private static Method findBuildWardrobeModel(Class tickSystemClass, Class playerWardrobeApiClass) {
      try {
         return tickSystemClass.getMethod("buildWardrobeModel", Player.class, PlayerSettings.class, PlayerSkin.class, playerWardrobeApiClass, PlayerRef.class);
      } catch (Throwable var7) {
         for(Method m : tickSystemClass.getMethods()) {
            if (m.getName().equals("buildWardrobeModel") && Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 5) {
               return m;
            }
         }

         return null;
      }
   }

   private static void tryApplyModelViaNpcPlugin(Store store, Ref npcRef, Model model) {
      try {
         Class<?> npcPluginClass = Class.forName("com.hypixel.hytale.server.npc.NPCPlugin");
         Object npcPlugin = npcPluginClass.getMethod("get").invoke((Object)null);
         if (npcPlugin == null) {
            return;
         }

         for(Method m : npcPluginClass.getMethods()) {
            String n = m.getName().toLowerCase();
            if (n.contains("model") && (n.contains("set") || n.contains("apply") || n.contains("update"))) {
               Class<?>[] p = m.getParameterTypes();
               if (p.length >= 2 && p.length <= 4) {
                  Object[] args = new Object[p.length];
                  boolean hasRef = false;
                  boolean hasModel = false;
                  boolean ok = true;

                  for(int i = 0; i < p.length; ++i) {
                     Class<?> pt = p[i];
                     if (!hasRef && pt.isInstance(npcRef)) {
                        args[i] = npcRef;
                        hasRef = true;
                     } else if (!hasModel && pt.isInstance(model)) {
                        args[i] = model;
                        hasModel = true;
                     } else {
                        if (!pt.isInstance(store)) {
                           ok = false;
                           break;
                        }

                        args[i] = store;
                     }
                  }

                  if (ok && hasRef && hasModel) {
                     m.invoke(npcPlugin, args);
                     return;
                  }
               }
            }
         }
      } catch (Throwable var17) {
      }

   }

   private static void tryInvokeNoArg(Object instance, String methodName) {
      if (instance != null && methodName != null && !methodName.isBlank()) {
         try {
            Method m = instance.getClass().getMethod(methodName);
            m.setAccessible(true);
            m.invoke(instance);
         } catch (Throwable var3) {
         }

      }
   }

   private static Object invokeStoreComponentMethod(Store store, String methodName, Ref ref, Object componentType) throws Exception {
      Method m = Store.class.getMethod(methodName, Ref.class, ComponentType.class);
      return m.invoke(store, ref, componentType);
   }

   private static void putOrSetComponent(Store store, Ref ref, Object componentType, Object component) {
      for(String methodName : List.of("putComponent", "setComponent")) {
         try {
            Method m = Store.class.getMethod(methodName, Ref.class, ComponentType.class, Object.class);
            m.invoke(store, ref, componentType, component);
            return;
         } catch (Throwable var7) {
         }
      }

   }

   private static BsonArray getArray(BsonDocument doc, String key) {
      try {
         return doc.containsKey(key) && doc.get(key).isArray() ? doc.getArray(key) : null;
      } catch (Throwable var3) {
         return null;
      }
   }

   private static String getString(BsonDocument doc, String key) {
      try {
         return doc.containsKey(key) && doc.get(key).isString() ? doc.getString(key).getValue() : null;
      } catch (Throwable var3) {
         return null;
      }
   }

   private static record SnapshotCosmetic(String slot, String valueId, String optionId, String variantId, byte[] valueBytes) {
   }

   private static record SnapshotData(List cosmetics, LinkedHashSet cosmeticIds, LinkedHashSet hiddenTypes) {
      boolean hasAnyData() {
         return this.cosmetics != null && !this.cosmetics.isEmpty() || this.cosmeticIds != null && !this.cosmeticIds.isEmpty() || this.hiddenTypes != null && !this.hiddenTypes.isEmpty();
      }
   }
}
