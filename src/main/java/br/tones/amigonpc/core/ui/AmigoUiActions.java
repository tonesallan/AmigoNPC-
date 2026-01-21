package br.tones.amigonpc.ui;

import java.util.UUID;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.HytaleBridge;

/**
 * Ações acionadas pela UI (botões / callbacks).
 *
 * Tudo isolado aqui para:
 * - manter a UI simples
 * - permitir evoluir sem mexer em comandos/core
 */
public final class AmigoUiActions {

    private static final AmigoNpcManager MANAGER = AmigoNpcManager.getShared();
    private static volatile String LAST_ERROR;

    private AmigoUiActions() {}

    public static String getLastError() {
        return LAST_ERROR;
    }

    private static void setError(String msg) {
        LAST_ERROR = msg;
    }

    /**
     * Executa uma action vinda da UI.
     *
     * @param commandContext CommandContext (ou algo equivalente) quando disponível; pode ser null dependendo da UI API
     * @param playerSender   sender/player que abriu a UI (obrigatório)
     * @param actionId       "spawn" | "despawn" | "close" | etc.
     */
    public static boolean handle(Object commandContext, Object playerSender, String actionId) {
        if (playerSender == null) {
            setError("playerSender é null.");
            return false;
        }
        if (actionId == null || actionId.isBlank()) {
            setError("actionId vazio.");
            return false;
        }

        try {
            // pega UUID do player via reflexão (método conhecido nas builds: getUuid())
            UUID ownerId = tryGetUuid(playerSender);
            if (ownerId == null) {
                setError("Não consegui obter UUID do playerSender.");
                return false;
            }

            Object world = null;
            if (commandContext != null) {
                world = HytaleBridge.tryGetWorldFromCommandContext(commandContext);
            }
            if (world == null) {
                // fallback: tenta pegar world direto do sender
                world = tryGetWorldFromSender(playerSender);
            }
            if (world == null) {
                setError("Não consegui obter World para executar a ação.");
                return false;
            }

            switch (actionId.toLowerCase()) {
                case "spawn" -> {
                    boolean ok = MANAGER.spawn(world, ownerId);
                    if (!ok) {
                        setError("Spawn falhou: " + MANAGER.getLastError());
                        return false;
                    }
                    return true;
                }
                case "despawn" -> {
                    boolean ok = MANAGER.despawn(world, ownerId);
                    if (!ok) {
                        setError("Despawn falhou: " + MANAGER.getLastError());
                        return false;
                    }
                    return true;
                }
                case "close" -> {
                    // fechar geralmente é tratado pelo próprio UI system;
                    // aqui só retornamos true.
                    return true;
                }
                default -> {
                    setError("ActionId desconhecido: " + actionId);
                    return false;
                }
            }

        } catch (Throwable t) {
            setError("handle falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }

    // --------------------
    // Reflection helpers
    // --------------------

    private static UUID tryGetUuid(Object sender) {
        try {
            var m = sender.getClass().getMethod("getUuid");
            Object r = m.invoke(sender);
            if (r instanceof UUID u) return u;
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object tryGetWorldFromSender(Object sender) {
        String[] names = { "getWorld", "world", "getCurrentWorld", "getPlayerWorld" };
        for (String n : names) {
            try {
                var m = sender.getClass().getMethod(n);
                Object r = m.invoke(sender);
                if (r != null) return r;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
