package br.tones.amigonpc.core.ui.lvlgui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;

public final class AmigoLvlGuiEventData {
   public static final BuilderCodec<AmigoLvlGuiEventData> CODEC = ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)BuilderCodec.builder(
                                       AmigoLvlGuiEventData.class, AmigoLvlGuiEventData::new
                                    )
                                    .append(new KeyedCodec("StatName", Codec.STRING), AmigoLvlGuiEventData::setStatName, AmigoLvlGuiEventData::getStatName)
                                    .add())
                                 .append(new KeyedCodec("Action", Codec.STRING), AmigoLvlGuiEventData::setAction, AmigoLvlGuiEventData::getAction)
                                 .add())
                              .append(
                                 new KeyedCodec("Amount", Codec.STRING), AmigoLvlGuiEventData::setAmountFromString, AmigoLvlGuiEventData::getAmountAsString
                              )
                              .add())
                           .append(new KeyedCodec("NavBar", Codec.STRING), AmigoLvlGuiEventData::setNavBar, AmigoLvlGuiEventData::getNavBar)
                           .add())
                        .append(
                           new KeyedCodec("ClaimLevel", Codec.STRING),
                           AmigoLvlGuiEventData::setClaimLevelFromString,
                           AmigoLvlGuiEventData::getClaimLevelAsString
                        )
                        .add())
                     .append(new KeyedCodec("@ModelId", Codec.STRING), AmigoLvlGuiEventData::setModelId, AmigoLvlGuiEventData::getModelId)
                     .add())
                  .append(new KeyedCodec("@Scale", Codec.STRING), AmigoLvlGuiEventData::setScaleFromString, AmigoLvlGuiEventData::getScaleAsString)
                  .add())
               .append(new KeyedCodec("@NpcName", Codec.STRING), AmigoLvlGuiEventData::setNpcName, AmigoLvlGuiEventData::getNpcName)
               .add())
            .append(new KeyedCodec("@Language", Codec.STRING), AmigoLvlGuiEventData::setLanguage, AmigoLvlGuiEventData::getLanguage)
            .add())
         .append(new KeyedCodec("@LanguageText", Codec.STRING), AmigoLvlGuiEventData::setLanguageText, AmigoLvlGuiEventData::getLanguageText)
         .add())
      .build();
   public String statName;
   public String action;
   public Integer amount;
   public String navBar;
   public Integer claimLevel;
   public String modelId;
   public Double scale;
   public String npcName;
   public String language;
   public String languageText;

   private void setStatName(String s) {
      this.statName = AmigoLvlGuiEventDataValueSupport.stringOrDefault(s, "");
   }

   private String getStatName() {
      return AmigoLvlGuiEventDataValueSupport.stringOrDefault(this.statName, "");
   }

   private void setAction(String s) {
      this.action = AmigoLvlGuiEventDataValueSupport.stringOrDefault(s, "");
   }

   private String getAction() {
      return AmigoLvlGuiEventDataValueSupport.stringOrDefault(this.action, "");
   }

   private void setNavBar(String s) {
      this.navBar = AmigoLvlGuiEventDataValueSupport.stringOrDefault(s, "");
   }

   private String getNavBar() {
      return AmigoLvlGuiEventDataValueSupport.stringOrDefault(this.navBar, "");
   }

   private void setAmountFromString(String raw) {
      this.amount = AmigoLvlGuiEventDataValueSupport.parseIntegerOrDefault(raw, 0);
   }

   private String getAmountAsString() {
      return AmigoLvlGuiEventDataValueSupport.intToString(this.amount, "0");
   }

   private void setClaimLevelFromString(String raw) {
      this.claimLevel = AmigoLvlGuiEventDataValueSupport.parseIntegerOrDefault(raw, null);
   }

   private String getClaimLevelAsString() {
      return AmigoLvlGuiEventDataValueSupport.intToString(this.claimLevel, "");
   }

   private void setModelId(String s) {
      this.modelId = AmigoLvlGuiEventDataValueSupport.stringOrDefault(s, "");
   }

   private String getModelId() {
      return AmigoLvlGuiEventDataValueSupport.stringOrDefault(this.modelId, "");
   }

   private void setScaleFromString(String raw) {
      this.scale = AmigoLvlGuiEventDataValueSupport.parseDoubleOrNull(raw);
   }

   private String getScaleAsString() {
      return AmigoLvlGuiEventDataValueSupport.doubleToString(this.scale, "");
   }

   private void setNpcName(String s) {
      this.npcName = AmigoLvlGuiEventDataValueSupport.stringOrDefault(s, "");
   }

   private String getNpcName() {
      return AmigoLvlGuiEventDataValueSupport.stringOrDefault(this.npcName, "");
   }

   private void setLanguage(String s) {
      this.language = AmigoLvlGuiEventDataValueSupport.stringOrDefault(s, "");
   }

   private String getLanguage() {
      return AmigoLvlGuiEventDataValueSupport.stringOrDefault(this.language, "");
   }

   private void setLanguageText(String s) {
      this.languageText = AmigoLvlGuiEventDataValueSupport.stringOrDefault(s, "");
   }

   private String getLanguageText() {
      return AmigoLvlGuiEventDataValueSupport.stringOrDefault(this.languageText, "");
   }
}
