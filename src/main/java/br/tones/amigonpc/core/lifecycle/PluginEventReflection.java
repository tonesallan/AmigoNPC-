package br.tones.amigonpc.core.lifecycle;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

final class PluginEventReflection {
   private PluginEventReflection() {
   }

   static Method findRegisterMethod(Object registry) {
      if (registry == null) {
         return null;
      }

      for (Method m : registry.getClass().getMethods()) {
         if (m.getName().equals("register") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == Class.class) {
            return m;
         }
      }

      return null;
   }

   static UUID extractUuid(Object evt) {
      if (evt == null) {
         return null;
      }

      Object playerRef = extractPlayerRef(evt);
      if (playerRef != null) {
         Object u = invokeNoArg(playerRef, "getUuid", "uuid");
         UUID id = asUuid(u);
         if (id != null) {
            return id;
         }
      }

      Object player = extractPlayer(evt);
      if (player != null) {
         Object u = invokeNoArg(player, "getUuid", "getUniqueId", "uuid");
         UUID id = asUuid(u);
         if (id != null) {
            return id;
         }
      }

      Object u2 = invokeNoArg(evt, "getUuid", "getPlayerUuid", "getOwnerId", "getPlayerUUID");
      return asUuid(u2);
   }

   static PlayerRef extractPlayerRef(Object evt) {
      if (evt == null) {
         return null;
      } else if (invokeNoArg(evt, "getPlayerRef", "playerRef") instanceof PlayerRef pref) {
         return pref;
      } else {
         Object player = extractPlayer(evt);
         if (player == null) {
            return null;
         } else {
            return invokeNoArg(player, "getPlayerRef", "getReference", "getRef", "playerRef", "playerReference") instanceof PlayerRef pref ? pref : null;
         }
      }
   }

   static Player extractPlayer(Object evt) {
      if (evt == null) {
         return null;
      } else {
         return invokeNoArg(evt, "getPlayer", "player") instanceof Player player ? player : null;
      }
   }

   static Object invokeNoArg(Object obj, String... names) {
      if (obj == null) {
         return null;
      }

      for (String n : names) {
         try {
            Method m = obj.getClass().getMethod(n);
            return m.invoke(obj);
         } catch (Throwable var8) {
            try {
               Field f = obj.getClass().getField(n);
               return f.get(obj);
            } catch (Throwable var7) {
            }
         }
      }

      return null;
   }

   private static UUID asUuid(Object u) {
      if (u instanceof UUID) {
         return (UUID)u;
      }

      if (u instanceof String s) {
         try {
            return UUID.fromString(s);
         } catch (Exception var3) {
         }
      }

      return null;
   }
}
