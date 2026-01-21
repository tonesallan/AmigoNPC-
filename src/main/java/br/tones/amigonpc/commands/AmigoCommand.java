package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

public final class AmigoCommand extends AbstractCommand {

    public AmigoCommand(AmigoService service) {
        super("amigo", "Comandos do AmigoNPC");

        // ✅ MUITO IMPORTANTE: permite "/amigo <algo>" sem o parser travar em Expected: 0
        this.setAllowsExtraArguments(true);

        // ✅ Subcomandos oficiais da API
        this.addSubCommand(new AmigoSpawnSubCommand(service));
        this.addSubCommand(new AmigoDespawnSubCommand(service));

        // Aliases opcionais (se quiser)
        // this.addAliases("anpc");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        // Executa quando o player digita só "/amigo"
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigo spawn §7| §f/amigo despawn"));
        return CompletableFuture.completedFuture(null);
    }
}
