package br.tones.amigonpc;

import br.tones.amigonpc.commands.AmigoCommand;
import br.tones.amigonpc.commands.AmigoDebugCommand;
import br.tones.amigonpc.commands.AmigoUi2Command;
import br.tones.amigonpc.commands.AmigoUiCommand;
import br.tones.amigonpc.core.AmigoService;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public final class AmigoNPCPlugin extends JavaPlugin {

    private AmigoService service;

    public AmigoNPCPlugin(JavaPluginInit init) {
        super(init);
    }

    public void onEnable() {
        this.service = new AmigoService();

        // Comando principal: /amigo
        CommandManager.get().registerSystemCommand(new AmigoCommand(service));

        // UI v1: /amigoui
        CommandManager.get().registerSystemCommand(new AmigoUiCommand());

        // UI v2: /amigoui2
        CommandManager.get().registerSystemCommand(new AmigoUi2Command());

        // Debug: /amigodebug
        CommandManager.get().registerSystemCommand(
                AmigoDebugCommand.createAsStandalone()
        );

    }
}
