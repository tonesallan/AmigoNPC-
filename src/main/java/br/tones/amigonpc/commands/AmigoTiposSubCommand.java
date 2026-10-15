package br.tones.amigonpc.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AmigoTiposSubCommand extends AbstractPlayerCommand {
   private final OptionalArg filtroArg;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoTiposSubCommand() {
      super("tipos", "Lista os tipos (npcType) disponíveis");
      this.filtroArg = this.withOptionalArg("filtro", "Filtrar por texto (ex.: villager, citizen, human)", ArgTypes.STRING);
   }

   protected void execute(CommandContext ctx, Store store, Ref playerEntityRef, PlayerRef playerRef, World world) {
      String filtro = null;
      if (this.filtroArg.provided(ctx)) {
         Object v = this.filtroArg.get(ctx);
         if (v != null) {
            filtro = String.valueOf(v).trim();
         }
      }

      if (filtro != null && filtro.isEmpty()) {
         filtro = null;
      }

      List<String> tipos = fetchRoleTemplateNames(true);
      if (tipos.isEmpty()) {
         ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui listar tipos nesta build."));
      } else {
         if (filtro != null) {
            String f = filtro.toLowerCase();
            tipos.removeIf((s) -> s == null || !s.toLowerCase().contains(f));
         }

         if (tipos.isEmpty()) {
            String var15 = filtro != null ? " para filtro: §f" + filtro : "";
            ctx.sendMessage(Message.raw("§e[AmigoNPC] Nenhum tipo encontrado" + var15 + "§e."));
         } else {
            Collections.sort(tipos, String::compareToIgnoreCase);
            String var10001 = filtro != null ? " (filtro: §f" + filtro + "§a)" : "";
            ctx.sendMessage(Message.raw("§a[AmigoNPC] Tipos disponíveis" + var10001 + ": §f" + tipos.size()));
            ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigo tipo <nome>"));
            int maxPerLine = 6;
            int shown = 0;
            StringBuilder line = new StringBuilder("§f");

            for(String t : tipos) {
               if (t != null) {
                  if (shown > 0) {
                     line.append("§7, §f");
                  }

                  line.append(t);
                  ++shown;
                  if (shown >= maxPerLine) {
                     ctx.sendMessage(Message.raw(line.toString()));
                     line = new StringBuilder("§f");
                     shown = 0;
                  }
               }
            }

            if (shown > 0) {
               ctx.sendMessage(Message.raw(line.toString()));
            }

         }
      }
   }

   private static List fetchRoleTemplateNames(boolean spawnableOnly) {
      try {
         Class<?> npcPluginClass = Class.forName("com.hypixel.hytale.server.npc.NPCPlugin");
         Method get = npcPluginClass.getMethod("get");
         Object npcPlugin = get.invoke((Object)null);
         if (npcPlugin == null) {
            return List.of();
         }

         Method m = npcPlugin.getClass().getMethod("getRoleTemplateNames", Boolean.TYPE);
         Object out = m.invoke(npcPlugin, spawnableOnly);
         if (out instanceof List list) {
            ArrayList<String> r = new ArrayList();

            for(Object v : list) {
               if (v != null) {
                  r.add(String.valueOf(v));
               }
            }

            return r;
         }
      } catch (Throwable var10) {
      }

      return List.of();
   }
}
