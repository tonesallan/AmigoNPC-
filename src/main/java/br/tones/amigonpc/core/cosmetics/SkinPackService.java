package br.tones.amigonpc.core.cosmetics;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.asset.AssetModule;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SkinPackService {
   private static final String SKINS_DIRECTORY_NAME = "Skins";
   private final List<String> registeredPackIds = new ArrayList<>();
   private Path skinsDirectory;

   public Path getSkinsDirectory() {
      return this.skinsDirectory;
   }

   public void loadFromPluginFile(Path pluginFile) {
      this.shutdown();
      if (pluginFile == null || pluginFile.getParent() == null) {
         return;
      }

      Path modsDirectory = pluginFile.toAbsolutePath().normalize().getParent();
      Path skins = modsDirectory.resolve(SKINS_DIRECTORY_NAME);
      this.skinsDirectory = skins;

      try {
         Files.createDirectories(skins);
      } catch (Throwable ignored) {
         return;
      }

      List<Path> zipFiles = new ArrayList<>();
      try (var stream = Files.list(skins)) {
         stream.filter(Files::isRegularFile)
            .filter(SkinPackService::isZip)
            .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
            .forEach(zipFiles::add);
      } catch (Throwable ignored) {
         return;
      }

      for (Path zip : zipFiles) {
         this.registerSkinPack(zip);
      }
   }

   public void shutdown() {
      if (this.registeredPackIds.isEmpty()) {
         return;
      }

      AssetModule module;
      try {
         module = AssetModule.get();
      } catch (Throwable ignored) {
         this.registeredPackIds.clear();
         return;
      }

      for (int i = this.registeredPackIds.size() - 1; i >= 0; i--) {
         String packId = this.registeredPackIds.get(i);
         try {
            if (packId != null && module.getAssetPack(packId) != null) {
               module.unregisterPack(packId);
            }
         } catch (Throwable ignored) {
         }
      }

      this.registeredPackIds.clear();
   }

   private void registerSkinPack(Path zip) {
      PluginManifest manifest = readManifest(zip);
      if (manifest == null || manifest.getGroup() == null || manifest.getGroup().isBlank()
         || manifest.getName() == null || manifest.getName().isBlank()) {
         return;
      }

      String packId;
      try {
         packId = new PluginIdentifier(manifest).toString();
      } catch (Throwable ignored) {
         return;
      }

      try {
         AssetModule module = AssetModule.get();
         if (module == null || module.getAssetPack(packId) != null) {
            return;
         }

         module.registerPack(packId, zip, manifest, true);
         if (module.getAssetPack(packId) != null) {
            this.registeredPackIds.add(packId);
         }
      } catch (Throwable ignored) {
      }
   }

   private static PluginManifest readManifest(Path zip) {
      if (zip == null || !Files.isRegularFile(zip)) {
         return null;
      }

      try (FileSystem fs = FileSystems.newFileSystem(zip, (ClassLoader)null)) {
         Path manifestPath = fs.getPath("manifest.json");
         if (!Files.isRegularFile(manifestPath)) {
            return null;
         }

         try (
            BufferedReader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8);
            RawJsonReader json = new RawJsonReader(reader, new char[8192])
         ) {
            ExtraInfo info = new ExtraInfo();
            PluginManifest manifest = PluginManifest.CODEC.decodeJson(json, info);
            if (manifest == null || info.getValidationResults().hasFailed()) {
               return null;
            }

            return manifest;
         }
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static boolean isZip(Path path) {
      if (path == null || path.getFileName() == null) {
         return false;
      }

      return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
   }
}
