package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.AmigoPersistence;

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
 * /amigo defender
 * /amigo defender on|off
 *
 * - Por padrão é OFF (se não houver arquivo salvo).
 * - Sem argumento: alterna (toggle) ON/OFF.
 * - Com argumento: força ON ou OFF.
 */
public final class AmigoDefenderSubCommand extends AbstractPlayerCommand {

    // ✅ subcomando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private final OptionalArg stateArg;

    public AmigoDefenderSubCommand() {
        super("defender", "Alterna o modo Defender do AmigoNPC");
        this.addAliases("defende");

        this.stateArg = this.withOptionalArg(
                "state",
                "on|off (ou true|false)",
                ArgTypes.STRING
        );

        this.setAllowsExtraArguments(false);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {

        final var ownerId = playerRef.getUuid();

        boolean current = AmigoPersistence.loadDefenderEnabled(ownerId);
        boolean enabled;

        // Sem argumento -> toggle
        if (!stateArg.provided(ctx)) {
            enabled = !current;
        } else {
            String raw = String.valueOf(stateArg.get(ctx));
            if (isOn(raw)) {
                enabled = true;
            } else if (isOff(raw)) {
                enabled = false;
            } else {
                ctx.sendMessage(Message.raw("§c[AmigoNPC] Valor inválido. Use on/off."));
                return;
            }
        }

        // Persistência + aplica em memória
        AmigoPersistence.saveDefenderEnabled(ownerId, enabled);
        AmigoNpcManager.getShared().setDefendeEnabled(ownerId, enabled);

        ctx.sendMessage(Message.raw("§7[AmigoNPC] Defender agora: " + (enabled ? "§aON" : "§cOFF")));
    }

    private static boolean isOn(String s) {
        if (s == null) return false;
        String v = s.trim().toLowerCase();
        return v.equals("on") || v.equals("true") || v.equals("1") || v.equals("sim") || v.equals("yes");
    }

    private static boolean isOff(String s) {
        if (s == null) return false;
        String v = s.trim().toLowerCase();
        return v.equals("off") || v.equals("false") || v.equals("0") || v.equals("nao") || v.equals("não") || v.equals("no");
    }
}
