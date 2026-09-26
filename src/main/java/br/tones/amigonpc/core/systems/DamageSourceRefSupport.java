package br.tones.amigonpc.core.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.Source;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class DamageSourceRefSupport {
   private DamageSourceRefSupport() {
   }

   static Ref<EntityStore> extractAttackerRef(Source src) {
      if (src == null) {
         return null;
      }

      try {
         if (src instanceof EntitySource es) {
            return es.getRef();
         }
      } catch (Throwable var13) {
      }

      String[] methods = new String[]{"getRef", "getEntityRef", "getSourceRef", "getOwnerRef", "getShooterRef", "getAttackerRef"};

      for (String methodName : methods) {
         try {
            Method m = src.getClass().getMethod(methodName);
            if (m.invoke(src) instanceof Ref<?> ref) {
               return (Ref<EntityStore>)ref;
            }
         } catch (Throwable var12) {
         }
      }

      String[] fields = new String[]{"ref", "entityRef", "sourceRef", "ownerRef", "shooterRef", "attackerRef"};

      for (String fieldName : fields) {
         try {
            Field f = src.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.get(src) instanceof Ref<?> ref) {
               return (Ref<EntityStore>)ref;
            }
         } catch (Throwable var11) {
         }
      }

      return null;
   }
}
