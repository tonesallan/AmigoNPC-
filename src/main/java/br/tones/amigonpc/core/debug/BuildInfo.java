package br.tones.amigonpc.core.debug;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;

public final class BuildInfo {
   private static final String MOD_VERSION = readManifestVersionBestEffort();
   private static final BuildInfo.BuildIdInfo BUILD_ID_INFO = computeBuildIdBestEffort();

   private BuildInfo() {
   }

   public static String getModVersion() {
      return MOD_VERSION;
   }

   public static String getBuildId() {
      return BUILD_ID_INFO.buildId;
   }

   public static String getBuildIdSource() {
      return BUILD_ID_INFO.source;
   }

   public static String getJarLocation() {
      return BUILD_ID_INFO.location;
   }

   public static String getJavaVersion() {
      try {
         return System.getProperty("java.version");
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static String readManifestVersionBestEffort() {
      try (InputStream in = BuildInfo.class.getResourceAsStream("/manifest.json")) {
         if (in == null) {
            return null;
         }

         String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
         int idx = s.indexOf("\"Version\"");
         if (idx < 0) {
            return null;
         }

         int colon = s.indexOf(58, idx);
         if (colon < 0) {
            return null;
         }

         int q1 = s.indexOf(34, colon + 1);
         if (q1 < 0) {
            return null;
         }

         int q2 = s.indexOf(34, q1 + 1);
         if (q2 < 0) {
            return null;
         }

         String v = s.substring(q1 + 1, q2).trim();
         return v.isEmpty() ? null : v;
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static BuildInfo.BuildIdInfo computeBuildIdBestEffort() {
      BuildInfo.BuildIdInfo out = new BuildInfo.BuildIdInfo();
      out.buildId = "unknown";
      out.source = "none";
      out.location = null;

      try {
         URL locUrl = BuildInfo.class.getProtectionDomain().getCodeSource().getLocation();
         if (locUrl != null) {
            String loc = locUrl.toString();
            out.location = loc;
            if (loc.toLowerCase(Locale.ROOT).endsWith(".jar") && loc.startsWith("file:")) {
               try {
                  Path jarPath = Path.of(locUrl.toURI());
                  if (Files.isRegularFile(jarPath)) {
                     String sha = sha256Hex(Files.readAllBytes(jarPath));
                     out.buildId = shortId(sha);
                     out.source = "jarSha256";
                     return out;
                  }
               } catch (Throwable var6) {
               }
            }
         }
      } catch (Throwable var7) {
      }

      try (InputStream in = BuildInfo.class.getResourceAsStream("/br/tones/amigonpc/AmigoNPCPlugin.class")) {
         if (in != null) {
            String sha = sha256Hex(in.readAllBytes());
            out.buildId = "dev-" + shortId(sha);
            out.source = "classSha256";
            return out;
         }
      } catch (Throwable var9) {
      }

      out.buildId = "unknown";
      out.source = "none";
      return out;
   }

   private static String sha256Hex(byte[] data) throws Exception {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(data);
      StringBuilder sb = new StringBuilder(dig.length * 2);

      for (byte b : dig) {
         sb.append(String.format("%02x", b));
      }

      return sb.toString();
   }

   private static String shortId(String shaHex) {
      if (shaHex == null) {
         return "unknown";
      }

      String s = shaHex.trim();
      return s.length() <= 12 ? s : s.substring(0, 12);
   }

   private static final class BuildIdInfo {
      String buildId;
      String source;
      String location;
   }
}
