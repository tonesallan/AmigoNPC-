package br.tones.amigonpc.core.autoloot;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AutoLootConfig {
   public boolean EnableCategoryFilter = true;
   public int VerticalScanBlocks = 10;
   public Map<String, Boolean> Categories = new LinkedHashMap<>();

   public static AutoLootConfig defaults() {
      return new AutoLootConfig();
   }
}
