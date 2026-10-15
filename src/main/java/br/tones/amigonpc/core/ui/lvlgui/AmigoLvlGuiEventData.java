package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class AmigoLvlGuiEventData {
   public static final BuilderCodec CODEC;
   public String statName;
   public String action;
   public Integer amount;
   public String navBar;
   public Integer claimLevel;
   public String modelId;
   public Double scale;

   private void setStatName(String s) {
      this.statName = s != null ? s : "";
   }

   private String getStatName() {
      return this.statName != null ? this.statName : "";
   }

   private void setAction(String s) {
      this.action = s != null ? s : "";
   }

   private String getAction() {
      return this.action != null ? this.action : "";
   }

   private void setNavBar(String s) {
      this.navBar = s != null ? s : "";
   }

   private String getNavBar() {
      return this.navBar != null ? this.navBar : "";
   }

   private void setAmountFromString(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            this.amount = Integer.parseInt(raw);
         } catch (NumberFormatException var3) {
            this.amount = 0;
         }

      } else {
         this.amount = 0;
      }
   }

   private String getAmountAsString() {
      return this.amount != null ? String.valueOf(this.amount) : "0";
   }

   private void setClaimLevelFromString(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            this.claimLevel = Integer.parseInt(raw.trim());
         } catch (NumberFormatException var3) {
            this.claimLevel = null;
         }

      } else {
         this.claimLevel = null;
      }
   }

   private String getClaimLevelAsString() {
      return this.claimLevel != null ? String.valueOf(this.claimLevel) : "";
   }

   private void setModelId(String s) {
      this.modelId = s != null ? s : "";
   }

   private String getModelId() {
      return this.modelId != null ? this.modelId : "";
   }

   private void setScaleFromString(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            this.scale = Double.parseDouble(raw.trim());
         } catch (NumberFormatException var3) {
            this.scale = null;
         }

      } else {
         this.scale = null;
      }
   }

   private String getScaleAsString() {
      return this.scale != null ? String.valueOf(this.scale) : "";
   }

   static {
      CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(AmigoLvlGuiEventData.class, AmigoLvlGuiEventData::new).append(new KeyedCodec("StatName", Codec.STRING), AmigoLvlGuiEventData::setStatName, AmigoLvlGuiEventData::getStatName).add()).append(new KeyedCodec("Action", Codec.STRING), AmigoLvlGuiEventData::setAction, AmigoLvlGuiEventData::getAction).add()).append(new KeyedCodec("Amount", Codec.STRING), AmigoLvlGuiEventData::setAmountFromString, AmigoLvlGuiEventData::getAmountAsString).add()).append(new KeyedCodec("NavBar", Codec.STRING), AmigoLvlGuiEventData::setNavBar, AmigoLvlGuiEventData::getNavBar).add()).append(new KeyedCodec("ClaimLevel", Codec.STRING), AmigoLvlGuiEventData::setClaimLevelFromString, AmigoLvlGuiEventData::getClaimLevelAsString).add()).append(new KeyedCodec("@ModelId", Codec.STRING), AmigoLvlGuiEventData::setModelId, AmigoLvlGuiEventData::getModelId).add()).append(new KeyedCodec("@Scale", Codec.STRING), AmigoLvlGuiEventData::setScaleFromString, AmigoLvlGuiEventData::getScaleAsString).add()).build();
   }
}
