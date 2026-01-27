package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;

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

/**
 * /amigo defende [on|off]
 * Sem argumento -> alterna ON/OFF.
 *
 * Quando ON:
 * - Prioriza atacar quem deu dano no player (últimos segundos).
 * - Se o player bater em alguém e não houver agressor ativo, auxilia atacando o alvo do player.
 * - Só causa dano quando ON.
 */
public final class AmigoDefendeSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private final OptionalArg valueArg;

    public AmigoDefendeSubCommand() {
        super("defende", "Alterna o modo de defesa/assistência do AmigoNPC (ON/OFF)");
        this.valueArg = this.withOptionalArg("valor", "on/off (sem argumento alterna)", ArgTypes.STRING);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {

        AmigoNpcManager manager = AmigoNpcManager.getShared();
        java.util.UUID owner = playerRef.getUuid();

        boolean current = manager.isDefendeEnabled(owner);
        boolean next = current;

        if (!valueArg.provided(ctx)) {
            next = !current;
        } else {
            Object raw = valueArg.get(ctx);
            String s = raw == null ? "" : raw.toString().trim().toLowerCase();
            if (s.isEmpty()) {
                next = !current;
            } else if (s.equals("on") || s.equals("true") || s.equals("1") || s.equals("sim")) {
                next = true;
            } else if (s.equals("off") || s.equals("false") || s.equals("0") || s.equals("nao") || s.equals("não")) {
                next = false;
            } else {
                ctx.sendMessage(Message.raw("§e[AmigoNPC] Use: §f/amigo defende §7(para alternar) §eou §f/amigo defende on|off"));
                return;
            }
        }

        manager.setDefendeEnabled(owner, next);

        if (next) {
            ctx.sendMessage(Message.raw("§a[AmigoNPC] Defesa: §fON§a. (O NPC vai atacar quem te atacar e ajudar no seu alvo)"));
        } else {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Defesa: §fOFF§c. (O NPC não causará dano)"));
        }
    }
}
