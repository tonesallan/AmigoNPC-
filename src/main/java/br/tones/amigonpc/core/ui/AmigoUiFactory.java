package br.tones.amigonpc.ui;

import java.lang.reflect.Method;

/**
 * Fábrica de UI do AmigoNPC (por reflexão).
 *
 * Estratégia mais estável:
 * - Botões executam comandos ("/amigo spawn" e "/amigo despawn")
 * - Evita depender de callbacks (que variam bastante entre builds)
 */
public final class AmigoUiFactory {

    private AmigoUiFactory() {}

    private static volatile String LAST_ERROR;

    public static String getLastError() {
        return LAST_ERROR;
    }

    private static void setError(String msg) {
        LAST_ERROR = msg;
    }

    /**
     * Constrói o objeto de UI principal.
     *
     * @return objeto final da UI (qualquer tipo), ou null se falhar
     */
    public static Object buildMainUi() {
        try {
            Class<?> builderClass = tryLoad(
                    "com.hypixel.hytale.server.core.ui.UICommandBuilder",
                    "com.hypixel.hytale.server.core.ui.command.UICommandBuilder",
                    "com.hypixel.hytale.server.core.ui.uicommands.UICommandBuilder"
            );

            if (builderClass == null) {
                setError("UICommandBuilder não encontrado nesta build.");
                return null;
            }

            Object builder = instantiate(builderClass);
            if (builder == null) {
                setError("Falha ao instanciar UICommandBuilder.");
                return null;
            }

            // Título / texto (tentativas comuns)
            tryInvoke(builder, "title", String.class, "AmigoNPC");
            tryInvoke(builder, "setTitle", String.class, "AmigoNPC");

            tryInvoke(builder, "text", String.class,
                    "Controle do seu Amigo NPC\n\n" +
                    "• Spawn cria o NPC\n" +
                    "• Despawn remove o NPC\n");
            tryInvoke(builder, "setText", String.class,
                    "Controle do seu Amigo NPC\n\n" +
                    "• Spawn cria o NPC\n" +
                    "• Despawn remove o NPC\n");

            // Botões que executam comandos (mais compatível)
            addCommandButton(builder, "Spawn", "/amigo spawn");
            addCommandButton(builder, "Despawn", "/amigo despawn");
            // /amigoui e /amigoui2 foram removidos. Mantemos um "Fechar" que apenas re-executa /amigo.
            addCommandButton(builder, "Fechar", "/amigo");

            // Finaliza
            Object uiObj = tryInvokeReturn(builder, "build");
            if (uiObj == null) uiObj = tryInvokeReturn(builder, "create");
            if (uiObj == null) uiObj = tryInvokeReturn(builder, "finish");
            if (uiObj == null) uiObj = tryInvokeReturn(builder, "getResult");
            if (uiObj == null) uiObj = tryInvokeReturn(builder, "getCommand");

            if (uiObj == null) {
                setError("Não consegui obter o objeto final da UI (build/create/finish/getResult/getCommand).");
                return null;
            }

            return uiObj;

        } catch (Throwable t) {
            setError("buildMainUi falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return null;
        }
    }

    // -------------------------
    // Helpers: adicionar botões
    // -------------------------

    private static void addCommandButton(Object builder, String label, String command) {
        // Tentativas de assinaturas comuns:
        // button(label, command)
        // addButton(label, command)
        // commandButton(label, command)
        // buttonCommand(label, command)
        // option(label, command)
        // addOption(label, command)

        if (tryInvoke(builder, "button", String.class, String.class, label, command)) return;
        if (tryInvoke(builder, "addButton", String.class, String.class, label, command)) return;
        if (tryInvoke(builder, "commandButton", String.class, String.class, label, command)) return;
        if (tryInvoke(builder, "buttonCommand", String.class, String.class, label, command)) return;
        if (tryInvoke(builder, "option", String.class, String.class, label, command)) return;
        if (tryInvoke(builder, "addOption", String.class, String.class, label, command)) return;

        // Se nenhum método existir, fica sem botão (não quebra)
        // Você ainda consegue abrir a UI e ver o texto.
    }

    // -------------------------
    // Reflection helpers
    // -------------------------

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
