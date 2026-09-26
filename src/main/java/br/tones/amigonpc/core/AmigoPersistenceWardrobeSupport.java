package br.tones.amigonpc.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.bson.BsonDocument;
import org.bson.BsonValue;

final class AmigoPersistenceWardrobeSupport {
   private static final String KEY_COSMETICS = "cosmetics";
   private static final String KEY_COSMETIC_IDS = "cosmeticIds";
   private static final String KEY_COSMETIC_ID_SET = "cosmeticIdSet";
   private static final String KEY_HIDDEN_COSMETIC_TYPES = "hiddenCosmeticTypes";
   private static final String KEY_HIDDEN_TYPES = "hiddenTypes";
   private static final String KEY_PLAYER_WARDROBE = "PlayerWardrobe";
   private static final String KEY_PLAYER_WARDROBE_ALT = "playerWardrobe";
   private static final String KEY_PLAYER_WARDROBE_COSMETICS = "Cosmetics";

   private AmigoPersistenceWardrobeSupport() {
   }

   static BsonDocument loadWardrobeState(Path file, UUID ownerId, String wardrobeStateKey) {
      try {
         BsonDocument doc = AmigoPersistenceDocSupport.loadDocumentSafely(file);
         if (doc == null) {
            return new BsonDocument();
         }

         BsonValue value = doc.get(ownerId.toString());
         if (value != null && value.isDocument()) {
            BsonDocument entry = value.asDocument();
            BsonValue state = entry.get(wardrobeStateKey);
            if (state != null && state.isDocument()) {
               return state.asDocument();
            } else {
               return looksLikeWardrobeStateDoc(entry) ? entry : new BsonDocument();
            }
         } else {
            return new BsonDocument();
         }
      } catch (Throwable ignored) {
         return new BsonDocument();
      }
   }

   static void saveWardrobeState(Path file, int formatVersion, UUID ownerId, BsonDocument wardrobeDoc, String wardrobeStateKey) throws IOException {
      BsonDocument doc = AmigoPersistenceDocSupport.prepareWritableDocument(file, formatVersion);
      if (wardrobeDoc != null && !wardrobeDoc.isEmpty()) {
         BsonValue existing = doc.get(ownerId.toString());
         BsonDocument entry;
         if (existing != null && existing.isDocument()) {
            entry = existing.asDocument();
         } else {
            entry = new BsonDocument();
         }

         AmigoPersistenceDocSupport.touchDocument(entry, formatVersion);
         entry.put(wardrobeStateKey, wardrobeDoc);
         doc.put(ownerId.toString(), entry);
      } else {
         doc.remove(ownerId.toString());
      }

      AmigoPersistenceDocSupport.writeDocument(file, doc);
   }

   private static boolean looksLikeWardrobeStateDoc(BsonDocument doc) {
      return doc != null && !doc.isEmpty()
         ? doc.containsKey("cosmetics")
            || doc.containsKey("cosmeticIds")
            || doc.containsKey("cosmeticIdSet")
            || doc.containsKey("hiddenCosmeticTypes")
            || doc.containsKey("hiddenTypes")
            || doc.containsKey("PlayerWardrobe")
            || doc.containsKey("playerWardrobe")
            || doc.containsKey("Cosmetics")
         : false;
   }
}
