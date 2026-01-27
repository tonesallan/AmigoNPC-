package br.tones.amigonpc.commands;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import br.tones.amigonpc.core.HytaleBridge;
import br.tones.amigonpc.ui.AmigoUiFactory;
import br.tones.amigonpc.ui.UiBridge;

public final class AmigoDebugCommand extends AbstractCommand {

    // ✅ comando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    public AmigoDebugCommand() {
        super("amigo", "Debug do AmigoNPC");
        // ⚠️ Não usamos este construtor para substituir /amigo.
        // Vamos registrar este comando como subcomando separado no próximo arquivo.
    }

    /**
     * Este arquivo é um comando "isolado", então usamos outro nome para não conflitar.
     * Para manter /amigo debug, vamos registrar este como "amigodebug".
     *
     * Se você realmente quiser /amigo debug, eu faço isso alterando o AmigoCommand
     * (mas você disse que não quer substituições).
     */
    public static AmigoDebugCommand createAsStandalone() {
        return new AmigoDebugCommand("amigodebug");
    }

    private AmigoDebugCommand(String name) {
        super(name, "Mostra informações de debug do AmigoNPC");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        ctx.sendMessage(Message.raw("§b[AmigoNPC] Debug"));

        String hb = HytaleBridge.getLastError();
        ctx.sendMessage(Message.raw("§7- HytaleBridge: " + (hb == null ? "OK/sem erro" : hb)));

        String ub = UiBridge.getLastError();
        ctx.sendMessage(Message.raw("§7- UiBridge: " + (ub == null ? "OK/sem erro" : ub)));

        String uf = AmigoUiFactory.getLastError();
        ctx.sendMessage(Message.raw("§7- AmigoUiFactory: " + (uf == null ? "OK/sem erro" : uf)));

        ctx.sendMessage(Message.raw("§7Dica: teste §f/amigo spawn§7, §f/amigo despawn§7, §f/amigo defender§7."));
        return CompletableFuture.completedFuture(null);
    }
}
