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
   private static volatile AmigoNpcRefApi.Provider provider;
   private static final CopyOnWriteArrayList<BiConsumer<UUID, Ref<EntityStore>>> npcRefListeners = new CopyOnWriteArrayList<>();
   private static final CopyOnWriteArrayList<Consumer<UUID>> despawnListeners = new CopyOnWriteArrayList<>();

   private AmigoNpcRefApi() {
   }

   public static void register(AmigoNpcRefApi.Provider p) {
      provider = Objects.requireNonNull(p, "provider");
   }

   public static void unregister(AmigoNpcRefApi.Provider p) {
      if (provider == p) {
         provider = null;
      }
   }

   public static Optional<Ref<EntityStore>> getNpcRef(UUID ownerUuid) {
      AmigoNpcRefApi.Provider p = provider;
      return p == null ? Optional.empty() : p.getNpcRef(ownerUuid);
   }

   public static Optional<Ref<EntityStore>> ensureNpcSpawned(UUID ownerUuid) {
      AmigoNpcRefApi.Provider p = provider;
      return p == null ? Optional.empty() : p.ensureNpcSpawned(ownerUuid);
   }

   public static boolean isNpcReady(UUID ownerUuid) {
      AmigoNpcRefApi.Provider p = provider;
      return p != null && p.isNpcReady(ownerUuid);
   }

   public static Optional<Long> getNpcEntityId(UUID ownerUuid) {
      AmigoNpcRefApi.Provider p = provider;
      return p == null ? Optional.empty() : p.getNpcEntityId(ownerUuid);
   }

   public static void addNpcRefListener(BiConsumer<UUID, Ref<EntityStore>> listener) {
      npcRefListeners.add(Objects.requireNonNull(listener, "listener"));
   }

   public static void removeNpcRefListener(BiConsumer<UUID, Ref<EntityStore>> listener) {
      npcRefListeners.remove(listener);
   }

   public static void addNpcDespawnListener(Consumer<UUID> listener) {
      despawnListeners.add(Objects.requireNonNull(listener, "listener"));
   }

   public static void removeNpcDespawnListener(Consumer<UUID> listener) {
      despawnListeners.remove(listener);
   }

   public static void notifyNpcRef(UUID ownerUuid, Ref<EntityStore> npcRef) {
      for (BiConsumer<UUID, Ref<EntityStore>> l : npcRefListeners) {
         try {
            l.accept(ownerUuid, npcRef);
         } catch (Throwable var5) {
         }
      }
   }

   public static void notifyNpcDespawn(UUID ownerUuid) {
      for (Consumer<UUID> l : despawnListeners) {
         try {
            l.accept(ownerUuid);
         } catch (Throwable var4) {
         }
      }
   }

   public interface Provider {
      Optional<Ref<EntityStore>> getNpcRef(UUID var1);

      Optional<Ref<EntityStore>> ensureNpcSpawned(UUID var1);

      boolean isNpcReady(UUID var1);

      Optional<Long> getNpcEntityId(UUID var1);
   }
}
