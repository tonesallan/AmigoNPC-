package br.tones.amigonpc.commands;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import br.tones.amigonpc.core.AmigoService;

public final class AmigoSpawnCommand extends AbstractCommand {

    // ✅ comando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private final AmigoService service;

    public AmigoSpawnCommand(AmigoService service) {
        super("amigospawn", "Spawna o NPC do AmigoNPC");
        this.service = service;
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        service.spawn(ctx);
        return CompletableFuture.completedFuture(null);
    }
}
