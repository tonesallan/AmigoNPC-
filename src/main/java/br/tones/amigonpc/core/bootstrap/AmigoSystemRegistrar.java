package br.tones.amigonpc.core.bootstrap;

import br.tones.amigonpc.core.systems.AmigoDamageAndDownedSystem;
import br.tones.amigonpc.core.systems.AmigoPlayerKillNpcXpSystem;
import java.lang.reflect.Method;

public final class AmigoSystemRegistrar {
   private final Object entityStoreRegistry;

   public AmigoSystemRegistrar(Object entityStoreRegistry) {
      this.entityStoreRegistry = entityStoreRegistry;
   }

   public void registerCoreSystems() {
      try {
         this.registerSystem(new AmigoDamageAndDownedSystem());
         this.registerSystem(new AmigoPlayerKillNpcXpSystem());
      } catch (Throwable var2) {
      }
   }

   private void registerSystem(Object system) {
      if (this.entityStoreRegistry != null && system != null) {
         try {
            for (Method m : this.entityStoreRegistry.getClass().getMethods()) {
               if (m.getName().equals("registerSystem") && m.getParameterCount() == 1) {
                  Class<?> p = m.getParameterTypes()[0];
                  if (p.isAssignableFrom(system.getClass())) {
                     m.invoke(this.entityStoreRegistry, system);
                     return;
                  }
               }
            }
         } catch (Throwable var7) {
         }
      }
   }
}
