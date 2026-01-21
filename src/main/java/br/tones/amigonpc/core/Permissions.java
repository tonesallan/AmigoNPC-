package br.tones.amigonpc.core;

import java.lang.reflect.Method;

/**
 * Permissões do AmigoNPC.
 *
 * Como API de permissão pode variar, usamos reflexão para checagem.
 */
public final class Permissions {

    private Permissions() {}

    // Nós das permissões (você pode mudar depois se quiser padrão diferente)
    public static final String AMIGO_USE = "amigonpc.use";
    public static final String AMIGO_SPAWN = "amigonpc.spawn";
    public static final String AMIGO_DESPAWN = "amigonpc.despawn";
    public static final String AMIGO_UI = "amigonpc.ui";
    public static final String AMIGO_DEBUG = "amigonpc.debug";

    private static volatile String LAST_ERROR;

    public static String getLastError() {
        return LAST_ERROR;
    }

    private static void setError(String msg) {
        LAST_ERROR = msg;
    }

    /**
     * Checa permissão no sender sem assumir API fixa.
     * Retorna:
     * - true se encontrar um método de permissão e ele permitir
     * - true se não achar NENHUM método (modo permissivo por padrão, para não travar dev)
     */
    public static boolean has(Object sender, String permissionNode) {
        if (sender == null || permissionNode == null || permissionNode.isBlank()) return true;

        // Métodos comuns: hasPermission(String), hasPerm(String), can(String)
        String[] candidates = { "hasPermission", "hasPerm", "can" };

        for (String name : candidates) {
            try {
                Method m = sender.getClass().getMethod(name, String.class);
                Object r = m.invoke(sender, permissionNode);
                if (r instanceof Boolean b) return b.booleanValue();
            } catch (Throwable ignored) {}
        }

        // Algumas APIs usam permissionManager()/getPermissions()/permissions()
        Object pm = tryNoArg(sender, "getPermissionManager", "permissionManager", "getPermissions", "permissions");
        if (pm != null) {
            for (String name : candidates) {
                try {
                    Method m = pm.getClass().getMethod(name, String.class);
                    Object r = m.invoke(pm, permissionNode);
                    if (r instanceof Boolean b) return b.booleanValue();
                } catch (Throwable ignored) {}
            }
        }

        // Não achou método algum: modo permissivo para não quebrar desenvolvimento
        setError("Nenhum método de permissão encontrado no sender nesta build (modo permissivo ativo).");
        return true;
    }

    private static Object tryNoArg(Object target, String... names) {
        for (String n : names) {
            try {
                Method m = target.getClass().getMethod(n);
                return m.invoke(target);
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
