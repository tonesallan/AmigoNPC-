package br.tones.amigonpc.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import br.tones.amigonpc.core.AmigoService;

public final class AmigoSpawnSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private final AmigoService service;

    public AmigoSpawnSubCommand(AmigoService service) {
        super("spawn", "Spawna o NPC do jogador");
        this.service = service;

        // spawn não precisa aceitar args
        this.setAllowsExtraArguments(false);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {
        // ✅ Nesta API, o comando player já entrega Store/World/PlayerRef prontos.
        // Usamos o Store diretamente para evitar spawn assíncrono e perder o registro do NPC.
        service.spawn(ctx, world, store, playerEntityRef, playerRef);
    }
}
