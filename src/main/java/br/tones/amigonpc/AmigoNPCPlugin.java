package br.tones.amigonpc;

import br.tones.amigonpc.api.AmigoNpcRefApi;
import br.tones.amigonpc.commands.AmigoCommand;
import br.tones.amigonpc.commands.AmigoDebugCommand;
import br.tones.amigonpc.commands.AmigoLvlCommand;
import br.tones.amigonpc.commands.AmigoPvpCommand;
import br.tones.amigonpc.commands.AutoLootCommand;
import br.tones.amigonpc.commands.CommandAccess;
import br.tones.amigonpc.commands.LootCommand;
import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.AmigoService;
import br.tones.amigonpc.core.autoloot.AutoLootConfigService;
import br.tones.amigonpc.core.debug.ActionTraceService;
import br.tones.amigonpc.core.debug.ErrorDumpService;
import br.tones.amigonpc.core.debug.ServerErrorHook;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksConfigService;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksService;
import br.tones.amigonpc.core.progress.XpProgression;
import br.tones.amigonpc.core.systems.AmigoDamageAndDownedSystem;
import br.tones.amigonpc.core.systems.AmigoPlayerKillNpcXpSystem;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingConfigService;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfigService;
import br.tones.amigonpc.core.zones.AmigoZonesConfigService;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

public final class AmigoNPCPlugin extends JavaPlugin {
   public static final PluginManifest MANIFEST = PluginManifest.corePlugin(AmigoNPCPlugin.class).build();
   private AmigoService service;
   private ScheduledExecutorService scheduler;
   private ScheduledFuture downedTicker;
   private ScheduledFuture followTicker;
   private ScheduledFuture hudTicker;
   private transient AmigoNpcRefApi.Provider npcRefApiProvider;

   public AmigoNPCPlugin(@Nonnull JavaPluginInit init) {
      super(init);
   }

   protected void setup() {
      this.service = new AmigoService();

      try {
         final AmigoNpcManager mgr = AmigoNpcManager.getShared();
         AmigoNpcRefApi.Provider p = new AmigoNpcRefApi.Provider() {
            private Field fNpcMap;
            private Class recordClass;
            private Field fRefObj;
            private Field fState;

            private Object getRecord(UUID ownerUuid) {
               try {
                  if (ownerUuid == null) {
                     return null;
                  } else {
                     if (this.fNpcMap == null) {
                        this.fNpcMap = AmigoNpcManager.class.getDeclaredField("npcRefPorPlayer");
                        this.fNpcMap.setAccessible(true);
                     }

                     Object mapObj = this.fNpcMap.get(mgr);
                     return !(mapObj instanceof Map) ? null : ((Map)mapObj).get(ownerUuid);
                  }
               } catch (Throwable var3) {
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
               } catch (Throwable var3) {
                  return null;
               }
            }

            private String getStateName(Object rec) {
               try {
                  this.ensureRecordFields(rec);
                  if (this.fState == null) {
                     return null;
                  } else {
                     Object st = this.fState.get(rec);
                     return st == null ? null : String.valueOf(st);
                  }
               } catch (Throwable var3) {
                  return null;
               }
            }

            public Optional getNpcRef(UUID ownerUuid) {
               try {
                  Object rec = this.getRecord(ownerUuid);
                  if (rec == null) {
                     return Optional.empty();
                  } else {
                     Object refObj = this.getRefObj(rec);
                     if (refObj instanceof Ref) {
                        Ref<EntityStore> ref = (Ref)refObj;
                        return Optional.of(ref);
                     } else {
                        return Optional.empty();
                     }
                  }
               } catch (Throwable var5) {
                  return Optional.empty();
               }
            }

            public Optional ensureNpcSpawned(UUID ownerUuid) {
               return this.getNpcRef(ownerUuid);
            }

            public boolean isNpcReady(UUID ownerUuid) {
               try {
                  Object rec = this.getRecord(ownerUuid);
                  if (rec == null) {
                     return false;
                  } else {
                     Object refObj = this.getRefObj(rec);
                     if (!(refObj instanceof Ref)) {
                        return false;
                     } else {
                        String st = this.getStateName(rec);
                        return "ACTIVE".equals(st);
                     }
                  }
               } catch (Throwable var5) {
                  return false;
               }
            }

            public Optional getNpcEntityId(UUID ownerUuid) {
               try {
                  Optional<Ref<EntityStore>> opt = this.getNpcRef(ownerUuid);
                  if (opt.isEmpty()) {
                     return Optional.empty();
                  } else {
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
                  }
               } catch (Throwable var7) {
                  return Optional.empty();
               }
            }
         };
         this.npcRefApiProvider = p;
         AmigoNpcRefApi.register(p);
      } catch (Throwable var13) {
      }

      try {
         ServerErrorHook.install();
      } catch (Throwable var12) {
      }

      try {
         boolean pvp = AmigoPersistence.loadPvpEnabledGlobal();
         AmigoNpcManager.getShared().setPvpEnabled(pvp);
      } catch (Throwable var11) {
      }

      try {
         XpProgression.init();
      } catch (Throwable var10) {
      }

      try {
         NpcXpSourcesConfigService.get();
      } catch (Throwable var9) {
      }

      try {
         PlayerStatTweaksConfigService.get();
      } catch (Throwable var8) {
      }

      try {
         AmigoZonesConfigService.get();
      } catch (Throwable var7) {
      }

      try {
         WorldMobScalingConfigService.get();
      } catch (Throwable var6) {
      }

      try {
         AutoLootConfigService.get();
      } catch (Throwable var5) {
      }

      AmigoCommand amigoCmd = new AmigoCommand(this.service);
      CommandAccess.makePublicRecursive(amigoCmd);
      this.getCommandRegistry().registerCommand(amigoCmd);
      LootCommand lootCmd = new LootCommand();
      CommandAccess.makePublicRecursive(lootCmd);
      this.getCommandRegistry().registerCommand(lootCmd);
      AutoLootCommand autolootCmd = new AutoLootCommand();
      CommandAccess.makePublicRecursive(autolootCmd);
      this.getCommandRegistry().registerCommand(autolootCmd);
      AmigoDebugCommand dbgCmd = AmigoDebugCommand.createAsStandalone();
      CommandAccess.makePublicRecursive(dbgCmd);
      this.getCommandRegistry().registerCommand(dbgCmd);
      this.getCommandRegistry().registerCommand(new AmigoPvpCommand());
      this.getCommandRegistry().registerCommand(new AmigoLvlCommand());
      this.hookLogoutCleanupViaEventRegistry();
      this.hookLoginFailsafeViaEventRegistry();
      this.registerDamageAndDownedSystem();
      this.startDownedTicker();
      this.startFollowTicker();
      this.hookTeleportAutoRespawnViaEventRegistry();
      this.hookHudLifecycleViaEventRegistry();
      this.startHudTicker();
   }

