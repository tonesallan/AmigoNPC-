package br.tones.amigonpc.core.rpgleveling.stage1;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class RpgStage1UuidSupport {
   private RpgStage1UuidSupport() {
   }

   static UUID resolveTargetUuid(Store<EntityStore> store, Ref<EntityStore> targetRef, Object targetRefObj) {
      UUID uuid = resolveUuidComponent(store, targetRef);
      if (uuid == null) {
         uuid = tryExtractUuidFromRefObject(targetRefObj);
      }

      if (uuid != null) {
         return uuid;
      }

      String seed = "AmigoNPC:ref:" + (targetRefObj != null ? targetRefObj : targetRef);
      return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
   }

   private static UUID resolveUuidComponent(Store<EntityStore> store, Ref<EntityStore> targetRef) {
      try {
         UUIDComponent uc = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
         if (uc != null) {
            return uc.getUuid();
         }
      } catch (Throwable var3) {
      }

      return null;
   }

   private static UUID tryExtractUuidFromRefObject(Object refObj) {
      if (refObj == null) {
         return null;
      }

      UUID uuid = tryInvokeUuidGetter(refObj, "getUuid");
      if (uuid != null) {
         return uuid;
      }

      uuid = tryInvokeUuidGetter(refObj, "getId");
      return uuid != null ? uuid : tryInvokeUuidGetter(refObj, "id");
   }

   private static UUID tryInvokeUuidGetter(Object refObj, String methodName) {
      try {
         Method m = refObj.getClass().getMethod(methodName);
         Object v = m.invoke(refObj);
         if (v instanceof UUID u) {
            return u;
         }

         if (v instanceof String s) {
            try {
               return UUID.fromString(s);
            } catch (Throwable var6) {
            }
         }
      } catch (Throwable var7) {
      }

      return null;
   }
}
