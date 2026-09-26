package br.tones.amigonpc.core;

import java.io.IOException;
import java.nio.file.Path;
import org.bson.BsonDocument;
import org.bson.BsonInt32;

final class AmigoPersistenceFlagSupport {
   private AmigoPersistenceFlagSupport() {
   }

   static boolean loadFlag(Path file, String key, boolean defaultValue) {
      try {
         return AmigoPersistenceDocSupport.getFlexibleBoolean(AmigoPersistenceDocSupport.loadDocumentSafely(file), key, defaultValue);
      } catch (Throwable ignored) {
         return defaultValue;
      }
   }

   static void saveFlag(Path file, int formatVersion, String key, boolean enabled) throws IOException {
      AmigoPersistenceDocSupport.updateDocument(
         file, formatVersion, doc -> doc.put(key, new BsonInt32(enabled ? 1 : 0))
      );
   }
}
