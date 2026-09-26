package br.tones.amigonpc.core.hud.levelprogress;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.progress.NpcLevelProgressSnapshot;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LevelProgressHudService {
   private static final LevelProgressHudService SHARED = new LevelProgressHudService();
   private static final String HUD_ID = "AmigoNPC_LevelProgress";
   private static final long UI_THROTTLE_NANOS = 250000000L;
   private final HudBackend backend;
   private final Map<PlayerRef, LevelProgressHudService.HudState> statesByRef = new ConcurrentHashMap<>();
   private final Map<UUID, PlayerRef> ownerToRef = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> enabledByOwner = new ConcurrentHashMap<>();

   public static LevelProgressHudService getShared() {
      return SHARED;
   }

   private LevelProgressHudService() {
      this.backend = this.detectBackend();
   }

   public void onPlayerJoin(Player player, PlayerRef playerRef, UUID ownerId) {
      if (playerRef != null && ownerId != null) {
         AmigoText.initializePlayerLocale(ownerId, playerRef);
         LevelProgressHudService.HudState st = this.statesByRef.get(playerRef);
         if (st == null) {
            LevelProgressHud hud = new LevelProgressHud(playerRef);
            st = new LevelProgressHudService.HudState(ownerId, player, playerRef, hud);
            this.statesByRef.put(playerRef, st);
         } else {
            st.player = player;
            st.playerRef = playerRef;
            st.ownerId = ownerId;
         }

         this.ownerToRef.put(ownerId, playerRef);
         this.enabledByOwner.put(ownerId, AmigoPersistence.loadHudEnabled(ownerId));

         try {
            NpcLevelProgressSnapshot snap = AmigoNpcManager.getShared().getLevelProgressSnapshot(ownerId);
            String text = AmigoText.formatPlayer(ownerId, "hud.levelprogress.text", snap.level(), snap.xp(), snap.xpNeeded());
            float progress = 0.0F;
            if (snap.xpNeeded() > 0L) {
               progress = (float)((double)Math.max(0L, snap.xp()) / snap.xpNeeded());
               if (progress < 0.0F) {
                  progress = 0.0F;
               }

               if (progress > 1.0F) {
                  progress = 1.0F;
               }
            }

            st.hud.setLevelInfoFromPreformatted(text, progress);
            st.hud.setTextColor(AmigoNpcManager.getShared().getZoneHudColor(ownerId));
         } catch (Throwable var10) {
         }

         if (!this.isEnabled(ownerId)) {
            if (st.isAttached && st.isVisible) {
               try {
                  st.hud.setRootVisible(false);
               } catch (Throwable var8) {
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
               } catch (Throwable var9) {
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
         LevelProgressHudService.HudState st = this.statesByRef.remove(playerRef);
         if (st != null) {
            try {
               this.ownerToRef.remove(st.ownerId);
            } catch (Throwable var7) {
            }

            try {
               if (st.ownerId != null) {
                  this.enabledByOwner.remove(st.ownerId);
               }
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
         playerRef.getPacketHandler().writeNoCache(new CustomHud("AmigoNPC_LevelProgress", 0, true, new CustomUICommand[0]));
      } catch (Throwable var2) {
      }
   }

   public void requestImmediate(UUID ownerId) {
      if (ownerId != null) {
         if (this.isEnabled(ownerId)) {
            PlayerRef pref = this.ownerToRef.get(ownerId);
            if (pref != null) {
               LevelProgressHudService.HudState st = this.statesByRef.get(pref);
               if (st != null) {
                  st.forceNextFlush = true;
                  st.dirty = true;
               }
            }
         }
      }
   }

   public boolean isEnabled(UUID ownerId) {
      if (ownerId == null) {
         return true;
      }

      Boolean enabled = this.enabledByOwner.get(ownerId);
      if (enabled != null) {
         return enabled;
      }

      boolean persisted = true;

      try {
         persisted = AmigoPersistence.loadHudEnabled(ownerId);
      } catch (Throwable var5) {
      }

      this.enabledByOwner.put(ownerId, persisted);
      return persisted;
   }

   public boolean toggleEnabledPersisted(UUID ownerId) {
      boolean newVal = !this.isEnabled(ownerId);
      this.setEnabledPersisted(ownerId, newVal);
      return newVal;
   }

   public void tick() {
      for (LevelProgressHudService.HudState st : this.statesByRef.values()) {
         try {
            UUID ownerId = st.ownerId;
            if (ownerId != null) {
               boolean enabledForPlayer = this.isEnabled(ownerId);
               if (!enabledForPlayer) {
                  if (st.isAttached && st.isVisible) {
                     try {
                        st.hud.setRootVisible(false);
                     } catch (Throwable var20) {
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
                        } catch (Throwable var19) {
                        }
                     } else if (!st.isVisible) {
                        try {
                           st.hud.setRootVisible(true);
                           st.isVisible = true;
                           st.forceNextFlush = true;
                           st.dirty = true;
                        } catch (Throwable var18) {
                        }
                     }
                  }

                  if (st.isVisible) {
                     AmigoText.initializePlayerLocale(ownerId, st.playerRef);
                     NpcLevelProgressSnapshot snap = AmigoNpcManager.getShared().getLevelProgressSnapshot(ownerId);
                     int lvl = snap.level();
                     long xp = snap.xp();
                     long need = snap.xpNeeded();
                     String text = AmigoText.formatPlayer(ownerId, "hud.levelprogress.text", lvl, xp, need);

                     try {
                        int zid = AmigoNpcManager.getShared().getZoneForHud(ownerId);
                        String zname = AmigoNpcManager.getShared().getZoneNameForHud(ownerId);
                        if (zid > 0 && zname != null && !zname.isBlank()) {
                           text = text + AmigoText.formatPlayer(ownerId, "hud.levelprogress.zone_suffix", zname);
                        }
                     } catch (Throwable var17) {
                     }

                     float progress = 0.0F;
                     if (need > 0L) {
                        progress = (float)((double)xp / need);
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
         } catch (Throwable var21) {
         }
      }
   }

   private void flushIfAllowed(LevelProgressHudService.HudState st) {
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

   private void setEnabledPersisted(UUID ownerId, boolean newEnabled) {
      if (ownerId != null) {
         this.enabledByOwner.put(ownerId, newEnabled);

         try {
            AmigoPersistence.saveHudEnabled(ownerId, newEnabled);
         } catch (Throwable var8) {
         }

         PlayerRef playerRef = this.ownerToRef.get(ownerId);
         if (playerRef != null) {
            LevelProgressHudService.HudState st = this.statesByRef.get(playerRef);
            if (st != null) {
               if (!newEnabled) {
                  if (st.isAttached && st.isVisible) {
                     try {
                        st.hud.setRootVisible(false);
                     } catch (Throwable var6) {
                     }

                     st.isVisible = false;
                  }
               } else {
                  try {
                     if (!st.isAttached) {
                        if (st.player != null) {
                           this.backend.show(st.player, st.playerRef, "AmigoNPC_LevelProgress", st.hud);
                        } else {
                           st.hud.show();
                        }

                        st.isAttached = true;
                        st.isVisible = true;
                     } else if (!st.isVisible) {
                        st.hud.setRootVisible(true);
                        st.isVisible = true;
                     }
                  } catch (Throwable var7) {
                  }

                  st.forceNextFlush = true;
                  st.dirty = true;
               }
            }
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
