package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoLogSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    public AmigoLogSubCommand() {
        super("log", "Alterna o log de debug do AmigoNPC no chat");
        this.setAllowsExtraArguments(false);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {

        final var ownerId = playerRef.getUuid();
        boolean enabled = AmigoNpcManager.getShared().toggleDebugLog(ownerId);

        ctx.sendMessage(Message.raw("§7[AmigoNPC] Log agora: " + (enabled ? "§aON" : "§cOFF")));
    }
}
