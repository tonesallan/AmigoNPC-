package br.tones.amigonpc.core;

import com.hypixel.hytale.server.core.util.BsonUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonValue;

final class AmigoPersistenceDocSupport {
   private static final ConcurrentMap<Path, Object> FILE_LOCKS = new ConcurrentHashMap<>();

   private AmigoPersistenceDocSupport() {
   }

   static void updateDocument(Path file, int formatVersion, DocumentUpdater updater) throws IOException {
      if (file == null || updater == null) {
         return;
      }

      Path key = file.toAbsolutePath().normalize();
      Object lock = FILE_LOCKS.computeIfAbsent(key, ignored -> new Object());
      synchronized (lock) {
         Files.createDirectories(file.getParent());
         BsonDocument doc = Files.exists(file) ? BsonUtil.readDocumentNow(file) : null;
         if (doc == null) {
            doc = new BsonDocument();
         }

         touchDocument(doc, formatVersion);
         updater.update(doc);
         BsonUtil.writeDocument(file, doc, true).join();
      }
   }

   static BsonDocument loadDocumentSafely(Path file) {
      if (file != null && Files.exists(file)) {
         try {
            return BsonUtil.readDocumentNow(file);
         } catch (Throwable ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   static BsonDocument prepareWritableDocument(Path file, int formatVersion) throws IOException {
      Files.createDirectories(file.getParent());
      BsonDocument doc = BsonUtil.readDocumentNow(file);
      if (doc == null) {
         doc = new BsonDocument();
      }

      touchDocument(doc, formatVersion);
      return doc;
   }

   static void touchDocument(BsonDocument doc, int formatVersion) {
      doc.put("formatVersion", new BsonInt32(formatVersion));
      doc.put("savedAt", new BsonDateTime(System.currentTimeMillis()));
   }

   static void writeDocument(Path file, BsonDocument doc) {
      BsonUtil.writeDocument(file, doc, true).join();
   }

   static void putLongCompat(BsonDocument doc, String key, long value) {
      try {
         doc.put(key, new BsonInt64(value));
      } catch (Throwable ignored) {
         doc.put(key, new BsonDouble(value));
      }
   }

   static String getString(BsonDocument doc, String key) {
      if (doc != null && key != null && doc.containsKey(key)) {
         BsonValue value = doc.get(key);
         return value != null && value.isString() ? value.asString().getValue() : null;
      } else {
         return null;
      }
   }

   static double getDouble(BsonDocument doc, String key, double defaultValue) {
      if (doc != null && key != null && doc.containsKey(key)) {
         BsonValue value = doc.get(key);
         if (value == null) {
            return defaultValue;
         } else if (value.isDouble()) {
            return value.asDouble().getValue();
         } else {
            return value.isInt32() ? value.asInt32().getValue() : defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   static long getLong(BsonDocument doc, String key, long defaultValue) {
      if (doc != null && key != null && doc.containsKey(key)) {
         BsonValue value = doc.get(key);
         if (value == null) {
            return defaultValue;
         }

         if (value.isInt64()) {
            return value.asInt64().getValue();
         }

         if (value.isInt32()) {
            return value.asInt32().getValue();
         }

         if (value.isDouble()) {
            return (long)value.asDouble().getValue();
         }

         if (value.isString()) {
            try {
               return Long.parseLong(value.asString().getValue());
            } catch (Throwable ignored) {
               return defaultValue;
            }
         } else {
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   static boolean getFlexibleBoolean(BsonDocument doc, String key, boolean defaultValue) {
      if (doc != null && key != null && doc.containsKey(key)) {
         BsonValue value = doc.get(key);
         if (value == null) {
            return defaultValue;
         }

         if (value.isBoolean()) {
            return value.asBoolean().getValue();
         }

         if (value.isInt32()) {
            return value.asInt32().getValue() != 0;
         }

         if (value.isDouble()) {
            return value.asDouble().getValue() != 0.0;
         }

         if (value.isString()) {
            String text = value.asString().getValue();
            if (text == null) {
               return defaultValue;
            }

            text = text.trim().toLowerCase();
            return text.equals("on") || text.equals("true") || text.equals("1") || text.equals("sim") || text.equals("yes");
         } else {
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   @FunctionalInterface
   interface DocumentUpdater {
      void update(BsonDocument doc);
   }

   static BsonDocument getDocument(BsonDocument doc, String key) {
      if (doc != null && key != null && doc.containsKey(key)) {
         BsonValue value = doc.get(key);
         return value != null && value.isDocument() ? value.asDocument() : new BsonDocument();
      } else {
         return new BsonDocument();
      }
   }
}