   private void registerDamageAndDownedSystem() {
      try {
         this.getEntityStoreRegistry().registerSystem(new AmigoDamageAndDownedSystem());
         this.getEntityStoreRegistry().registerSystem(new AmigoPlayerKillNpcXpSystem());
      } catch (Throwable var2) {
      }

   }

   private void startDownedTicker() {
      try {
         if (this.scheduler != null) {
            return;
         }

         this.scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "AmigoNPC-DownedTicker");
            t.setDaemon(true);
            return t;
         });
         this.downedTicker = this.scheduler.scheduleAtFixedRate(() -> {
            try {
               AmigoNpcManager.getShared().tickDowned();
            } catch (Throwable var3) {
               Throwable t = var3;

               try {
                  ErrorDumpService.getShared().dumpOnServerError((UUID)null, "ticker:downed", t);
               } catch (Throwable var2) {
               }
            }

         }, 1L, 1L, TimeUnit.SECONDS);
      } catch (Throwable var2) {
      }

   }

   private void startFollowTicker() {
      try {
         if (this.scheduler == null) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
               Thread t = new Thread(r, "AmigoNPC-Ticker");
               t.setDaemon(true);
               return t;
            });
         }

         if (this.followTicker != null) {
            return;
         }

         this.followTicker = this.scheduler.scheduleAtFixedRate(() -> {
            try {
               AmigoNpcManager.getShared().tickFollow();
            } catch (Throwable var3) {
               Throwable t = var3;

               try {
                  ErrorDumpService.getShared().dumpOnServerError((UUID)null, "ticker:follow", t);
               } catch (Throwable var2) {
               }
            }

         }, 200L, 200L, TimeUnit.MILLISECONDS);
      } catch (Throwable var2) {
      }

   }

   private void startHudTicker() {
      try {
         if (this.scheduler == null) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
               Thread t = new Thread(r, "AmigoNPC-Ticker");
               t.setDaemon(true);
               return t;
            });
         }

         if (this.hudTicker != null) {
            return;
         }

         this.hudTicker = this.scheduler.scheduleAtFixedRate(() -> {
            try {
               LevelProgressHudService.getShared().tick();
            } catch (Throwable var3) {
               Throwable t = var3;

               try {
                  ErrorDumpService.getShared().dumpOnServerError((UUID)null, "ticker:hud", t);
               } catch (Throwable var2) {
               }
            }

         }, 250L, 250L, TimeUnit.MILLISECONDS);
      } catch (Throwable var2) {
      }

   }

   protected void shutdown() {
      try {
         if (this.npcRefApiProvider != null) {
            AmigoNpcRefApi.unregister(this.npcRefApiProvider);
            this.npcRefApiProvider = null;
         }
      } catch (Throwable var6) {
      }

      try {
         if (this.downedTicker != null) {
            this.downedTicker.cancel(false);
         }
      } catch (Throwable var5) {
      }

      try {
         if (this.followTicker != null) {
            this.followTicker.cancel(false);
         }
      } catch (Throwable var4) {
      }

      try {
         if (this.hudTicker != null) {
            this.hudTicker.cancel(false);
         }
      } catch (Throwable var3) {
      }

      try {
         if (this.scheduler != null) {
            this.scheduler.shutdownNow();
         }
      } catch (Throwable var2) {
      }

   }

   private void hookHudLifecycleViaEventRegistry() {
      try {
         Object registry = this.getEventRegistry();
         if (registry == null) {
            return;
         }

         String[] joinCandidates = new String[]{"com.hypixel.hytale.server.core.event.events.player.PlayerJoinEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerConnectedEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLoginEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedInEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerSpawnedEvent"};
         String[] leaveCandidates = new String[]{"com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectedEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLeaveEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerQuitEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLogoutEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedOutEvent"};

         for(String cn : joinCandidates) {
            this.tryRegisterHudJoin(registry, cn);
         }

         for(String cn : leaveCandidates) {
            this.tryRegisterHudLeave(registry, cn);
         }
      } catch (Throwable var8) {
      }

   }

   private void tryRegisterHudJoin(Object registry, String eventClassName) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = null;

         for(Method m : registry.getClass().getMethods()) {
            if (m.getName().equals("register") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == Class.class) {
               register = m;
               break;
            }
         }

         if (register == null) {
            return;
         }

         Consumer handler = (evt) -> {
            try {
               UUID uuid = this.extractUuid(evt);
               if (uuid == null) {
                  return;
               }

               try {
                  ActionTraceService.getShared().record(uuid, "session", "join " + evt.getClass().getSimpleName());
               } catch (Throwable var9) {
               }

               Object prObj = this.invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
               PlayerRef pref = prObj instanceof PlayerRef ? (PlayerRef)prObj : null;
               Object pObj = this.invokeNoArg(evt, "getPlayer", "player");
               Player player = pObj instanceof Player ? (Player)pObj : null;
               if (pref != null) {
                  LevelProgressHudService.getShared().onPlayerJoin(player, pref, uuid);
               }

               try {
                  if (player != null) {
                     PlayerStatTweaksService.getShared().onPlayerJoin(player, pref, uuid);
                  }
               } catch (Throwable var8) {
               }
            } catch (Throwable var10) {
            }

         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var9) {
      }

   }

   private void tryRegisterHudLeave(Object registry, String eventClassName) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = null;

         for(Method m : registry.getClass().getMethods()) {
            if (m.getName().equals("register") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == Class.class) {
               register = m;
               break;
            }
         }

         if (register == null) {
            return;
         }

         Consumer handler = (evt) -> {
            try {
               Object prObj = this.invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
               PlayerRef pref = prObj instanceof PlayerRef ? (PlayerRef)prObj : null;
               Object pObj = this.invokeNoArg(evt, "getPlayer", "player");
               Player player = pObj instanceof Player ? (Player)pObj : null;
               if (pref != null) {
                  LevelProgressHudService.getShared().onPlayerLeave(player, pref);
               }

               try {
                  if (player != null) {
                     PlayerStatTweaksService.getShared().onPlayerLeave(player, pref);
                  }
               } catch (Throwable var7) {
               }
            } catch (Throwable var8) {
            }

         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var9) {
      }

   }

   private void hookLogoutCleanupViaEventRegistry() {
      try {
         Object registry = this.getEventRegistry();
         if (registry == null) {
            return;
         }

         String[] candidates = new String[]{"com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectedEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLeaveEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerQuitEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLogoutEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedOutEvent"};

         for(String cn : candidates) {
            this.tryRegister(registry, cn, true);
         }
      } catch (Throwable var7) {
      }

   }

   private void hookLoginFailsafeViaEventRegistry() {
      try {
         Object registry = this.getEventRegistry();
         if (registry == null) {
            return;
         }

         String[] candidates = new String[]{"com.hypixel.hytale.server.core.event.events.player.PlayerJoinEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerConnectedEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLoginEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerLoggedInEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerSpawnedEvent"};

         for(String cn : candidates) {
            this.tryRegister(registry, cn, false);
         }
      } catch (Throwable var7) {
      }

   }

   private void hookTeleportAutoRespawnViaEventRegistry() {
      try {
         Object registry = this.getEventRegistry();
         if (registry == null) {
            return;
         }

         String[] candidates = new String[]{"com.hypixel.hytale.server.core.event.events.player.PlayerTeleportEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerTeleportedEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerChangedWorldEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerWorldChangeEvent", "com.hypixel.hytale.server.core.event.events.player.PlayerDimensionChangeEvent"};

         for(String cn : candidates) {
            this.tryRegisterTeleport(registry, cn);
         }
      } catch (Throwable var7) {
      }

   }

   private void tryRegisterTeleport(Object registry, String eventClassName) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = null;

         for(Method m : registry.getClass().getMethods()) {
            if (m.getName().equals("register") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == Class.class) {
               register = m;
               break;
            }
         }

         if (register == null) {
            return;
         }

         Consumer handler = (evt) -> {
            UUID uuid = this.extractUuid(evt);
            if (uuid != null) {
               Object playerRef = this.invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
               Object worldObj = this.extractWorld(evt, playerRef);
               if (worldObj != null) {
                  try {
                     ActionTraceService.getShared().record(uuid, "session", "teleport/world-change " + evt.getClass().getSimpleName());
                  } catch (Throwable var6) {
                  }

                  AmigoNpcManager.getShared().requestRespawn(worldObj, uuid, playerRef != null ? playerRef : evt);
               }
            }
         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var9) {
      }

   }

   private Object extractWorld(Object evt, Object playerRef) {
      Object w = this.invokeNoArg(evt, "getWorld", "getToWorld", "getDestinationWorld", "getNewWorld", "getUniverseWorld");
      if (w != null) {
         return w;
      } else {
         if (playerRef != null) {
            Object w2 = this.invokeNoArg(playerRef, "getWorld", "getUniverseWorld", "world");
            if (w2 != null) {
               return w2;
            }
         }

         return null;
      }
   }

   private void tryRegister(Object registry, String eventClassName, boolean isLogout) {
      try {
         Class<?> evtClass = Class.forName(eventClassName);
         Method register = null;

         for(Method m : registry.getClass().getMethods()) {
            if (m.getName().equals("register") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == Class.class) {
               register = m;
               break;
            }
         }

         if (register == null) {
            return;
         }

         Consumer handler = (evt) -> {
            UUID uuid = this.extractUuid(evt);
            if (uuid != null) {
               if (isLogout) {
                  try {
                     SimpleItemContainer bag = AmigoNpcManager.getShared().getOrLoadBackpack(uuid);
                     if (bag != null) {
                        AmigoPersistence.saveBackpack(uuid, bag);
                     }
                  } catch (Throwable var6) {
                  }
               }

               AmigoNpcManager.getShared().despawnStored(uuid);
               if (isLogout) {
                  try {
                     ErrorDumpService.getShared().maybeDumpOnDisconnect(uuid, evt);
                  } catch (Throwable var5) {
                  }
               }
            }

         };
         register.invoke(registry, evtClass, handler);
      } catch (Throwable var10) {
      }

   }

   private UUID extractUuid(Object evt) {
      if (evt == null) {
         return null;
      } else {
         Object playerRef = this.invokeNoArg(evt, "getPlayerRef", "getPlayer", "playerRef");
         if (playerRef != null) {
            Object u = this.invokeNoArg(playerRef, "getUuid", "uuid");
            UUID id = this.asUuid(u);
            if (id != null) {
               return id;
            }
         }

         Object u2 = this.invokeNoArg(evt, "getUuid", "getPlayerUuid", "getOwnerId", "getPlayerUUID");
         return this.asUuid(u2);
      }
   }

   private UUID asUuid(Object u) {
      if (u instanceof UUID) {
         return (UUID)u;
      } else {
         if (u instanceof String) {
            String s = (String)u;

            try {
               return UUID.fromString(s);
            } catch (Exception var4) {
            }
         }

         return null;
      }
   }

   private Object invokeNoArg(Object obj, String... names) {
      if (obj == null) {
         return null;
      } else {
         for(String n : names) {
            try {
               Method m = obj.getClass().getMethod(n);
               return m.invoke(obj);
            } catch (Throwable var9) {
               try {
                  Field f = obj.getClass().getField(n);
                  return f.get(obj);
               } catch (Throwable var8) {
               }
            }
         }

         return null;
      }
   }
}
