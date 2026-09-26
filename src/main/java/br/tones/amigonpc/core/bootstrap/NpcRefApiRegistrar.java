package br.tones.amigonpc.core.bootstrap;

import br.tones.amigonpc.api.AmigoNpcRefApi;
import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NpcRefApiRegistrar {
   private transient AmigoNpcRefApi.Provider provider;

   public void register() {
      try {
         final AmigoNpcManager mgr = AmigoNpcManager.getShared();
         AmigoNpcRefApi.Provider p = new AmigoNpcRefApi.Provider() {
            private Field fNpcMap;
            private Class<?> recordClass;
            private Field fRefObj;
            private Field fState;

            private Object getRecord(UUID ownerUuid) {
               try {
                  if (ownerUuid == null) {
                     return null;
                  }

                  if (this.fNpcMap == null) {
                     this.fNpcMap = AmigoNpcManager.class.getDeclaredField("npcRefPorPlayer");
                     this.fNpcMap.setAccessible(true);
                  }

                  Object mapObj = this.fNpcMap.get(mgr);
                  return !(mapObj instanceof Map) ? null : ((Map)mapObj).get(ownerUuid);
               } catch (Throwable ignored) {
                  return null;
               }
            }

            private void ensureRecordFields(Object rec) {
               try {
                  if (rec == null) {
                     return;
                  }

                  if (this.recordClass == rec.getClass() && this.fRefObj != null && this.fState != null) {
                     return;
                  }

                  this.recordClass = rec.getClass();
                  this.fRefObj = this.recordClass.getDeclaredField("refObj");
                  this.fRefObj.setAccessible(true);
                  this.fState = this.recordClass.getDeclaredField("state");
                  this.fState.setAccessible(true);
               } catch (Throwable var3) {
               }
            }

            private Object getRefObj(Object rec) {
               try {
                  this.ensureRecordFields(rec);
                  return this.fRefObj == null ? null : this.fRefObj.get(rec);
               } catch (Throwable ignored) {
                  return null;
               }
            }

            private String getStateName(Object rec) {
               try {
                  this.ensureRecordFields(rec);
                  if (this.fState == null) {
                     return null;
                  }

                  Object st = this.fState.get(rec);
                  return st == null ? null : String.valueOf(st);
               } catch (Throwable ignored) {
                  return null;
               }
            }

            @Override
            public Optional<Ref<EntityStore>> getNpcRef(UUID ownerUuid) {
               try {
                  Object rec = this.getRecord(ownerUuid);
                  if (rec == null) {
                     return Optional.empty();
                  } else {
                     return this.getRefObj(rec) instanceof Ref<EntityStore> ref ? Optional.of(ref) : Optional.empty();
                  }
               } catch (Throwable ignored) {
                  return Optional.empty();
               }
            }

            @Override
            public Optional<Ref<EntityStore>> ensureNpcSpawned(UUID ownerUuid) {
               return this.getNpcRef(ownerUuid);
            }

            @Override
            public boolean isNpcReady(UUID ownerUuid) {
               try {
                  Object rec = this.getRecord(ownerUuid);
                  if (rec == null) {
                     return false;
                  }

                  Object refObj = this.getRefObj(rec);
                  if (!(refObj instanceof Ref)) {
                     return false;
                  }

                  String st = this.getStateName(rec);
                  return "ACTIVE".equals(st);
               } catch (Throwable ignored) {
                  return false;
               }
            }

            @Override
            public Optional<Long> getNpcEntityId(UUID ownerUuid) {
               try {
                  Optional<Ref<EntityStore>> opt = this.getNpcRef(ownerUuid);
                  if (opt.isEmpty()) {
                     return Optional.empty();
                  }

                  Object ref = opt.get();

                  try {
                     Method m = ref.getClass().getMethod("getIndex");
                     Object v = m.invoke(ref);
                     if (v instanceof Number) {
                        return Optional.of(((Number)v).longValue());
                     }
                  } catch (Throwable var6) {
                  }

                  return Optional.empty();
               } catch (Throwable ignored) {
                  return Optional.empty();
               }
            }
         };
         this.provider = p;
         AmigoNpcRefApi.register(p);
      } catch (Throwable var3) {
      }
   }

   public void unregister() {
      try {
         if (this.provider != null) {
            AmigoNpcRefApi.unregister(this.provider);
            this.provider = null;
         }
      } catch (Throwable var2) {
      }
   }
}
