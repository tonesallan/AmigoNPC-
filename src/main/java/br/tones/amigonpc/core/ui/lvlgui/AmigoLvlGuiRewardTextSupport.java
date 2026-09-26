package br.tones.amigonpc.core.ui.lvlgui;

import java.util.ArrayList;
import java.util.List;

final class AmigoLvlGuiRewardTextSupport {
   private AmigoLvlGuiRewardTextSupport() {
   }

   static String formatCommandDisplay(String command, String commandTitle) {
      if (commandTitle != null && !commandTitle.isEmpty()) {
         return commandTitle;
      } else {
         return command != null && !command.isEmpty() ? command : "";
      }
   }

   static String escapeUiString(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "");
   }

   static String escapeUiStringMultiline(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
   }

   static List<String> wrapTextToLines(String text) {
      List<String> out = new ArrayList<>();
      if (text != null && !text.isEmpty()) {
         String t = text.trim();
         if (t.isEmpty()) {
            return out;
         }

         String[] parts = t.split(",\\s*", -1);

         for (String part : parts) {
            if (part != null) {
               String s = part.trim();
               if (!s.isEmpty()) {
                  if (s.length() <= 24) {
                     out.add(s);
                  } else {
                     int idx = 0;

                     while (idx < s.length()) {
                        int end = Math.min(idx + 24, s.length());
                        if (end < s.length()) {
                           int lastSpace = s.lastIndexOf(32, end);
                           if (lastSpace > idx) {
                              end = lastSpace + 1;
                           }
                        }

                        out.add(s.substring(idx, end).trim());
                        idx = end;
                     }
                  }
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }
}
