package br.tones.amigonpc.core.ui.lvlgui;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.CombatMode;
import br.tones.amigonpc.core.hud.levelprogress.LevelProgressHudService;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;

final class AmigoLvlGuiSettingsStateSupport {
   private AmigoLvlGuiSettingsStateSupport() {
   }

   static AmigoLvlGuiSettingsSnapshot loadLiveSettings(UUID ownerId) {
      AmigoNpcManager mgr = AmigoNpcManager.getShared();
      AmigoLvlGuiSettingsSnapshot s = new AmigoLvlGuiSettingsSnapshot();

      try {
         s.autoLoot = mgr.isAutoLootEnabled(ownerId);
      } catch (Throwable var8) {
      }

      try {
         s.combatMode = mgr.getCombatMode(ownerId);
      } catch (Throwable ignored) {
         s.combatMode = CombatMode.PROTECT_OWNER;
      }

      try {
         s.autoWeaponSwitch = mgr.isAutoWeaponSwitchEnabled(ownerId);
      } catch (Throwable ignored) {
      }

      try {
         s.interruptAttacks = mgr.isInterruptAttacksEnabled(ownerId);
      } catch (Throwable ignored) {
      }

      try {
         s.hud = LevelProgressHudService.getShared().isEnabled(ownerId);
      } catch (Throwable var6) {
      }

      try {
         s.godMode = mgr.isGodMode(ownerId);
      } catch (Throwable var5) {
      }

      try {
         s.pvp = mgr.isPvpEnabled();
      } catch (Throwable var4) {
      }

      return s;
   }

   static boolean isDirty(AmigoLvlGuiSettingsSnapshot live, AmigoLvlGuiSettingsSnapshot pending) {
      return live != null && pending != null ? !pending.sameToggles(live) : false;
   }

   static void tryAcquireAdminLockIfNeeded(PlayerRef viewerPlayerRef, UUID ownerId) {
      if (AmigoLvlGuiPermissionSupport.isAdminBestEffort(viewerPlayerRef, ownerId)) {
         try {
            AmigoLvlGuiAdminSettingsLock.tryAcquire(ownerId, AmigoLvlGuiPermissionSupport.bestEffortViewerName(viewerPlayerRef, ownerId));
         } catch (Throwable var3) {
         }
      }
   }

   static boolean ensureAdminLockHeld(PlayerRef viewerPlayerRef, UUID ownerId) {
      try {
         if (!AmigoLvlGuiAdminSettingsLock.isHeldBy(ownerId)) {
            AmigoLvlGuiAdminSettingsLock.tryAcquire(ownerId, AmigoLvlGuiPermissionSupport.bestEffortViewerName(viewerPlayerRef, ownerId));
         }
      } catch (Throwable var6) {
      }

      boolean held = false;

      try {
         held = AmigoLvlGuiAdminSettingsLock.isHeldBy(ownerId);
      } catch (Throwable var5) {
      }

      if (held) {
         try {
            AmigoLvlGuiAdminSettingsLock.touch(ownerId);
         } catch (Throwable var4) {
         }
      }

      return held;
   }
}
