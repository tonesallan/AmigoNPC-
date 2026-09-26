package br.tones.amigonpc.core;

import br.tones.amigonpc.core.i18n.AmigoText;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class HytaleBridge {
   private static volatile String LAST_ERROR;

   private HytaleBridge() {
   }

   public static String getLastError() {
      return LAST_ERROR;
   }

   private static void setError(String msg) {
      LAST_ERROR = msg;
   }

   public static Object tryGetWorldFromCommandContext(Object commandContext) {
      try {
         if (commandContext == null) {
            return null;
         }

         Method senderMethod = commandContext.getClass().getMethod("sender");
         Object sender = senderMethod.invoke(commandContext);
         if (sender == null) {
            return null;
         }

         String[] candidates = new String[]{"getWorld", "world", "getCurrentWorld", "getPlayerWorld"};

         for (String name : candidates) {
            try {
               Method m = sender.getClass().getMethod(name);
               Object world = m.invoke(sender);
               if (world != null) {
                  return world;
               }
            } catch (Throwable var10) {
            }
         }

         return null;
      } catch (Throwable t) {
         setError(AmigoText.format("core.bridge.error.get_world_from_context_failed", t.getClass().getSimpleName(), t.getMessage()));
         return null;
      }
   }

   public static boolean worldExecute(Object world, Runnable task) {
      if (world != null && task != null) {
         try {
            Method m = world.getClass().getMethod("execute", Runnable.class);
            m.invoke(world, task);
            return true;
         } catch (Throwable var4) {
            try {
               Method m = world.getClass().getMethod("run", Runnable.class);
               m.invoke(world, task);
               return true;
            } catch (Throwable var3) {
               setError(AmigoText.text("core.bridge.error.world_execute_incompatible"));
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean spawnBasicNpc(Object worldObj, UUID ownerId) {
      if (worldObj == null) {
         setError(AmigoText.text("core.bridge.error.world_null"));
         return false;
      } else if (ownerId == null) {
         setError(AmigoText.text("core.bridge.error.owner_id_null"));
         return false;
      } else {
         return worldExecute(worldObj, () -> {
            try {
               Object store = invokeNoArg(worldObj, "getEntityStore", "entityStore");
               if (store == null) {
                  setError(AmigoText.text("core.bridge.error.entity_store_missing"));
                  return;
               }

               Object holder = createEntityHolder();
               if (holder == null) {
                  setError(AmigoText.text("core.bridge.error.create_holder_failed"));
                  return;
               }

               tryAddTransform(holder);
               tryAddUUIDComponent(holder, ownerId);
               tryAddNetworkId(holder, store);
               Object addReasonSpawn = getEnumConstant("com.hypixel.hytale.server.core.universe.world.storage.EntityStore$AddReason", "SPAWN");
               if (addReasonSpawn == null) {
                  boolean ok = invokeAddEntityWithoutReason(store, holder);
                  if (!ok) {
                     setError(AmigoText.text("core.bridge.error.add_entity_without_reason_failed"));
                  }

                  return;
               }

               boolean ok = invokeAddEntityWithReason(store, holder, addReasonSpawn);
               if (!ok) {
                  setError(AmigoText.text("core.bridge.error.add_entity_with_reason_failed"));
               }
            } catch (Throwable t) {
               setError(AmigoText.format("core.bridge.error.spawn_basic_npc_failed", t.getClass().getSimpleName(), t.getMessage()));
            }
         });
      }
   }

   private static Object createEntityHolder() {
      try {
         Class<?> entityStoreClass = Class.forName("com.hypixel.hytale.server.core.universe.world.storage.EntityStore");
         Field registryField = entityStoreClass.getField("REGISTRY");
         Object registry = registryField.get(null);
         if (registry == null) {
            return null;
         }

         Method newHolder = registry.getClass().getMethod("newHolder");
         return newHolder.invoke(registry);
      } catch (Throwable t) {
         setError(AmigoText.format("core.bridge.error.create_entity_holder_failed", t.getClass().getSimpleName(), t.getMessage()));
         return null;
      }
   }

   private static boolean invokeAddEntityWithReason(Object store, Object holder, Object addReasonSpawn) {
      try {
         for (Method m : store.getClass().getMethods()) {
            if (m.getName().equals("addEntity")) {
               Class<?>[] p = m.getParameterTypes();
               if (p.length == 2 && p[1].isInstance(addReasonSpawn)) {
                  m.invoke(store, holder, addReasonSpawn);
                  return true;
               }
            }
         }
      } catch (Throwable var8) {
      }

      return false;
   }

   private static boolean invokeAddEntityWithoutReason(Object store, Object holder) {
      try {
         Method m = store.getClass().getMethod("addEntity", holder.getClass());
         m.invoke(store, holder);
         return true;
      } catch (Throwable var8) {
         try {
            for (Method m : store.getClass().getMethods()) {
               if (m.getName().equals("addEntity")) {
                  Class<?>[] p = m.getParameterTypes();
                  if (p.length == 1) {
                     m.invoke(store, holder);
                     return true;
                  }
               }
            }
         } catch (Throwable var7) {
         }

         return false;
      }
   }

   private static void tryAddTransform(Object holder) {
      String[] classNames = new String[]{
         "com.hypixel.hytale.server.core.universe.world.entity.component.TransformComponent",
         "com.hypixel.hytale.server.core.universe.world.entity.components.TransformComponent"
      };
      Object comp = tryInstantiateFirstExisting(classNames);
      if (comp != null) {
         invokeHolderAdd(holder, comp);
      }
   }

   private static void tryAddUUIDComponent(Object holder, UUID ownerId) {
      String[] classNames = new String[]{
         "com.hypixel.hytale.server.core.universe.world.entity.component.UUIDComponent",
         "com.hypixel.hytale.server.core.universe.world.entity.components.UUIDComponent"
      };
      Object comp = tryInstantiateUUIDComponent(classNames, ownerId);
      if (comp != null) {
         invokeHolderAdd(holder, comp);
      }
   }

   private static void tryAddNetworkId(Object holder, Object store) {
      String[] classNames = new String[]{
         "com.hypixel.hytale.server.core.universe.world.entity.component.NetworkIdComponent",
         "com.hypixel.hytale.server.core.universe.world.entity.components.NetworkIdComponent"
      };
      Object networkId = null;

      try {
         Object externalData = invokeNoArg(store, "getExternalData", "externalData");
         if (externalData != null) {
            networkId = invokeNoArg(externalData, "takeNextNetworkId", "nextNetworkId", "getNextNetworkId");
         }
      } catch (Throwable var5) {
      }

      if (networkId != null) {
         Object comp = tryInstantiateSingleArgFirstExisting(classNames, networkId);
         if (comp != null) {
            invokeHolderAdd(holder, comp);
         }
      }
   }

   private static void invokeHolderAdd(Object holder, Object component) {
      try {
         for (Method m : holder.getClass().getMethods()) {
            if (m.getName().equals("add")) {
               Class<?>[] p = m.getParameterTypes();
               if (p.length == 1) {
                  m.invoke(holder, component);
                  return;
               }
            }
         }
      } catch (Throwable var7) {
      }
   }

   private static Object invokeNoArg(Object target, String... methodNames) {
      for (String name : methodNames) {
         try {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static Object getEnumConstant(String enumClassName, String constantName) {
      try {
         Class<?> enumClass = Class.forName(enumClassName);
         return !enumClass.isEnum() ? null : Enum.valueOf(enumClass, constantName);
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static Object tryInstantiateFirstExisting(String[] classNames) {
      for (String cn : classNames) {
         try {
            Class<?> c = Class.forName(cn);
            Constructor<?> ctor = c.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static Object tryInstantiateUUIDComponent(String[] classNames, UUID ownerId) {
      for (String cn : classNames) {
         try {
            Class<?> c = Class.forName(cn);

            try {
               Constructor<?> ctor = c.getDeclaredConstructor(UUID.class);
               ctor.setAccessible(true);
               return ctor.newInstance(ownerId);
            } catch (Throwable var16) {
            }

            try {
               Constructor<?> ctor = c.getDeclaredConstructor();
               ctor.setAccessible(true);
               Object obj = ctor.newInstance();
               String[] setters = new String[]{"setUuid", "setId", "setValue"};

               for (String s : setters) {
                  try {
                     Method m = c.getMethod(s, UUID.class);
                     m.invoke(obj, ownerId);
                     return obj;
                  } catch (Throwable var18) {
                  }
               }

               String[] fields = new String[]{"uuid", "id", "value"};

               for (String f : fields) {
                  try {
                     Field field = c.getDeclaredField(f);
                     field.setAccessible(true);
                     field.set(obj, ownerId);
                     return obj;
                  } catch (Throwable var17) {
                  }
               }
            } catch (Throwable var19) {
            }
         } catch (Throwable var20) {
         }
      }

      return null;
   }

   private static Object tryInstantiateSingleArgFirstExisting(String[] classNames, Object arg) {
      for (String cn : classNames) {
         try {
            Class<?> c = Class.forName(cn);

            for (Constructor<?> ctor : c.getDeclaredConstructors()) {
               Class<?>[] p = ctor.getParameterTypes();
               if (p.length == 1 && (arg == null || p[0].isInstance(arg) || isPrimitiveWrapperMatch(p[0], arg.getClass()))) {
                  ctor.setAccessible(true);
                  return ctor.newInstance(arg);
               }
            }
         } catch (Throwable var12) {
         }
      }

      return null;
   }

   private static boolean isPrimitiveWrapperMatch(Class<?> paramType, Class<?> argType) {
      if (!paramType.isPrimitive()) {
         return false;
      } else if (paramType == int.class && argType == Integer.class) {
         return true;
      } else if (paramType == long.class && argType == Long.class) {
         return true;
      } else if (paramType == boolean.class && argType == Boolean.class) {
         return true;
      } else if (paramType == double.class && argType == Double.class) {
         return true;
      } else if (paramType == float.class && argType == Float.class) {
         return true;
      } else if (paramType == short.class && argType == Short.class) {
         return true;
      } else {
         return paramType == byte.class && argType == Byte.class ? true : paramType == char.class && argType == Character.class;
      }
   }
}
