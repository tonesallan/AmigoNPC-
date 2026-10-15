package br.tones.amigonpc.core.hud.levelprogress;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LevelProgressHudService {
   private static final LevelProgressHudService SHARED = new LevelProgressHudService();
   private static final String HUD_ID = "AmigoNPC_LevelProgress";
   private static final long UI_THROTTLE_NANOS = 250000000L;
   private static final String CONFIG_KEY_ENABLED = "hud.levelProgress.enabled";
   private static final long CONFIG_RELOAD_MS = 2000L;
   private volatile boolean enabled = true;
   private volatile long nextConfigReloadMillis = 0L;
   private final HudBackend backend = this.detectBackend();
   private final Map statesByRef = new ConcurrentHashMap();
   private final Map ownerToRef = new ConcurrentHashMap();

   public static LevelProgressHudService getShared() {
      return SHARED;
   }

   private LevelProgressHudService() {
   }

   public void onPlayerJoin(Player player, PlayerRef playerRef, UUID ownerId) {
      if (playerRef != null && ownerId != null) {
         this.reloadConfigIfDue();
         HudState st = (HudState)this.statesByRef.get(playerRef);
         if (st == null) {
            LevelProgressHud hud = new LevelProgressHud(playerRef);
            st = new HudState(ownerId, player, playerRef, hud);
            this.statesByRef.put(playerRef, st);
         } else {
            st.player = player;
            st.playerRef = playerRef;
            st.ownerId = ownerId;
         }

         this.ownerToRef.put(ownerId, playerRef);

         try {
            AmigoNpcManager.LevelProgressSnapshot snap = AmigoNpcManager.getShared().getLevelProgressSnapshot(ownerId);
            st.hud.setLevelInfo(snap.level(), snap.xp(), snap.xpNeeded());
            st.hud.setTextColor(AmigoNpcManager.getShared().getZoneHudColor(ownerId));
         } catch (Throwable var8) {
         }

         if (!this.enabled) {
            if (st.isAttached && st.isVisible) {
               try {
                  st.hud.setRootVisible(false);
               } catch (Throwable var6) {
               }

               st.isVisible = false;
            }

         } else {
            if (!st.isAttached) {
               if (player != null) {
                  this.backend.show(player, playerRef, "AmigoNPC_LevelProgress", st.hud);
               } else {
                  st.hud.show();
               }

               st.isAttached = true;
               st.isVisible = true;
            } else if (!st.isVisible) {
               try {
                  st.hud.setRootVisible(true);
               } catch (Throwable var7) {
               }

               st.isVisible = true;
            }

            st.forceNextFlush = true;
            st.dirty = true;
         }
      }
   }

   public void onPlayerLeave(Player player, PlayerRef playerRef) {
      if (playerRef != null) {
         HudState st = (HudState)this.statesByRef.remove(playerRef);
         if (st != null) {
            try {
               this.ownerToRef.remove(st.ownerId);
            } catch (Throwable var6) {
            }

            try {
               st.hud.setRootVisible(false);
            } catch (Throwable var5) {
            }
         }

      }
   }

   private static void sendClearCustomHud(PlayerRef playerRef) {
      try {
         playerRef.getPacketHandler().writeNoCache(new CustomHud(true, new CustomUICommand[0]));
      } catch (Throwable var2) {
      }

   }

   public void requestImmediate(UUID ownerId) {
      if (ownerId != null) {
         this.reloadConfigIfDue();
         if (this.enabled) {
            PlayerRef pref = (PlayerRef)this.ownerToRef.get(ownerId);
            if (pref != null) {
               HudState st = (HudState)this.statesByRef.get(pref);
               if (st != null) {
                  st.forceNextFlush = true;
                  st.dirty = true;
               }
            }
         }
      }
   }

   public boolean isEnabled() {
      this.reloadConfigIfDue();
      return this.enabled;
   }

   public boolean toggleEnabledPersisted() {
      this.reloadConfigIfDue();
      boolean newVal = !this.enabled;
      this.setEnabledPersisted(newVal);
      return newVal;
   }

   public void tick() {
      this.reloadConfigIfDue();

      for(HudState st : this.statesByRef.values()) {
         try {
            if (!this.enabled) {
               if (st.isAttached && st.isVisible) {
                  try {
                     st.hud.setRootVisible(false);
                  } catch (Throwable var19) {
                  }

                  st.isVisible = false;
               }
            } else {
               if (st.playerRef != null) {
                  if (!st.isAttached) {
                     try {
                        if (st.player != null) {
                           this.backend.show(st.player, st.playerRef, "AmigoNPC_LevelProgress", st.hud);
                        } else {
                           st.hud.show();
                        }

                        st.isAttached = true;
                        st.isVisible = true;
                        st.forceNextFlush = true;
                        st.dirty = true;
                     } catch (Throwable var18) {
                     }
                  } else if (!st.isVisible) {
                     try {
                        st.hud.setRootVisible(true);
                        st.isVisible = true;
                        st.forceNextFlush = true;
                        st.dirty = true;
                     } catch (Throwable var17) {
                     }
                  }
               }

               if (st.isVisible) {
                  UUID ownerId = st.ownerId;
                  if (ownerId != null) {
                     AmigoNpcManager.LevelProgressSnapshot snap = AmigoNpcManager.getShared().getLevelProgressSnapshot(ownerId);
                     int lvl = snap.level();
                     long xp = snap.xp();
                     long need = snap.xpNeeded();
                     String text = "LEVEL: " + lvl + " XP: " + xp + " / " + need;

                     try {
                        int zid = AmigoNpcManager.getShared().getZoneForHud(ownerId);
                        String zname = AmigoNpcManager.getShared().getZoneNameForHud(ownerId);
                        if (zid > 0 && zname != null && !zname.isBlank()) {
                           text = text + " | Zona: " + zname;
                        }
                     } catch (Throwable var16) {
                     }

                     float progress = 0.0F;
                     if (need > 0L) {
                        progress = (float)((double)xp / (double)need);
                        if (progress < 0.0F) {
                           progress = 0.0F;
                        }

                        if (progress > 1.0F) {
                           progress = 1.0F;
                        }
                     }

                     boolean textChanged = st.lastText == null || !st.lastText.equals(text);
                     boolean progressChanged = Math.abs(progress - st.lastProgress) > 1.0E-4F;
                     Color c = AmigoNpcManager.getShared().getZoneHudColor(ownerId);
                     boolean colorChanged = false;
                     if (c != null) {
                        colorChanged = st.lastColorR != c.red || st.lastColorG != c.green || st.lastColorB != c.blue;
                     }

                     if (textChanged || progressChanged || colorChanged || st.forceNextFlush) {
                        st.pendingText = text;
                        st.pendingProgress = progress;
                        st.pendingColor = c;
                        st.dirty = true;
                        this.flushIfAllowed(st);
                     }
                  }
               }
            }
         } catch (Throwable var20) {
         }
      }

   }

   private void flushIfAllowed(HudState st) {
      if (st.dirty) {
         long now = System.nanoTime();
         if (st.forceNextFlush || now - st.lastUiUpdateNanos >= 250000000L) {
            st.hud.setLevelInfoFromPreformatted(st.pendingText, st.pendingProgress);
            if (st.pendingColor != null) {
               st.hud.setTextColor(st.pendingColor);
            }

            if (this.backend.supportsIncrementalUpdates()) {
               st.hud.requestUpdate();
            } else {
               st.hud.show();
            }

            st.lastUiUpdateNanos = now;
            st.forceNextFlush = false;
            st.dirty = false;
            st.lastText = st.pendingText;
            st.lastProgress = st.pendingProgress;
            if (st.pendingColor != null) {
               st.lastColorR = st.pendingColor.red;
               st.lastColorG = st.pendingColor.green;
               st.lastColorB = st.pendingColor.blue;
            }

         }
      }
   }

   private HudBackend detectBackend() {
      try {
         Object plugin = PluginManager.get().getPlugin(PluginIdentifier.fromString("Buuz135:MultipleHUD"));
         if (plugin != null) {
            return new MultipleHudBackend();
         }
      } catch (Throwable var2) {
      }

      return new NativeHudBackend();
   }

   private void reloadConfigIfDue() {
      long now = System.currentTimeMillis();
      if (now >= this.nextConfigReloadMillis) {
         this.nextConfigReloadMillis = now + 2000L;
         this.enabled = this.readEnabledFromDisk();
      }
   }

   private boolean readEnabledFromDisk() {
      try {
         Path p = Constants.UNIVERSE_PATH.resolve("amigonpc").resolve("config.properties");
         if (!Files.exists(p, new LinkOption[0])) {
            return true;
         } else {
            Properties props = new Properties();
            InputStream in = Files.newInputStream(p);

            try {
               props.load(in);
            } catch (Throwable var7) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (in != null) {
               in.close();
            }

            String v = props.getProperty("hud.levelProgress.enabled");
            if (v == null) {
               return true;
            } else {
               v = v.trim().toLowerCase();
               if (!v.equals("false") && !v.equals("0") && !v.equals("no") && !v.equals("off")) {
                  return !v.equals("true") && !v.equals("1") && !v.equals("yes") && !v.equals("on") ? true : true;
               } else {
                  return false;
               }
            }
         }
      } catch (Throwable var8) {
         return true;
      }
   }

   private void setEnabledPersisted(boolean newEnabled) {
      this.enabled = newEnabled;
      this.nextConfigReloadMillis = System.currentTimeMillis() + 2000L;

      try {
         Path dir = Constants.UNIVERSE_PATH.resolve("amigonpc");
         Path p = dir.resolve("config.properties");
         Files.createDirectories(dir);
         Properties props = new Properties();
         if (Files.exists(p, new LinkOption[0])) {
            try {
               InputStream in = Files.newInputStream(p);

               try {
                  props.load(in);
               } catch (Throwable var12) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var10) {
                        var12.addSuppressed(var10);
                     }
                  }

                  throw var12;
               }

               if (in != null) {
                  in.close();
               }
            } catch (Throwable var13) {
            }
         }

         props.setProperty("hud.levelProgress.enabled", String.valueOf(newEnabled));
         OutputStream out = Files.newOutputStream(p);

         try {
            props.store(out, "AmigoNPC config");
         } catch (Throwable var11) {
            if (out != null) {
               try {
                  out.close();
               } catch (Throwable var9) {
                  var11.addSuppressed(var9);
               }
            }

            throw var11;
         }

         if (out != null) {
            out.close();
         }
      } catch (Throwable var14) {
      }

      if (!newEnabled) {
         for(HudState st : this.statesByRef.values()) {
            if (st.isAttached && st.isVisible) {
               try {
                  st.hud.setRootVisible(false);
               } catch (Throwable var8) {
               }

               st.isVisible = false;
            }
         }
      } else {
         for(HudState st : this.statesByRef.values()) {
            st.forceNextFlush = true;
            st.dirty = true;
         }
      }

   }

   private static final class HudState {
      volatile UUID ownerId;
      volatile Player player;
      volatile PlayerRef playerRef;
      final LevelProgressHud hud;
      volatile boolean isAttached = false;
      volatile boolean isVisible = false;
      volatile String lastText;
      volatile float lastProgress;
      volatile byte lastColorR;
      volatile byte lastColorG;
      volatile byte lastColorB;
      volatile String pendingText;
      volatile float pendingProgress;
      volatile Color pendingColor;
      volatile boolean dirty;
      volatile boolean forceNextFlush;
      volatile long lastUiUpdateNanos;

      HudState(UUID ownerId, Player player, PlayerRef playerRef, LevelProgressHud hud) {
         this.ownerId = ownerId;
         this.player = player;
         this.playerRef = playerRef;
         this.hud = hud;
      }
   }
}
