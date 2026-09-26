package br.tones.amigonpc.core.rewards;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public final class AmigoLevelRewardsConfig {
   @SerializedName("Version")
   public String Version = "amigonpc-1";
   @SerializedName("RewardTarget")
   public String RewardTarget = "NPC";
   @SerializedName("DebugRewardsLogging")
   public boolean DebugRewardsLogging = false;
   @SerializedName("Rewards")
   public List<AmigoLevelRewardsConfig.RewardEntry> Rewards = new ArrayList<>();

   public static final class ItemEntry {
      @SerializedName("ItemId")
      public String ItemId = "";
      @SerializedName("Quantity")
      public int Quantity = 0;
   }

   public static final class RewardEntry {
      @SerializedName("Level")
      public int Level = 0;
      @SerializedName("Items")
      public List<AmigoLevelRewardsConfig.ItemEntry> Items = new ArrayList<>();
      @SerializedName("ResetPoints")
      public int ResetPoints = 0;
      @SerializedName("Command")
      public String Command = "";
      @SerializedName("CommandTitle")
      public String CommandTitle = "";
   }
}
