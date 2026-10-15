package br.tones.amigonpc.core.zones;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;

public final class ZoneResolver {
   private ZoneResolver() {
   }

   public static ZoneContext resolve(Object worldObj, Store store, Object ownerRefObj) {
      int zoneId = 0;
      int instMin = 0;
      int instMax = 0;
      String biomeName = null;
      String instanceId = null;
      Vector3d pos = null;

      try {
         if (store != null && ownerRefObj instanceof Ref ref) {
            TransformComponent tc = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
            if (tc != null) {
               pos = tc.getPosition();
            }
         }
      } catch (Throwable var13) {
      }

      if (worldObj != null && pos != null) {
         ZoneBiome wb = tryResolveZoneBiomeViaWorldgenCache(worldObj, pos);
         if (wb != null && wb.zoneId > 0) {
            zoneId = wb.zoneId;
            biomeName = wb.biomeName;
         } else {
            zoneId = tryResolveZoneId(worldObj, pos);
         }

         try {
            Object n = invoke(worldObj, "getName", new Class[0], new Object[0]);
            if (n instanceof String) {
               String s = (String)n;
               if (!s.isBlank()) {
                  instanceId = s;
               }
            }
         } catch (Throwable var12) {
         }
      }

      return new ZoneContext(zoneId, instMin, instMax, biomeName, instanceId);
   }

   private static ZoneBiome tryResolveZoneBiomeViaWorldgenCache(Object worldObj, Vector3d pos) {
      if (worldObj != null && pos != null) {
         int seedInt;
         try {
            Object wc = invoke(worldObj, "getWorldConfig", new Class[0], new Object[0]);
            Object seedObj = wc != null ? invoke(wc, "getSeed", new Class[0], new Object[0]) : null;
            seedInt = toSeedInt(seedObj);
         } catch (Throwable var17) {
            return null;
         }

         try {
            Object chunkStore = invoke(worldObj, "getChunkStore", new Class[0], new Object[0]);
            Object generator = null;
            if (chunkStore != null) {
               generator = safeInvokeNoArg(chunkStore, "getGenerator");
               if (generator == null) {
                  generator = safeInvokeNoArg(chunkStore, "getWorldGen");
               }
            }

            Class<?> cacheClass = Class.forName("com.hypixel.hytale.server.worldgen.cache.ChunkGeneratorCache");
            Object cache = firstInstanceOf(cacheClass, 3, worldObj, chunkStore, generator);
            if (cache == null) {
               return null;
            }

            Method m = cacheClass.getMethod("getZoneBiomeResult", Integer.TYPE, Integer.TYPE, Integer.TYPE);
            int xBlock = (int)Math.floor(pos.getX());
            int zBlock = (int)Math.floor(pos.getZ());
            int[][] coords = new int[][]{{xBlock, zBlock}, {xBlock >> 4, zBlock >> 4}, {xBlock >> 5, zBlock >> 5}};

            for(int[] cz : coords) {
               Object zr = m.invoke(cache, seedInt, cz[0], cz[1]);
               ZoneBiome out = extractZoneBiome(zr);
               if (out != null && out.zoneId > 0) {
                  return out;
               }
            }
         } catch (Throwable var18) {
         }

         return null;
      } else {
         return null;
      }
   }

   private static ZoneBiome extractZoneBiome(Object zoneBiomeResult) {
      if (zoneBiomeResult == null) {
         return null;
      } else {
         try {
            Object zone = safeInvokeNoArg(zoneBiomeResult, "getZone");
            int zoneId = 0;
            if (zone != null) {
               Object idObj = safeInvokeNoArg(zone, "id");
               if (idObj instanceof Integer) {
                  Integer i = (Integer)idObj;
                  zoneId = i;
               } else if (idObj instanceof Short) {
                  Short s = (Short)idObj;
                  zoneId = s;
               } else if (idObj instanceof Byte) {
                  Byte b = (Byte)idObj;
                  zoneId = b;
               }
            }

            String biomeName = null;
            Object biome = safeInvokeNoArg(zoneBiomeResult, "getBiome");
            if (biome != null) {
               Object nameObj = safeInvokeNoArg(biome, "getName");
               if (nameObj instanceof String) {
                  String s = (String)nameObj;
                  if (!s.isBlank()) {
                     biomeName = s;
                  }
               }
            }

            return zoneId <= 0 ? null : new ZoneBiome(zoneId, biomeName);
         } catch (Throwable var7) {
            return null;
         }
      }
   }

