package br.tones.amigonpc.core.optional;

import br.tones.amigonpc.core.AmigoPersistence;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonString;

public final class AmigoWardrobePersistence {
   private static final String CLS_PLAYER_WARDROBE_COMPONENT = "dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent";
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

   public static boolean saveNpcWardrobe(UUID ownerId, Store<EntityStore> store, Ref<EntityStore> npcRef) {
      if (ownerId != null && store != null && npcRef != null) {
         try {
            Class<?> pwcClass = tryLoadWardrobeClass();
            if (pwcClass == null) {
               return false;
            } else {
               Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke(null);
               Object wardrobe = AmigoWardrobeModelSupport.invokeStoreComponentMethod(store, "getComponent", npcRef, wardrobeComponentType);
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
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean hasSavedCosmetics(UUID ownerId) {
      if (ownerId == null) {
         return false;
      }

      try {
         BsonDocument snapshot = AmigoPersistence.loadWardrobeState(ownerId);
         return hasSnapshotCosmetics(snapshot);
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean restoreNpcWardrobe(UUID ownerId, Store<EntityStore> store, Ref<EntityStore> ownerRef, Ref<EntityStore> npcRef) {
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
                  Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke(null);
                  Object wardrobe = AmigoWardrobeModelSupport.invokeStoreComponentMethod(store, "ensureAndGetComponent", npcRef, wardrobeComponentType);
                  if (wardrobe == null) {
                     forceDefaultWardrobe(store, npcRef);
                     return false;
                  } else {
                     boolean applied = applySnapshot(wardrobe, snapshot, pwcClass);
                     if (!applied) {
                        AmigoWardrobeApplySupport.clearWardrobeFields(wardrobe, pwcClass);
                        AmigoWardrobeModelSupport.forceWardrobeVisualRefresh(wardrobe);
                        AmigoWardrobeModelSupport.putOrSetComponent(store, npcRef, wardrobeComponentType, wardrobe);
                        return false;
                     } else {
                        AmigoWardrobeModelSupport.forceWardrobeVisualRefresh(wardrobe);
                        if (!AmigoWardrobeModelSupport.putOrSetComponent(store, npcRef, wardrobeComponentType, wardrobe)) {
                           return false;
                        }

                        if (ownerRef == null) {
                           return true;
                        }

                        return AmigoWardrobeModelSupport.tryApplyWardrobeModelTo(store, ownerRef, npcRef, wardrobe);
                     }
                  }
               }
            }
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean restoreNpcWardrobe(UUID ownerId, Store<EntityStore> store, Ref<EntityStore> npcRef) {
      return restoreNpcWardrobe(ownerId, store, null, npcRef);
   }

   public static void forceDefaultWardrobe(Store<EntityStore> store, Ref<EntityStore> npcRef) {
      if (store != null && npcRef != null) {
         try {
            Class<?> pwcClass = tryLoadWardrobeClass();
            if (pwcClass == null) {
               return;
            }

            Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke(null);
            Object wardrobe = AmigoWardrobeModelSupport.invokeStoreComponentMethod(store, "ensureAndGetComponent", npcRef, wardrobeComponentType);
            if (wardrobe == null) {
               return;
            }

            AmigoWardrobeApplySupport.clearWardrobeFields(wardrobe, pwcClass);
            AmigoWardrobeModelSupport.forceWardrobeVisualRefresh(wardrobe);
            AmigoWardrobeModelSupport.putOrSetComponent(store, npcRef, wardrobeComponentType, wardrobe);
         } catch (Throwable var5) {
         }
      }
   }

   public static Object buildOwnerWardrobeModel(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
      if (store != null && ownerRef != null) {
         try {
            Class<?> pwcClass = tryLoadWardrobeClass();
            return pwcClass == null ? null : AmigoWardrobeModelSupport.buildOwnerWardrobeModel(store, ownerRef, pwcClass);
         } catch (Throwable ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Class<?> tryLoadWardrobeClass() {
      try {
         return Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent");
      } catch (Throwable ignored) {
         try {
            return Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent", true, Thread.currentThread().getContextClassLoader());
         } catch (Throwable ignored2) {
            return null;
         }
      }
   }

   private static boolean hasSnapshotCosmetics(BsonDocument snapshot) {
      return AmigoWardrobeSnapshotSupport.hasSnapshotCosmetics(snapshot);
   }

   private static BsonDocument snapshotWardrobe(Object wardrobe, Class<?> pwcClass) {
      if (wardrobe == null) {
         return null;
      }

      BsonDocument out = new BsonDocument();
      byte[] fullBytes = AmigoWardrobeValueSupport.trySerializeJava(AmigoWardrobeValueSupport.safeClone(wardrobe));
      if (fullBytes == null) {
         fullBytes = AmigoWardrobeValueSupport.trySerializeJava(wardrobe);
      }

      if (fullBytes != null && fullBytes.length > 0) {
         out.put("componentBytes", new BsonBinary(fullBytes));
      }

      Map<?, ?> cosmetics = AmigoWardrobeValueSupport.asMap(AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "cosmetics"));
      BsonArray cosmeticsArray = new BsonArray();
      BsonDocument wardrobeCosmeticsMap = new BsonDocument();
      LinkedHashSet<String> cosmeticIds = AmigoWardrobeValueSupport.toStringSet(
         AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "cosmeticIdSet")
      );
      if (cosmetics != null && !cosmetics.isEmpty()) {
         for (Entry<?, ?> e : cosmetics.entrySet()) {
            if (e != null && e.getKey() != null && e.getValue() != null) {
               String slot = String.valueOf(e.getKey());
               Object value = e.getValue();
               byte[] valueBytes = AmigoWardrobeValueSupport.trySerializeJava(value);
               String valueId = AmigoWardrobeValueSupport.tryExtractId(value);
               String optionId = AmigoWardrobeValueSupport.tryExtractOptionId(value);
               String variantId = AmigoWardrobeValueSupport.tryExtractVariantId(value);
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

         for (String s : cosmeticIds) {
            arr.add(new BsonString(s));
         }

         out.put("cosmeticIds", arr);
      }

      LinkedHashSet<String> hiddenTypes = AmigoWardrobeValueSupport.toStringSet(
         AmigoWardrobeValueSupport.readFieldBestEffort(pwcClass, wardrobe, "hiddenCosmeticTypes")
      );
      BsonArray hiddenArr = new BsonArray();

      for (String s : hiddenTypes) {
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

   private static boolean applySnapshot(Object wardrobe, BsonDocument snapshot, Class<?> pwcClass) {
      if (wardrobe != null && snapshot != null && !snapshot.isEmpty()) {
         AmigoWardrobeSnapshotSupport.SnapshotData data = AmigoWardrobeSnapshotSupport.extractSnapshotData(snapshot);
         if (data != null && data.hasAnyData()) {
            boolean applied = AmigoWardrobeApplySupport.applyStructuredSnapshot(wardrobe, pwcClass, data);
            if (applied) {
               return true;
            }
         }

         if (snapshot.containsKey("componentBytes") && snapshot.get("componentBytes").isBinary()) {
            Object restored = AmigoWardrobeValueSupport.tryDeserializeJava(snapshot.getBinary("componentBytes").getData());
            if (restored != null && pwcClass.isInstance(restored) && AmigoWardrobeApplySupport.copyWardrobeFields(restored, wardrobe, pwcClass)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
