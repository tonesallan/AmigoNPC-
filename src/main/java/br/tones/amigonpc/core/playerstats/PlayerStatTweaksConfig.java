package br.tones.amigonpc.core.playerstats;

public final class PlayerStatTweaksConfig {
   public boolean enablePlayerStatTweaks = false;
   public boolean disableIfRPGLevelingPresent = true;
   public boolean enableStamina = true;
   public boolean enableMana = true;
   public boolean enableOxygen = true;
   public boolean enableAmmo = true;
   public float staminaAddMax = 0.0F;
   public float staminaMultMax = 0.0F;
   public float manaAddMax = 0.0F;
   public float manaMultMax = 0.0F;
   public float oxygenAddMax = 0.0F;
   public float oxygenMultMax = 0.0F;
   public float ammoAddMax = 0.0F;
   public float ammoMultMax = 0.0F;
   public boolean debugLog = false;

   public static PlayerStatTweaksConfig defaults() {
      return new PlayerStatTweaksConfig();
   }
}