   private static int toSeedInt(Object seedObj) {
      if (seedObj == null) {
         return 0;
      } else if (seedObj instanceof Integer) {
         Integer i = (Integer)seedObj;
         return i;
      } else if (seedObj instanceof Long) {
         Long l = (Long)seedObj;
         return (int)(l ^ l >>> 32);
      } else if (seedObj instanceof String) {
         String s = (String)seedObj;
         return s.hashCode();
      } else {
         return seedObj.toString().hashCode();
      }
   }

   private static Object safeInvokeNoArg(Object target, String name) {
      if (target == null) {
         return null;
      } else {
         try {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
         } catch (Throwable var3) {
            return null;
         }
      }
   }

   private static Object firstInstanceOf(Class targetType, int maxDepth, Object... roots) {
      if (targetType != null && maxDepth > 0 && roots != null) {
         IdentityHashMap<Object, Boolean> seen = new IdentityHashMap();
         ArrayDeque<ObjectDepth> q = new ArrayDeque();

         for(Object r : roots) {
            if (r != null) {
               q.add(new ObjectDepth(r, 0));
            }
         }

         while(!q.isEmpty()) {
            ObjectDepth od = (ObjectDepth)q.removeFirst();
            Object obj = od.obj;
            int depth = od.depth;
            if (obj != null && seen.put(obj, Boolean.TRUE) == null) {
               if (targetType.isInstance(obj)) {
                  return obj;
               }

               if (depth < maxDepth) {
                  Class<?> c = obj.getClass();
                  if (!c.getName().startsWith("java.")) {
                     for(Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                        Field[] fs;
                        try {
                           fs = k.getDeclaredFields();
                        } catch (Throwable var17) {
                           continue;
                        }

                        for(Field f : fs) {
                           try {
                              if (!Modifier.isStatic(f.getModifiers()) && !f.getType().isPrimitive()) {
                                 f.setAccessible(true);
                                 Object v = f.get(obj);
                                 if (v != null) {
                                    q.addLast(new ObjectDepth(v, depth + 1));
                                 }
                              }
                           } catch (Throwable var16) {
                           }
                        }
                     }
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static int tryResolveZoneId(Object worldObj, Vector3d pos) {
      try {
         Object v = invoke(worldObj, "getZoneIdAt", new Class[]{Vector3d.class}, new Object[]{pos});
         if (v instanceof Integer i) {
            return i;
         }

         if (v instanceof Short s) {
            return s;
         }
      } catch (Throwable var6) {
      }

      try {
         Object v = invoke(worldObj, "getZoneIdAt", new Class[]{Double.TYPE, Double.TYPE, Double.TYPE}, new Object[]{pos.getX(), pos.getY(), pos.getZ()});
         if (v instanceof Integer i) {
            return i;
         }
      } catch (Throwable var5) {
      }

      try {
         Object v = invoke(worldObj, "getZoneId", new Class[]{Vector3d.class}, new Object[]{pos});
         if (v instanceof Integer i) {
            return i;
         }
      } catch (Throwable var4) {
      }

      return 0;
   }

   private static Object invoke(Object target, String name, Class[] sig, Object[] args) throws Exception {
      Method m = target.getClass().getMethod(name, sig);
      return m.invoke(target, args);
   }

   public static record ZoneContext(int zoneId, int instanceLevelMin, int instanceLevelMax, String biomeName, String instanceId) {
   }

   private static final class ZoneBiome {
      final int zoneId;
      final String biomeName;

      ZoneBiome(int zoneId, String biomeName) {
         this.zoneId = zoneId;
         this.biomeName = biomeName;
      }
   }

   private static record ObjectDepth(Object obj, int depth) {
   }
}
