package br.tones.amigonpc.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class AmigoNpcRefApi {
   private static volatile Provider provider;
   private static final CopyOnWriteArrayList npcRefListeners = new CopyOnWriteArrayList();
   private static final CopyOnWriteArrayList despawnListeners = new CopyOnWriteArrayList();

   private AmigoNpcRefApi() {
   }

   public static void register(Provider p) {
      provider = (Provider)Objects.requireNonNull(p, "provider");
   }

   public static void unregister(Provider p) {
      if (provider == p) {
         provider = null;
      }

   }

   public static Optional getNpcRef(UUID ownerUuid) {
      Provider p = provider;
      return p == null ? Optional.empty() : p.getNpcRef(ownerUuid);
   }

   public static Optional ensureNpcSpawned(UUID ownerUuid) {
      Provider p = provider;
      return p == null ? Optional.empty() : p.ensureNpcSpawned(ownerUuid);
   }

   public static boolean isNpcReady(UUID ownerUuid) {
      Provider p = provider;
      return p != null && p.isNpcReady(ownerUuid);
   }

   public static Optional getNpcEntityId(UUID ownerUuid) {
      Provider p = provider;
      return p == null ? Optional.empty() : p.getNpcEntityId(ownerUuid);
   }

   public static void addNpcRefListener(BiConsumer listener) {
      npcRefListeners.add((BiConsumer)Objects.requireNonNull(listener, "listener"));
   }

   public static void removeNpcRefListener(BiConsumer listener) {
      npcRefListeners.remove(listener);
   }

   public static void addNpcDespawnListener(Consumer listener) {
      despawnListeners.add((Consumer)Objects.requireNonNull(listener, "listener"));
   }

   public static void removeNpcDespawnListener(Consumer listener) {
      despawnListeners.remove(listener);
   }

   public static void notifyNpcRef(UUID ownerUuid, Ref npcRef) {
      for(BiConsumer l : npcRefListeners) {
         try {
            l.accept(ownerUuid, npcRef);
         } catch (Throwable var5) {
         }
      }

   }

   public static void notifyNpcDespawn(UUID ownerUuid) {
      for(Consumer l : despawnListeners) {
         try {
            l.accept(ownerUuid);
         } catch (Throwable var4) {
         }
      }

   }

   public interface Provider {
      Optional getNpcRef(UUID var1);

      Optional ensureNpcSpawned(UUID var1);

      boolean isNpcReady(UUID var1);

      Optional getNpcEntityId(UUID var1);
   }
}
