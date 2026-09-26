package br.tones.amigonpc.core;

import java.io.IOException;
import java.nio.file.Path;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonString;

final class AmigoPersistenceProfileSupport {
   private AmigoPersistenceProfileSupport() {
   }

   static String loadString(Path file, String key) {
      try {
         return AmigoPersistenceDocSupport.getString(AmigoPersistenceDocSupport.loadDocumentSafely(file), key);
      } catch (Throwable ignored) {
         return null;
      }
   }

   static double loadDouble(Path file, String key, double defaultValue) {
      try {
         return AmigoPersistenceDocSupport.getDouble(AmigoPersistenceDocSupport.loadDocumentSafely(file), key, defaultValue);
      } catch (Throwable ignored) {
         return defaultValue;
      }
   }

   static int loadPositiveInt(Path file, String key, int defaultValue) {
      try {
         return Math.max(defaultValue, (int)AmigoPersistenceDocSupport.getLong(AmigoPersistenceDocSupport.loadDocumentSafely(file), key, defaultValue));
      } catch (Throwable ignored) {
         return defaultValue;
      }
   }

   static void saveModel(Path file, int formatVersion, String modelIdKey, String modelScaleKey, String modelId, double scale) throws IOException {
      BsonDocument doc = AmigoPersistenceDocSupport.prepareWritableDocument(file, formatVersion);
      if (modelId != null && !modelId.isBlank()) {
         doc.put(modelIdKey, new BsonString(modelId));
         doc.put(modelScaleKey, new BsonDouble(scale));
      } else {
         doc.remove(modelIdKey);
         doc.remove(modelScaleKey);
      }

      AmigoPersistenceDocSupport.writeDocument(file, doc);
   }

   static void saveOptionalString(Path file, int formatVersion, String key, String value) throws IOException {
      BsonDocument doc = AmigoPersistenceDocSupport.prepareWritableDocument(file, formatVersion);
      if (value != null && !value.isBlank()) {
         doc.put(key, new BsonString(value));
      } else {
         doc.remove(key);
      }

      AmigoPersistenceDocSupport.writeDocument(file, doc);
   }

   static void saveSwordState(Path file, int formatVersion, String swordLevelKey, String equippedWeaponKey, int swordLevel, String equippedWeaponId) throws IOException {
      BsonDocument doc = AmigoPersistenceDocSupport.prepareWritableDocument(file, formatVersion);
      doc.put(swordLevelKey, new BsonInt32(Math.max(1, swordLevel)));
      if (equippedWeaponId != null && !equippedWeaponId.isBlank()) {
         doc.put(equippedWeaponKey, new BsonString(equippedWeaponId));
      } else {
         doc.remove(equippedWeaponKey);
      }

      AmigoPersistenceDocSupport.writeDocument(file, doc);
   }
}
