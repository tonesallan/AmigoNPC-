package br.tones.amigonpc.commands;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** Admin: +1 nível de espada. */
public final class AmigoLvlUpSubCommand extends AbstractPlayerCommand {

    public AmigoLvlUpSubCommand() {
        super("up", "Aumenta o nível de espada do Amigo (+1)" );
        this.setAllowsExtraArguments(false);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {
        if (!isAdmin(ctx)) {
            ctx.sendMessage(Message.raw("§cSem permissão (admin)."));
            return;
        }

        UUID ownerId = playerRef.getUuid();
        int newLvl = AmigoLvlCommand.manager().changeSwordLevel(ownerId, +1, true);
        ctx.sendMessage(Message.raw("§7[AmigoNPC] Nível de espada agora: §f" + newLvl));
    }

    private static boolean isAdmin(CommandContext ctx) {
        // mesma regra do /amigopvp
        try {
            var sender = ctx.sender();
            if (sender == null) return false;

            String opNode = HytalePermissions.fromCommand("op");
            return sender.hasPermission(opNode, false)
                    || sender.hasPermission("hytale.command.op", false)
                    || sender.hasPermission("hytale.*", false)
                    || sender.hasPermission("*", false);
        } catch (Throwable t) {
            return false;
        }
    }
}
