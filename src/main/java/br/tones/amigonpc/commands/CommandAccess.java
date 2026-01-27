package br.tones.amigonpc.commands;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Best-effort para deixar comandos "públicos" (sem permissão) em builds diferentes do Hytale.
 *
 * Importante: não altera comandos admin-only (eles ainda checam admin no execute).
 *
 * A API de comandos/permissions muda entre builds; então aqui tentamos vários nomes via reflexão.
 * Se um método/campo não existir, ignoramos silenciosamente.
 */
public final class CommandAccess {

    private CommandAccess() {}

    /**
     * Tenta remover exigência de permissão do comando e (best-effort) de seus subcomandos.
     */
    public static void makePublicRecursive(Object cmd) {
        if (cmd == null) return;

        // 1) métodos comuns (boolean)
        tryInvoke(cmd, "setRequiresPermission", boolean.class, false);
        tryInvoke(cmd, "setPermissionRequired", boolean.class, false);
        tryInvoke(cmd, "setRequiresPermissions", boolean.class, false);
        tryInvoke(cmd, "setRequirePermission", boolean.class, false);
        tryInvoke(cmd, "setRequiresOp", boolean.class, false);
        tryInvoke(cmd, "setOpOnly", boolean.class, false);
        tryInvoke(cmd, "setAdminOnly", boolean.class, false);

        // 2) métodos comuns (String)
        tryInvoke(cmd, "setPermission", String.class, (String) null);
        tryInvoke(cmd, "setPermissionNode", String.class, (String) null);
        tryInvoke(cmd, "setPermission", String.class, "");
        tryInvoke(cmd, "setPermissionNode", String.class, "");

        // 3) tentativa de limpar campos internos
        tryClearPermissionFields(cmd);

        // 4) recursão em subcomandos (best-effort)
        for (Object sub : findSubCommands(cmd)) {
            makePublicRecursive(sub);
        }
    }

    private static void tryInvoke(Object target, String methodName, Class<?> paramType, Object arg) {
        // public
        try {
            Method m = target.getClass().getMethod(methodName, paramType);
            m.setAccessible(true);
            m.invoke(target, arg);
            return;
        } catch (Throwable ignored) {}

        // declared
        try {
            Method m = target.getClass().getDeclaredMethod(methodName, paramType);
            m.setAccessible(true);
            m.invoke(target, arg);
        } catch (Throwable ignored) {}
    }

    private static void tryClearPermissionFields(Object cmd) {
        try {
            Field[] fields = cmd.getClass().getDeclaredFields();
            for (Field f : fields) {
                String n = f.getName();
                if (n == null) continue;
                String ln = n.toLowerCase(Locale.ROOT);
                if (!(ln.contains("permission") || ln.contains("perm") || ln.contains("oponly") || ln.contains("admin"))) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Class<?> t = f.getType();
                    if (t == boolean.class) {
                        f.setBoolean(cmd, false);
                    } else if (t == Boolean.class) {
                        f.set(cmd, Boolean.FALSE);
                    } else if (t == String.class) {
                        f.set(cmd, null);
                    }
                } catch (Throwable ignored2) {}
            }
        } catch (Throwable ignored) {}
    }

    private static List<Object> findSubCommands(Object cmd) {
        if (cmd == null) return Collections.emptyList();

        // método getSubCommands()
        try {
            Method m = cmd.getClass().getMethod("getSubCommands");
            Object v = m.invoke(cmd);
            List<Object> out = toList(v);
            if (!out.isEmpty()) return out;
        } catch (Throwable ignored) {}

        // alguns builds usam children/getChildren
        for (String mn : new String[] { "getChildren", "children", "subCommands", "subcommands" }) {
            try {
                Method m = cmd.getClass().getMethod(mn);
                Object v = m.invoke(cmd);
                List<Object> out = toList(v);
                if (!out.isEmpty()) return out;
            } catch (Throwable ignored) {}
        }

        // campos comuns
        for (String fn : new String[] { "subCommands", "subcommands", "children", "subCommandList", "subCommandMap" }) {
            try {
                Field f = cmd.getClass().getDeclaredField(fn);
                f.setAccessible(true);
                Object v = f.get(cmd);
                List<Object> out = toList(v);
                if (!out.isEmpty()) return out;
            } catch (Throwable ignored) {}
        }

        return Collections.emptyList();
    }

    private static List<Object> toList(Object v) {
        if (v == null) return Collections.emptyList();

        List<Object> out = new ArrayList<>();
        try {
            if (v instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o != null) out.add(o);
                }
                return out;
            }
            if (v instanceof Map<?, ?> map) {
                for (Object o : map.values()) {
                    if (o != null) out.add(o);
                }
                return out;
            }
            if (v.getClass().isArray()) {
                int n = Array.getLength(v);
                for (int i = 0; i < n; i++) {
                    Object o = Array.get(v, i);
                    if (o != null) out.add(o);
                }
                return out;
            }
        } catch (Throwable ignored) {}

        return out;
    }
}
