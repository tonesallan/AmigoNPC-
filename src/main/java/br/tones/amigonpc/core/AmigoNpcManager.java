package br.tones.amigonpc.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 1 NPC por player.
 *
 * Spawn VISÍVEL: usa NPCPlugin.spawnNPC(Store,...), que já cria a entidade com setup/modelo correto.
 * Despawn: remove pelo Store.removeEntity(ref, RemoveReason.*).
 *
 * Tudo por reflexão pra aguentar variação de build.
 */
public final class AmigoNpcManager {

    private static final AmigoNpcManager SHARED = new AmigoNpcManager();
    public static AmigoNpcManager getShared() { return SHARED; }

    private final Map<UUID, Object> npcRefPorPlayer = new ConcurrentHashMap<>();
    private static volatile String LAST_ERROR;

    public String getLastError() { return LAST_ERROR; }
    private static void setError(String msg) { LAST_ERROR = msg; }

    public boolean hasNpc(UUID ownerId) {
        return ownerId != null && npcRefPorPlayer.containsKey(ownerId);
    }

    /** Compat com chamadas antigas (ex.: UI/Service). */
    public boolean spawn(Object worldObj, UUID ownerId) {
        return spawn(worldObj, ownerId, null);
    }

    /**
     * Spawn real via NPCPlugin.
     * @param senderObj opcional (player/sender) pra ajudar a pegar posição; pode ser null.
     */
    public boolean spawn(Object worldObj, UUID ownerId, Object senderObj) {
        if (worldObj == null || ownerId == null) {
            setError("worldObj ou ownerId null.");
            return false;
        }

        if (hasNpc(ownerId)) {
            despawn(worldObj, ownerId);
        }

        final Object[] outRef = new Object[1];
        final AtomicBoolean ok = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        boolean queued = HytaleBridge.worldExecute(worldObj, () -> {
            try {
                // 1) Store = world.getEntityStore().getStore()
                Object componentStore = getComponentStoreFromWorld(worldObj);
                if (componentStore == null) {
                    setError("Não consegui obter Store via world.getEntityStore().getStore().");
                    return;
                }

                // 2) NPCPlugin.get()
                Object npcPlugin = invokeStaticNoArg("com.hypixel.hytale.server.npc.NPCPlugin", "get");
                if (npcPlugin == null) {
                    setError("NPCPlugin.get() não disponível nesta build.");
                    return;
                }

                // 3) npcType (preset ou role spawnável)
                String npcType = firstStringFromArray(invokeNoArg(npcPlugin, "getPresetCoverageTestNPCs"));
                if (npcType == null || npcType.isBlank()) {
                    Object list = invokeOneArg(npcPlugin, "getRoleTemplateNames", boolean.class, true);
                    npcType = firstStringFromList(list);
                }
                if (npcType == null || npcType.isBlank()) {
                    setError("Não encontrei npcType válido (sem presets e sem roles spawnáveis).");
                    return;
                }

                // 4) posição: tenta pegar do próprio player pelo Store+TransformComponent
                Object pos = tryGetOwnerPositionFromWorldStore(worldObj, componentStore, ownerId);
                if (pos == null) {
                    // fallback: tenta pelo sender (se veio)
                    pos = tryGetSenderPosition(senderObj);
                }

                // garante Vector3d (se vier outra coisa, tentamos converter)
                pos = coerceToVector3d(pos);
                if (pos == null) {
                    setError("Não consegui obter posição Vector3d do player.");
                    return;
                }

                // spawn pertinho
                pos = offsetVector3d(pos, 1.5, 0.0, 1.5);

                // 5) rotação (usa NPCPlugin.NULL_ROTATION se existir)
                Object rot = getStaticFieldIfExists("com.hypixel.hytale.server.npc.NPCPlugin", "NULL_ROTATION");
                if (rot == null) rot = newVector3f(0f, 0f, 0f);

                // 6) spawnNPC(Store store, String npcType, String groupType, Vector3d pos, Vector3f rot)
                Object pair = invokeSpawnNPC(npcPlugin, componentStore, npcType, null, pos, rot);
                if (pair == null) {
                    setError("spawnNPC retornou null (assinatura incompatível ou tipos errados). npcType=" + npcType);
                    return;
                }

                // Pair normalmente tem a Ref no "left/first/key"
                Object ref = extractRefFromPair(pair);
                outRef[0] = (ref != null ? ref : pair);

                ok.set(true);

            } catch (Throwable t) {
                setError("spawn() falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            } finally {
                latch.countDown();
            }
        });

        if (!queued) {
            setError("world.execute falhou: " + HytaleBridge.getLastError());
            return false;
        }

        // espera a task rodar (sem travar o servidor por muito tempo)
        try {
            boolean finished = latch.await(1200, TimeUnit.MILLISECONDS);
            if (!finished) {
                setError("Spawn enfileirado, mas não confirmou execução (timeout).");
                return false;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            setError("Spawn interrompido.");
            return false;
        }

        if (!ok.get()) {
            // LAST_ERROR já foi setado lá dentro
            return false;
        }

        npcRefPorPlayer.put(ownerId, outRef[0] != null ? outRef[0] : new FallbackMarker(ownerId));
        return true;
    }

    public boolean despawn(Object worldObj, UUID ownerId) {
        if (worldObj == null || ownerId == null) {
            setError("worldObj ou ownerId null.");
            return false;
        }

        Object saved = npcRefPorPlayer.remove(ownerId);
        if (saved == null) {
            setError("Nenhum NPC registrado para este player.");
            return false;
        }

        final AtomicBoolean ok = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        boolean queued = HytaleBridge.worldExecute(worldObj, () -> {
            try {
                Object componentStore = getComponentStoreFromWorld(worldObj);
                if (componentStore == null) {
                    setError("Não consegui obter Store via world.getEntityStore().getStore().");
                    return;
                }

                Object ref = saved;
                if (!(ref instanceof FallbackMarker)) {
                    if (!looksLikeRef(ref)) {
                        Object extracted = extractRefFromPair(ref);
                        if (extracted != null) ref = extracted;
                    }
                } else {
                    setError("Sem ref real para despawn (fallback marker).");
                    return;
                }

                // Store.removeEntity(ref, RemoveReason.*)
                Object removeReason = getRemoveReasonBestEffort();
                if (removeReason == null) {
                    setError("Não encontrei RemoveReason nesta build.");
                    return;
                }

                if (!tryInvokeRemoveEntity(componentStore, ref, removeReason)) {
                    setError("Não achei Store.removeEntity(ref, reason) compatível nesta build.");
                    return;
                }

                ok.set(true);

            } catch (Throwable t) {
                setError("despawn() falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            } finally {
                latch.countDown();
            }
        });

        if (!queued) {
            setError("world.execute falhou: " + HytaleBridge.getLastError());
            return false;
        }

        try {
            boolean finished = latch.await(1200, TimeUnit.MILLISECONDS);
            if (!finished) {
                setError("Despawn enfileirado, mas não confirmou execução (timeout).");
                return false;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            setError("Despawn interrompido.");
            return false;
        }

        return ok.get();
    }

    // =========================================================
    // Store / World helpers
    // =========================================================

    private static Object getComponentStoreFromWorld(Object worldObj) {
        Object entityStore = invokeNoArg(worldObj, "getEntityStore", "entityStore");
        if (entityStore == null) return null;
        Object store = invokeNoArg(entityStore, "getStore", "store");
        return store;
    }

    /**
     * Pega a posição do dono via:
     * world.getEntityRef(UUID) -> Store.getComponent(ref, TransformComponent.getComponentType()).getPosition()
     */
    private static Object tryGetOwnerPositionFromWorldStore(Object worldObj, Object componentStore, UUID ownerId) {
        try {
            Object ref = invokeOneArg(worldObj, "getEntityRef", UUID.class, ownerId);
            if (ref == null) return null;

            // TransformComponent (pelo docs que você enviou)
            Class<?> tcClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
            Method getCt = tcClass.getMethod("getComponentType");
            Object componentType = getCt.invoke(null);
            if (componentType == null) return null;

            // Store.getComponent(Ref, ComponentType)
            Object tc = invokeStoreGetComponent(componentStore, ref, componentType);
            if (tc == null) return null;

            return invokeNoArg(tc, "getPosition", "position");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeStoreGetComponent(Object store, Object ref, Object componentType) {
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("getComponent")) continue;
                if (m.getParameterCount() != 2) continue;
                return m.invoke(store, ref, componentType);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // =========================================================
    // spawnNPC + Pair/ref extraction
    // =========================================================

    private static Object invokeSpawnNPC(Object npcPlugin, Object store, String npcType, String groupType, Object pos, Object rot) {
        try {
            for (Method m : npcPlugin.getClass().getMethods()) {
                if (!m.getName().equals("spawnNPC")) continue;
                if (m.getParameterCount() != 5) continue;
                try {
                    return m.invoke(npcPlugin, store, npcType, groupType, pos, rot);
                } catch (IllegalArgumentException ignoredTryOtherOverload) {
                    // continua tentando caso tenha overloads/bridge
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean looksLikeRef(Object o) {
        if (o == null) return false;
        String n = o.getClass().getName();
        return n.endsWith(".Ref") || n.endsWith("Ref") || n.contains(".Ref");
    }

    private static Object extractRefFromPair(Object pair) {
        if (pair == null) return null;
        String[] methods = { "getLeft", "getFirst", "getKey", "left", "first", "key" };
        for (String mname : methods) {
            try {
                Method m = pair.getClass().getMethod(mname);
                Object v = m.invoke(pair);
                if (v != null) return v;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // =========================================================
    // RemoveEntity helpers (Store.removeEntity(ref, reason))
    // =========================================================

    private static Object getRemoveReasonBestEffort() {
        // a API docs cita RemoveReason, mas não trouxe o arquivo MD.
        // então tentamos nomes comuns, e se não achar, pegamos o primeiro enum value.
        String[] enumCandidates = {
                "com.hypixel.hytale.component.RemoveReason",
                "com.hypixel.hytale.component.Store$RemoveReason"
        };
        for (String cn : enumCandidates) {
            Object r =
                    getEnumConstantAny(cn, "DESPAWN", "COMMAND", "PLUGIN", "CUSTOM", "REMOVE");
            if (r != null) return r;

            Object first = getFirstEnumValue(cn);
            if (first != null) return first;
        }
        return null;
    }

    private static Object getEnumConstantAny(String enumClassName, String... names) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (!enumClass.isEnum()) return null;
            @SuppressWarnings("unchecked")
            Class<? extends Enum> e = (Class<? extends Enum>) enumClass;

            for (String n : names) {
                try {
                    return Enum.valueOf(e, n);
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getFirstEnumValue(String enumClassName) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (!enumClass.isEnum()) return null;
            Object[] vals = enumClass.getEnumConstants();
            return (vals != null && vals.length > 0) ? vals[0] : null;
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean tryInvokeRemoveEntity(Object store, Object ref, Object reason) {
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("removeEntity")) continue;
                if (m.getParameterCount() != 2) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p[1].isInstance(reason)) {
                    m.invoke(store, ref, reason);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // =========================================================
    // Position helpers
    // =========================================================

    private static Object tryGetSenderPosition(Object senderObj) {
        if (senderObj == null) return null;

        Object p = invokeNoArg(senderObj, "getPosition", "position");
        if (p != null) return p;

        Object transform = invokeNoArg(senderObj, "getTransform", "transform");
        if (transform != null) {
            Object p2 = invokeNoArg(transform, "getPosition", "position");
            if (p2 != null) return p2;
        }

        return null;
    }

    /** tenta transformar qualquer coisa em Vector3d (se já for, retorna; se for Transform, pega getPosition; etc). */
    private static Object coerceToVector3d(Object maybe) {
        if (maybe == null) return null;

        // já tem getX/getY/getZ? então tratamos como “vetor”
        if (hasXYZGetters(maybe)) return maybe;

        // se for um Transform/Component com getPosition()
        Object pos = invokeNoArg(maybe, "getPosition", "position");
        if (pos != null && hasXYZGetters(pos)) return pos;

        return null;
    }

    private static boolean hasXYZGetters(Object v) {
        try {
            v.getClass().getMethod("getX");
            v.getClass().getMethod("getY");
            v.getClass().getMethod("getZ");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object offsetVector3d(Object vec, double ox, double oy, double oz) {
        try {
            double x = (double) vec.getClass().getMethod("getX").invoke(vec);
            double y = (double) vec.getClass().getMethod("getY").invoke(vec);
            double z = (double) vec.getClass().getMethod("getZ").invoke(vec);

            // tenta construir um novo Vector3d da mesma classe do vec
            try {
                return vec.getClass().getConstructor(double.class, double.class, double.class)
                        .newInstance(x + ox, y + oy, z + oz);
            } catch (Throwable ignoredCtor) {
                // fallback: tenta achar um Vector3d conhecido
                Object v2 = newVector3d(x + ox, y + oy, z + oz);
                return (v2 != null) ? v2 : vec;
            }
        } catch (Throwable ignored) {}
        return vec;
    }

    private static Object newVector3d(double x, double y, double z) {
        String[] candidates = {
                "com.hypixel.hytale.math.Vector3d",
                "com.hypixel.hytale.util.math.Vector3d",
                "com.hypixel.hytale.protocol.util.Vector3d",
                "com.hypixel.hytale.server.core.math.Vector3d"
        };
        for (String cn : candidates) {
            try {
                Class<?> c = Class.forName(cn);
                return c.getConstructor(double.class, double.class, double.class).newInstance(x, y, z);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object newVector3f(float a, float b, float c0) {
        String[] candidates = {
                "com.hypixel.hytale.math.Vector3f",
                "com.hypixel.hytale.util.math.Vector3f",
                "com.hypixel.hytale.protocol.util.Vector3f",
                "com.hypixel.hytale.server.core.math.Vector3f"
        };
        for (String cn : candidates) {
            try {
                Class<?> c = Class.forName(cn);
                return c.getConstructor(float.class, float.class, float.class).newInstance(a, b, c0);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // =========================================================
    // Generic reflection helpers
    // =========================================================

    private static Object invokeNoArg(Object target, String... methodNames) {
        if (target == null) return null;
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object invokeOneArg(Object target, String methodName, Class<?> argType, Object arg) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName, argType);
            return m.invoke(target, arg);
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object invokeStaticNoArg(String className, String methodName) {
        try {
            Class<?> c = Class.forName(className);
            Method m = c.getMethod(methodName);
            return m.invoke(null);
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getStaticFieldIfExists(String className, String fieldName) {
        try {
            Class<?> c = Class.forName(className);
            Field f = c.getField(fieldName);
            return f.get(null);
        } catch (Throwable ignored) {}
        return null;
    }

    private static String firstStringFromArray(Object arr) {
        if (arr instanceof String[] a && a.length > 0) return a[0];
        return null;
    }

    private static String firstStringFromList(Object listObj) {
        if (listObj instanceof List<?> list && !list.isEmpty()) {
            Object v = list.get(0);
            return v != null ? String.valueOf(v) : null;
        }
        return null;
    }

    private static final class FallbackMarker {
        final UUID ownerId;
        FallbackMarker(UUID ownerId) { this.ownerId = ownerId; }
    }
}
