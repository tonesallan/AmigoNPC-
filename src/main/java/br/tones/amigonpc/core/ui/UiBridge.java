package br.tones.amigonpc.ui;

import java.lang.reflect.Method;

/**
 * Bridge de UI (Interface) por reflexão.
 *
 * Objetivo:
 * - Evitar quebrar por variação de API de UI entre builds.
 * - Concentrar toda instabilidade de UI em 1 arquivo.
 *
 * Uso esperado (no próximo passo):
 * - obter player via ctx.sender()
 * - UiBridge.openAmigoMainUi(player)
 */
public final class UiBridge {

    private UiBridge() {}

    private static volatile String LAST_ERROR;

    public static String getLastError() {
        return LAST_ERROR;
    }

    private static void setError(String msg) {
        LAST_ERROR = msg;
    }

    /**
     * Tenta abrir a UI principal do AmigoNPC para o player.
     * Não assume classes/métodos fixos: tudo via reflexão.
     */
    public static boolean openAmigoMainUi(Object playerSender) {
        if (playerSender == null) {
            setError("playerSender é null.");
            return false;
        }

        try {
            // 1) Tenta localizar UICommandBuilder (ou equivalente)
            // (nome conhecido pelos seus docs: UICommandBuilder)
            Class<?> builderClass = tryLoad(
                    "com.hypixel.hytale.server.core.ui.UICommandBuilder",
                    "com.hypixel.hytale.server.core.ui.command.UICommandBuilder",
                    "com.hypixel.hytale.server.core.ui.uicommands.UICommandBuilder"
            );

            if (builderClass == null) {
                setError("UICommandBuilder não encontrado nesta build.");
                return false;
            }

            Object builder = instantiate(builderClass);
            if (builder == null) {
                setError("Falha ao instanciar UICommandBuilder.");
                return false;
            }

            // 2) Monta uma UI mínima:
            // tenta métodos comuns: title/text/button/close/build etc.
            // (se não existirem, só ignora e segue)
            tryInvoke(builder, "title", String.class, "AmigoNPC");
            tryInvoke(builder, "setTitle", String.class, "AmigoNPC");
            tryInvoke(builder, "text", String.class, "UI base carregada.\nPróximo: botões e status do NPC.");
            tryInvoke(builder, "setText", String.class, "UI base carregada.\nPróximo: botões e status do NPC.");

            // tenta adicionar um botão "Fechar"
            // alguns builders usam: button(label, actionId) / addButton / option / etc.
            tryInvoke(builder, "button", String.class, String.class, "Fechar", "close");
            tryInvoke(builder, "addButton", String.class, String.class, "Fechar", "close");
            tryInvoke(builder, "option", String.class, String.class, "Fechar", "close");

            // 3) "build" gera o comando/página
            Object uiObj = null;
            uiObj = tryInvokeReturn(builder, "build");
            if (uiObj == null) uiObj = tryInvokeReturn(builder, "create");
            if (uiObj == null) uiObj = tryInvokeReturn(builder, "finish");

            if (uiObj == null) {
                // Alguns builders retornam void e guardam internamente;
                // nesse caso tentamos pegar "getResult"/"getCommand"
                uiObj = tryInvokeReturn(builder, "getResult");
                if (uiObj == null) uiObj = tryInvokeReturn(builder, "getCommand");
            }

            if (uiObj == null) {
                setError("Não consegui obter o objeto final da UI (build/create/finish/getResult).");
                return false;
            }

            // 4) Mostrar a UI para o player
            // tentativas comuns: player.openUi(x), player.showUi(x), player.sendUi(x)...
            if (tryInvoke(playerSender, "openUi", Object.class, uiObj)) return true;
            if (tryInvoke(playerSender, "openUI", Object.class, uiObj)) return true;
            if (tryInvoke(playerSender, "showUi", Object.class, uiObj)) return true;
            if (tryInvoke(playerSender, "showUI", Object.class, uiObj)) return true;
            if (tryInvoke(playerSender, "sendUi", Object.class, uiObj)) return true;
            if (tryInvoke(playerSender, "sendUI", Object.class, uiObj)) return true;

            // alguns sistemas usam "ui()" / "getUiManager()" no player
            Object uiManager = tryInvokeReturn(playerSender, "getUiManager");
            if (uiManager != null) {
                if (tryInvoke(uiManager, "open", Object.class, uiObj)) return true;
                if (tryInvoke(uiManager, "show", Object.class, uiObj)) return true;
                if (tryInvoke(uiManager, "send", Object.class, uiObj)) return true;
            }

            setError("Não achei método para abrir UI no player (openUi/showUi/sendUi/getUiManager...).");
            return false;

        } catch (Throwable t) {
            setError("openAmigoMainUi falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }

    // ==========================
    // Helpers de reflexão
    // ==========================

    private static Class<?> tryLoad(String... names) {
        for (String n : names) {
            try {
                return Class.forName(n);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object instantiate(Class<?> c) {
        try {
            var ctor = c.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean tryInvoke(Object target, String methodName, Class<?> p1, Object a1) {
        try {
            Method m = target.getClass().getMethod(methodName, p1);
            m.invoke(target, a1);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean tryInvoke(Object target, String methodName, Class<?> p1, Class<?> p2, Object a1, Object a2) {
        try {
            Method m = target.getClass().getMethod(methodName, p1, p2);
            m.invoke(target, a1, a2);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static Object tryInvokeReturn(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {}
        return null;
    }
}
