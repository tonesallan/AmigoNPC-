package br.tones.amigonpc.core.optional;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSettings;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

final class AmigoWardrobeModelSupport {
   private static final String CLS_PLAYER_WARDROBE_API = "dev.hardaway.wardrobe.api.player.PlayerWardrobe";
   private static final String CLS_WARDROBE_SYSTEM_TICK = "dev.hardaway.wardrobe.impl.player.PlayerWardrobeSystems$Tick";

   private AmigoWardrobeModelSupport() {
   }

   static Object buildOwnerWardrobeModel(Store<EntityStore> store, Ref<EntityStore> ownerRef, Class<?> pwcClass) {
      if (store != null && ownerRef != null && pwcClass != null) {
         try {
            Object wardrobeComponentType = pwcClass.getMethod("getComponentType").invoke(null);
            Object wardrobe = invokeStoreComponentMethod(store, "ensureAndGetComponent", ownerRef, wardrobeComponentType);
            if (wardrobe == null) {
               return null;
            } else {
               Player ownerPlayer = (Player)store.getComponent(ownerRef, Player.getComponentType());
               PlayerSettings settings = (PlayerSettings)store.getComponent(ownerRef, PlayerSettings.getComponentType());
               PlayerSkinComponent skin = (PlayerSkinComponent)store.getComponent(ownerRef, PlayerSkinComponent.getComponentType());
               PlayerRef ownerPlayerRef = (PlayerRef)store.getComponent(ownerRef, PlayerRef.getComponentType());
               if (ownerPlayer != null && settings != null && skin != null && ownerPlayerRef != null) {
                  Class<?> tickSystemClass = Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeSystems$Tick");
                  Class<?> playerWardrobeApiClass = Class.forName("dev.hardaway.wardrobe.api.player.PlayerWardrobe");
                  Method buildWardrobeModel = findBuildWardrobeModel(tickSystemClass, playerWardrobeApiClass);
                  return buildWardrobeModel == null
                     ? null
                     : buildWardrobeModel.invoke(null, ownerPlayer, settings, skin.getPlayerSkin(), wardrobe, ownerPlayerRef);
               } else {
                  return null;
               }
            }
         } catch (Throwable ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   static void forceWardrobeVisualRefresh(Object wardrobe) {
      for (String methodName : List.of("rebuild", "update", "markDirty", "refresh", "sync")) {
         tryInvokeNoArg(wardrobe, methodName);
      }
   }

   static void tryApplyWardrobeModelTo(Store<EntityStore> store, Ref<EntityStore> ownerRef, Ref<EntityStore> npcRef, Object wardrobe) {
      if (store != null && ownerRef != null && npcRef != null && wardrobe != null) {
         try {
            Player ownerPlayer = (Player)store.getComponent(ownerRef, Player.getComponentType());
            PlayerSettings settings = (PlayerSettings)store.getComponent(ownerRef, PlayerSettings.getComponentType());
            PlayerSkinComponent skin = (PlayerSkinComponent)store.getComponent(ownerRef, PlayerSkinComponent.getComponentType());
            PlayerRef ownerPlayerRef = (PlayerRef)store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (ownerPlayer == null || settings == null || skin == null || ownerPlayerRef == null) {
               return;
            }

            Class<?> tickSystemClass = Class.forName("dev.hardaway.wardrobe.impl.player.PlayerWardrobeSystems$Tick");
            Class<?> playerWardrobeApiClass = Class.forName("dev.hardaway.wardrobe.api.player.PlayerWardrobe");
            Method buildWardrobeModel = findBuildWardrobeModel(tickSystemClass, playerWardrobeApiClass);
            if (buildWardrobeModel == null) {
               return;
            }

            if (!(buildWardrobeModel.invoke(null, ownerPlayer, settings, skin.getPlayerSkin(), wardrobe, ownerPlayerRef) instanceof Model model)) {
               return;
            }

            putOrSetComponent(store, npcRef, ModelComponent.getComponentType(), new ModelComponent(model));
            tryApplyModelViaNpcPlugin(store, npcRef, model);
         } catch (Throwable var13) {
         }
      }
   }

   static Object invokeStoreComponentMethod(Store<EntityStore> store, String methodName, Ref<EntityStore> ref, Object componentType) throws Exception {
      Method m = Store.class.getMethod(methodName, Ref.class, ComponentType.class);
      return m.invoke(store, ref, componentType);
   }

   static void putOrSetComponent(Store<EntityStore> store, Ref<EntityStore> ref, Object componentType, Object component) {
      for (String methodName : List.of("putComponent", "setComponent")) {
         try {
            Method m = Store.class.getMethod(methodName, Ref.class, ComponentType.class, Object.class);
            m.invoke(store, ref, componentType, component);
            return;
         } catch (Throwable var7) {
         }
      }
   }

   private static Method findBuildWardrobeModel(Class<?> tickSystemClass, Class<?> playerWardrobeApiClass) {
      try {
         return tickSystemClass.getMethod("buildWardrobeModel", Player.class, PlayerSettings.class, PlayerSkin.class, playerWardrobeApiClass, PlayerRef.class);
      } catch (Throwable ignored) {
         for (Method m : tickSystemClass.getMethods()) {
            if (m.getName().equals("buildWardrobeModel") && Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 5) {
               return m;
            }
         }

         return null;
      }
   }

   private static void tryApplyModelViaNpcPlugin(Store<EntityStore> store, Ref<EntityStore> npcRef, Model model) {
      try {
         Class<?> npcPluginClass = Class.forName("com.hypixel.hytale.server.npc.NPCPlugin");
         Object npcPlugin = npcPluginClass.getMethod("get").invoke(null);
         if (npcPlugin == null) {
            return;
         }

         for (Method m : npcPluginClass.getMethods()) {
            String n = m.getName().toLowerCase();
            if (n.contains("model") && (n.contains("set") || n.contains("apply") || n.contains("update"))) {
               Class<?>[] p = m.getParameterTypes();
               if (p.length >= 2 && p.length <= 4) {
                  Object[] args = new Object[p.length];
                  boolean hasRef = false;
                  boolean hasModel = false;
                  boolean ok = true;

                  for (int i = 0; i < p.length; i++) {
                     Class<?> pt = p[i];
                     if (!hasRef && pt.isInstance(npcRef)) {
                        args[i] = npcRef;
                        hasRef = true;
                     } else if (!hasModel && pt.isInstance(model)) {
                        args[i] = model;
                        hasModel = true;
                     } else {
                        if (!pt.isInstance(store)) {
                           ok = false;
                           break;
                        }

                        args[i] = store;
                     }
                  }

                  if (ok && hasRef && hasModel) {
                     m.invoke(npcPlugin, args);
                     return;
                  }
               }
            }
         }
      } catch (Throwable var17) {
      }
   }

   private static void tryInvokeNoArg(Object instance, String methodName) {
      if (instance != null && methodName != null && !methodName.isBlank()) {
         try {
            Method m = instance.getClass().getMethod(methodName);
            m.setAccessible(true);
            m.invoke(instance);
         } catch (Throwable var3) {
         }
      }
   }
}
