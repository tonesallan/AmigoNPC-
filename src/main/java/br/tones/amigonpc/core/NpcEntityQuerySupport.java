package br.tones.amigonpc.core;

import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import java.lang.reflect.Method;

final class NpcEntityQuerySupport {
   private NpcEntityQuerySupport() {
   }

   static boolean isValidEntityRef(Object store, Object refObj, NpcEntityQuerySupport.StoreComponentGetter componentGetter) {
      if (store != null && refObj != null) {
         try {
            Object tc = componentGetter.get(store, refObj, TransformComponent.getComponentType());
            return tc != null;
         } catch (Throwable t) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isAliveEntityRef(Object store, Object refObj, NpcEntityQuerySupport.StoreComponentGetter componentGetter) {
      if (!isValidEntityRef(store, refObj, componentGetter)) {
         return false;
      }

      try {
         try {
            Object dc = componentGetter.get(store, refObj, DeathComponent.getComponentType());
            if (dc != null) {
               return false;
            }
         } catch (Throwable var7) {
         }

         if (componentGetter.get(store, refObj, EntityStatMap.getComponentType()) instanceof EntityStatMap stats) {
            try {
               float hp = stats.get(DefaultEntityStatTypes.getHealth()).get();
               return hp > 0.0F;
            } catch (Throwable ignored) {
               return true;
            }
         } else {
            return true;
         }
      } catch (Throwable ignored) {
         return true;
      }
   }

   static double getEntityHeight(Object store, Object refObj, NpcEntityQuerySupport.StoreComponentGetter componentGetter) {
      if (store != null && refObj != null) {
         try {
            if (componentGetter.get(store, refObj, BoundingBox.getComponentType()) instanceof BoundingBox bb) {
               try {
                  Box box = bb.getBoundingBox();
                  if (box != null) {
                     double h = box.height();
                     if (h > 0.05) {
                        return h;
                     }
                  }
               } catch (Throwable var8) {
               }
            }
         } catch (Throwable var9) {
         }

         return 1.8;
      } else {
         return 1.8;
      }
   }

   static double getEntityRadiusXZ(Object store, Object refObj, NpcEntityQuerySupport.StoreComponentGetter componentGetter) {
      if (store != null && refObj != null) {
         try {
            if (componentGetter.get(store, refObj, BoundingBox.getComponentType()) instanceof BoundingBox bb) {
               Object box = null;

               try {
                  box = bb.getBoundingBox();
               } catch (Throwable var12) {
               }

               if (box != null) {
                  double w = readBoxDim(box, "width", "getWidth", "xSize", "getXSize");
                  double d = readBoxDim(box, "depth", "getDepth", "zSize", "getZSize");
                  if (w > 0.05 && d > 0.05) {
                     double r = 0.5 * Math.max(w, d);
                     return clamp(r, 0.2, 2.0);
                  }
               }
            }
         } catch (Throwable var13) {
         }

         return 0.45;
      } else {
         return 0.45;
      }
   }

   static void tickAssistHousekeeping(
      AmigoNpcManager.NpcRecord rec, Object store, long now, long assistGraceMillis, NpcEntityQuerySupport.StoreComponentGetter componentGetter
   ) {
      if (rec != null) {
         if (!rec.defendeEnabled) {
            rec.assistTargetRefObj = null;
            rec.assistUntilMillis = 0L;
         } else if (rec.assistTargetRefObj != null) {
            if (!isAliveEntityRef(store, rec.assistTargetRefObj, componentGetter)) {
               rec.assistTargetRefObj = null;
               rec.assistUntilMillis = now + assistGraceMillis;
            }
         } else {
            if (rec.assistUntilMillis > 0L && now > rec.assistUntilMillis) {
               rec.assistUntilMillis = 0L;
            }
         }
      }
   }

   private static double readBoxDim(Object box, String... methodNames) {
      if (box != null && methodNames != null) {
         for (String name : methodNames) {
            if (name != null && !name.isBlank()) {
               try {
                  Method m = box.getClass().getMethod(name);
                  if (m.invoke(box) instanceof Number n) {
                     double d = n.doubleValue();
                     if (d > 0.0) {
                        return d;
                     }
                  }
               } catch (Throwable var11) {
               }
            }
         }

         return -1.0;
      } else {
         return -1.0;
      }
   }

   private static double clamp(double v, double min, double max) {
      return Math.max(min, Math.min(max, v));
   }

   @FunctionalInterface
   interface StoreComponentGetter {
      Object get(Object var1, Object var2, Object var3);
   }
}
