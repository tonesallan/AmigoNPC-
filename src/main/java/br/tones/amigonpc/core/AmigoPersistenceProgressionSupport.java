package br.tones.amigonpc.core;

import java.io.IOException;
import java.nio.file.Path;
import org.bson.BsonDocument;

final class AmigoPersistenceProgressionSupport {
   private AmigoPersistenceProgressionSupport() {
   }

   static long loadLong(Path file, String key, long defaultValue) {
      try {
         return AmigoPersistenceDocSupport.getLong(AmigoPersistenceDocSupport.loadDocumentSafely(file), key, defaultValue);
      } catch (Throwable ignored) {
         return defaultValue;
      }
   }

   static void saveNpcProgress(Path file, int formatVersion, String totalXpKey, String baseHpKey, String baseDefKey, long totalXp, long baseHp, long baseDef) throws IOException {
      BsonDocument doc = AmigoPersistenceDocSupport.prepareWritableDocument(file, formatVersion);
      if (totalXp >= 0L) {
         AmigoPersistenceDocSupport.putLongCompat(doc, totalXpKey, totalXp);
      }

      if (baseHp >= 0L) {
         AmigoPersistenceDocSupport.putLongCompat(doc, baseHpKey, baseHp);
      }

      if (baseDef >= 0L) {
         AmigoPersistenceDocSupport.putLongCompat(doc, baseDefKey, baseDef);
      }

      AmigoPersistenceDocSupport.writeDocument(file, doc);
   }

   static BsonDocument loadDocumentSection(Path file, String key) {
      try {
         return AmigoPersistenceDocSupport.getDocument(AmigoPersistenceDocSupport.loadDocumentSafely(file), key);
      } catch (Throwable ignored) {
         return new BsonDocument();
      }
   }

   static void saveDocumentSection(Path file, int formatVersion, String key, BsonDocument sectionDoc) throws IOException {
      BsonDocument doc = AmigoPersistenceDocSupport.prepareWritableDocument(file, formatVersion);
      if (sectionDoc != null && !sectionDoc.isEmpty()) {
         doc.put(key, sectionDoc);
      } else {
         doc.remove(key);
      }

      AmigoPersistenceDocSupport.writeDocument(file, doc);
   }
}
