package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

public final class AmigoCommand extends AbstractCommand {

    // ✅ Comandos públicos (sem permissão)
    // Nesta build, AbstractCommand gera um permission node automaticamente ao registrar o comando.
    // Se desativarmos isso, getPermission() fica null e o comando vira "público".
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    public AmigoCommand(AmigoService service) {
        super("amigo", "Comandos do AmigoNPC");

        // ✅ MUITO IMPORTANTE: permite "/amigo <algo>" sem o parser travar em Expected: 0
        this.setAllowsExtraArguments(true);

        // ✅ Subcomandos oficiais da API
        this.addSubCommand(new AmigoSpawnSubCommand(service));
        this.addSubCommand(new AmigoDespawnSubCommand(service));

        // Aparência (opcional)
        this.addSubCommand(new AmigoModeloSubCommand());
        this.addSubCommand(new AmigoModeloOffSubCommand());

        // Combate: modo defender
        this.addSubCommand(new AmigoDefenderSubCommand());

        // Debug: liga/desliga logs no chat
        this.addSubCommand(new AmigoLogSubCommand());


        // Aliases opcionais (se quiser)
        // this.addAliases("anpc");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        // Executa quando o player digita só "/amigo"
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigo spawn §7| §f/amigo despawn"));
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Aparência: §f/amigo modelo <id> [scale] §7| §f/amigo modelooff"));
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Combate: §f/amigo defender §7(toggles ON/OFF, padrão OFF)"));
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: §f/amigo log §7(toggles ON/OFF)"));
        return CompletableFuture.completedFuture(null);
    }
}
