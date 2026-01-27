package br.tones.amigonpc.commands;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import br.tones.amigonpc.ui.AmigoUiFactory;

public final class AmigoUi2Command extends AbstractCommand {

    // ✅ comando público (sem permissão)
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    public AmigoUi2Command() {
        super("amigoui2", "Abre a interface do AmigoNPC (v2)");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Este comando só pode ser usado por jogadores."));
            return CompletableFuture.completedFuture(null);
        }

        Object player = ctx.sender();

        Object uiObj = AmigoUiFactory.buildMainUi();
        if (uiObj == null) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Falha ao montar UI v2 nesta build."));
            String err = AmigoUiFactory.getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return CompletableFuture.completedFuture(null);
        }

        // tenta abrir a UI no player (sem depender de método fixo)
        if (!openUiOnPlayer(player, uiObj)) {
            ctx.sendMessage(Message.raw("§c[AmigoNPC] Não consegui abrir UI v2 no player."));
            String err = getLastError();
            if (err != null) ctx.sendMessage(Message.raw("§7[AmigoNPC] Debug: " + err));
            return CompletableFuture.completedFuture(null);
        }

        ctx.sendMessage(Message.raw("§a[AmigoNPC] UI v2 aberta."));
        return CompletableFuture.completedFuture(null);
    }

    // -----------------------
    // Bridge interno de openUi
    // -----------------------

    private static volatile String LAST_ERROR;

    private static String getLastError() {
        return LAST_ERROR;
    }

    private static void setError(String msg) {
        LAST_ERROR = msg;
    }

    private static boolean openUiOnPlayer(Object playerSender, Object uiObj) {
        try {
            // tentativas comuns
            if (tryInvoke(playerSender, "openUi", uiObj)) return true;
            if (tryInvoke(playerSender, "openUI", uiObj)) return true;
            if (tryInvoke(playerSender, "showUi", uiObj)) return true;
            if (tryInvoke(playerSender, "showUI", uiObj)) return true;
            if (tryInvoke(playerSender, "sendUi", uiObj)) return true;
            if (tryInvoke(playerSender, "sendUI", uiObj)) return true;

            Object uiManager = tryInvokeReturn(playerSender, "getUiManager");
            if (uiManager != null) {
                if (tryInvoke(uiManager, "open", uiObj)) return true;
                if (tryInvoke(uiManager, "show", uiObj)) return true;
                if (tryInvoke(uiManager, "send", uiObj)) return true;
            }

            setError("Nenhum método openUi/showUi/sendUi/getUiManager compatível.");
            return false;

        } catch (Throwable t) {
            setError("openUiOnPlayer falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }

    private static boolean tryInvoke(Object target, String methodName, Object arg) {
        try {
            var m = target.getClass().getMethod(methodName, arg.getClass());
            m.invoke(target, arg);
            return true;
        } catch (Throwable ignored) {}

        // fallback: procura qualquer método com 1 parâmetro e nome igual
        try {
            for (var m : target.getClass().getMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (m.getParameterCount() != 1) continue;
                m.invoke(target, arg);
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static Object tryInvokeReturn(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {}
        return null;
    }
}
