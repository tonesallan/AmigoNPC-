package br.tones.amigonpc.core.optional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AmigoWardrobeValueSupport {
   private AmigoWardrobeValueSupport() {
   }

   static Object safeClone(Object component) {
      if (component == null) {
         return null;
      }

      for (String name : List.of("cloneSerializable", "clone")) {
         try {
            Method m = component.getClass().getMethod(name);
            m.setAccessible(true);
            return m.invoke(component);
         } catch (Throwable var4) {
         }
      }

      return null;
   }

   static Object readFieldBestEffort(Class<?> type, Object instance, String fieldName) {
      try {
         Field f = type.getDeclaredField(fieldName);
         f.setAccessible(true);
         return f.get(instance);
      } catch (Throwable ignored) {
         return null;
      }
   }

   static Map<?, ?> asMap(Object value) {
      return value instanceof Map<?, ?> m ? m : null;
   }

   static Map<Object, Object> asMutableMap(Object value) {
      return (Map<Object, Object>)(value instanceof Map<?, ?> m ? m : null);
   }

   static Set<Object> asMutableSet(Object value) {
      return (Set<Object>)(value instanceof Set<?> s ? s : null);
   }

   static LinkedHashSet<String> toStringSet(Object value) {
      LinkedHashSet<String> out = new LinkedHashSet<>();
      if (value instanceof Iterable) {
         for (Object v : (Iterable)value) {
            String s = normalizeStringLike(v);
            if (s != null && !s.isBlank()) {
               out.add(s);
            }
         }
      }

      return out;
   }

   static byte[] trySerializeJava(Object value) {
      if (value == null) {
         return null;
      }

      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();

         try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
         }

         return bos.toByteArray();
      } catch (Throwable ignored) {
         return null;
      }
   }

   static Object tryDeserializeJava(byte[] bytes) {
      if (bytes != null && bytes.length != 0) {
         try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readObject();
         } catch (Throwable ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   static String tryExtractId(Object value) {
      if (value == null) {
         return null;
      }

      for (String name : List.of("getId", "getCosmeticId")) {
         try {
            Method m = value.getClass().getMethod(name);
            Object v = m.invoke(value);
            if (v != null) {
               return String.valueOf(v);
            }
         } catch (Throwable var6) {
         }
      }

      for (String name : List.of("id", "cosmeticId")) {
         try {
            Field f = value.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(value);
            if (v != null) {
               return String.valueOf(v);
            }
         } catch (Throwable var5) {
         }
      }

      return null;
   }

   static String tryExtractOptionId(Object value) {
      return tryExtractStringValue(value, List.of("getOptionId", "getOption"), List.of("optionId", "option"));
   }

   static String tryExtractVariantId(Object value) {
      return tryExtractStringValue(value, List.of("getVariantId", "getVariant"), List.of("variantId", "variant"));
   }

   static String tryExtractStringValue(Object value, List<String> methodNames, List<String> fieldNames) {
      if (value == null) {
         return null;
      }

      for (String name : methodNames) {
         try {
            Method m = value.getClass().getMethod(name);
            Object v = m.invoke(value);
            String resolved = normalizeStringLike(v);
            if (resolved != null && !resolved.isBlank()) {
               return resolved;
            }
         } catch (Throwable var9) {
         }
      }

      for (String name : fieldNames) {
         try {
            Field f = value.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(value);
            String resolved = normalizeStringLike(v);
            if (resolved != null && !resolved.isBlank()) {
               return resolved;
            }
         } catch (Throwable var8) {
         }
      }

      return null;
   }

   static String normalizeStringLike(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof CharSequence seq) {
         return seq.toString();
      } else {
         try {
            Method m = value.getClass().getMethod("getId");
            Object v = m.invoke(value);
            if (v != null) {
               return String.valueOf(v);
            }
         } catch (Throwable var4) {
         }

         try {
            Field f = value.getClass().getDeclaredField("id");
            f.setAccessible(true);
            Object v = f.get(value);
            if (v != null) {
               return String.valueOf(v);
            }
         } catch (Throwable var3) {
         }

         return String.valueOf(value);
      }
   }
}
