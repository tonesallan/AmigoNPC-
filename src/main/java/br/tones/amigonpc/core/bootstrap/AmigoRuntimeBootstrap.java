package br.tones.amigonpc.core.bootstrap;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;
import br.tones.amigonpc.core.autoloot.AutoLootConfigService;
import br.tones.amigonpc.core.debug.ServerErrorHook;
import br.tones.amigonpc.core.i18n.AmigoText;
import br.tones.amigonpc.core.playerstats.PlayerStatTweaksConfigService;
import br.tones.amigonpc.core.progress.XpProgression;
import br.tones.amigonpc.core.worldscaling.WorldMobScalingConfigService;
import br.tones.amigonpc.core.xp.sources.NpcXpSourcesConfigService;
import br.tones.amigonpc.core.zones.AmigoZonesConfigService;

public final class AmigoRuntimeBootstrap {
   public void initialize() {
      try {
         AmigoText.reloadConfiguredLocale();
      } catch (Throwable var10) {
      }

      try {
         ServerErrorHook.install();
      } catch (Throwable var9) {
      }

      try {
         boolean pvp = AmigoPersistence.loadPvpEnabledGlobal();
         AmigoNpcManager.getShared().setPvpEnabled(pvp);
      } catch (Throwable var8) {
      }

      try {
         XpProgression.init();
      } catch (Throwable var7) {
      }

      try {
         NpcXpSourcesConfigService.get();
      } catch (Throwable var6) {
      }

      try {
         PlayerStatTweaksConfigService.get();
      } catch (Throwable var5) {
      }

      try {
         AmigoZonesConfigService.get();
      } catch (Throwable var4) {
      }

      try {
         WorldMobScalingConfigService.get();
      } catch (Throwable var3) {
      }

      try {
         AutoLootConfigService.get();
      } catch (Throwable var2) {
      }
   }
}
