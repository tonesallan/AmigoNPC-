package br.tones.amigonpc.core.bootstrap;

import br.tones.amigonpc.commands.AmigoCommand;
import br.tones.amigonpc.commands.AmigoDebugCommand;
import br.tones.amigonpc.commands.AmigoLvlCommand;
import br.tones.amigonpc.commands.AmigoPvpCommand;
import br.tones.amigonpc.commands.AutoLootCommand;
import br.tones.amigonpc.commands.CommandAccess;
import br.tones.amigonpc.commands.LootCommand;
import br.tones.amigonpc.core.AmigoService;
import java.lang.reflect.Method;

public final class AmigoCommandRegistrar {
   private final Object commandRegistry;
   private final AmigoService service;

   public AmigoCommandRegistrar(Object commandRegistry, AmigoService service) {
      this.commandRegistry = commandRegistry;
      this.service = service;
   }

   public void registerAll() {
      AmigoCommand amigoCmd = new AmigoCommand(this.service);
      CommandAccess.makePublicRecursive(amigoCmd);
      this.registerCommand(amigoCmd);
      LootCommand lootCmd = new LootCommand();
      CommandAccess.makePublicRecursive(lootCmd);
      this.registerCommand(lootCmd);
      AutoLootCommand autolootCmd = new AutoLootCommand();
      CommandAccess.makePublicRecursive(autolootCmd);
      this.registerCommand(autolootCmd);
      AmigoDebugCommand dbgCmd = AmigoDebugCommand.createAsStandalone();
      CommandAccess.makePublicRecursive(dbgCmd);
      this.registerCommand(dbgCmd);
      this.registerCommand(new AmigoPvpCommand());
      this.registerCommand(new AmigoLvlCommand());
   }

   private void registerCommand(Object command) {
      if (this.commandRegistry != null && command != null) {
         try {
            for (Method m : this.commandRegistry.getClass().getMethods()) {
               if (m.getName().equals("registerCommand") && m.getParameterCount() == 1) {
                  Class<?> p = m.getParameterTypes()[0];
                  if (p.isAssignableFrom(command.getClass())) {
                     m.invoke(this.commandRegistry, command);
                     return;
                  }
               }
            }
         } catch (Throwable var7) {
         }
      }
   }
}
