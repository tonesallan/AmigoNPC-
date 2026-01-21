package br.tones.amigonpc;

import javax.annotation.Nonnull;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import br.tones.amigonpc.commands.AmigoCommand;
import br.tones.amigonpc.commands.AmigoDebugCommand;
import br.tones.amigonpc.commands.AmigoUi2Command;
import br.tones.amigonpc.commands.AmigoUiCommand;
import br.tones.amigonpc.core.AmigoService;

public final class AmigoNPCPlugin extends JavaPlugin {

    /**
     * Manifest oficial (API).
     * O loader usa isso para reconhecer o plugin.
     */
    public static final PluginManifest MANIFEST = PluginManifest
            .corePlugin(AmigoNPCPlugin.class)
            .build();

    private AmigoService service;

    public AmigoNPCPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.service = new AmigoService();

        // ✅ Registra apenas o comando raiz "/amigo"
        // Os subcomandos "/amigo spawn" e "/amigo despawn" devem estar dentro do AmigoCommand via addSubCommand(...)
        this.getCommandRegistry().registerCommand(new AmigoCommand(service));

        // UI
        this.getCommandRegistry().registerCommand(new AmigoUiCommand());
        this.getCommandRegistry().registerCommand(new AmigoUi2Command());

        // Debug
        this.getCommandRegistry().registerCommand(AmigoDebugCommand.createAsStandalone());

        // Logger: sua API não tem getLogger().info(...)
        // Se quiser log, me mostre os métodos do HytaleLogger que eu coloco o correto.
    }
}
