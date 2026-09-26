package br.tones.amigonpc.core;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import java.io.IOException;
import java.nio.file.Path;
import org.bson.BsonDocument;
import org.bson.BsonValue;

final class AmigoPersistenceBackpackSupport {
   private AmigoPersistenceBackpackSupport() {
   }

   static SimpleItemContainer loadBackpack(Path file, short capacity) {
      try {
         BsonDocument doc = AmigoPersistenceDocSupport.loadDocumentSafely(file);
         if (doc == null) {
            return new SimpleItemContainer(capacity);
         }

         if (!doc.containsKey("backpack")) {
            return new SimpleItemContainer(capacity);
         }

         BsonValue value = doc.get("backpack");
         return value == null ? new SimpleItemContainer(capacity) : (SimpleItemContainer)SimpleItemContainer.CODEC.decode(value, new ExtraInfo());
      } catch (Throwable ignored) {
         return new SimpleItemContainer(capacity);
      }
   }

   static void saveBackpack(Path file, int formatVersion, SimpleItemContainer backpack) throws IOException {
      BsonDocument doc = AmigoPersistenceDocSupport.prepareWritableDocument(file, formatVersion);
      doc.put("backpack", SimpleItemContainer.CODEC.encode(backpack, new ExtraInfo()));
      AmigoPersistenceDocSupport.writeDocument(file, doc);
   }
}
