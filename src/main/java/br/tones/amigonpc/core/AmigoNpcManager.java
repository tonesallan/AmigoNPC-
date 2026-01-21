package br.tones.amigonpc.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia 1 NPC por player:
 * - Spawn -> guarda uma referência/handle retornada pelo EntityStore.addEntity(...)
 * - Despawn -> remove usando essa referência (ou tenta fallbacks)
 *
 * Tudo por reflexão para não quebrar com variações de API.
 */
public final class AmigoNpcManager {

    private static final AmigoNpcManager SHARED = new AmigoNpcManager();

    public static AmigoNpcManager getShared() {
        return SHARED;
    }

    private final Map<UUID, Object> npcRefPorPlayer = new ConcurrentHashMap<>();
    private static volatile String LAST_ERROR;

    public String getLastError() {
        return LAST_ERROR;
    }

    private static void setError(String msg) {
        LAST_ERROR = msg;
    }

    public boolean hasNpc(UUID ownerId) {
        return ownerId != null && npcRefPorPlayer.containsKey(ownerId);
    }

    /**
     * Spawna e guarda o handle retornado pelo store.addEntity(...), se houver.
     */
    public boolean spawn(Object worldObj, UUID ownerId) {
        if (worldObj == null || ownerId == null) {
            setError("worldObj ou ownerId null.");
            return false;
        }

        // Se já tem, tenta despawn primeiro (mantém 1 por player)
        if (hasNpc(ownerId)) {
            despawn(worldObj, ownerId);
        }

        final Object[] outRef = new Object[1];

        boolean queued = HytaleBridge.worldExecute(worldObj, () -> {
            try {
                Object store = invokeNoArg(worldObj, "getEntityStore", "entityStore");
                if (store == null) {
                    setError("World.getEntityStore() não encontrado.");
                    return;
                }

                Object holder = createEntityHolder();
                if (holder == null) {
                    setError("Falha ao criar Holder (EntityStore.REGISTRY.newHolder).");
                    return;
                }

                // Componentes mínimos, se existirem na build
                // (a mesma filosofia do HytaleBridge, mas aqui queremos capturar o retorno do addEntity)
                tryAddTransform(holder);
                tryAddUUIDComponent(holder, ownerId);
                tryAddNetworkId(holder, store);

                Object addReasonSpawn = getEnumConstant(
                        "com.hypixel.hytale.server.core.universe.world.storage.EntityStore$AddReason",
                        "SPAWN"
                );

                // Chama addEntity e tenta capturar retorno (se retornar algo)
                Object ref = null;

                if (addReasonSpawn != null) {
                    ref = invokeAddEntityCaptureReturn(store, holder, addReasonSpawn);
                }
                if (ref == null) {
                    ref = invokeAddEntityCaptureReturn(store, holder);
                }

                outRef[0] = ref; // pode ser null em builds onde addEntity retorna void

            } catch (Throwable t) {
                setError("spawn() falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });

        if (!queued) {
            setError("world.execute falhou: " + HytaleBridge.getLastError());
            return false;
        }

        // Se tivemos retorno, guardamos para despawn preciso.
        if (outRef[0] != null) {
            npcRefPorPlayer.put(ownerId, outRef[0]);
        } else {
            // Ainda assim consideramos spawn solicitado, mas despawn pode precisar fallback.
            npcRefPorPlayer.put(ownerId, new FallbackMarker(ownerId));
        }

        return true;
    }

    /**
     * Despawna o NPC do player.
     */
    public boolean despawn(Object worldObj, UUID ownerId) {
        if (worldObj == null || ownerId == null) {
            setError("worldObj ou ownerId null.");
            return false;
        }

        Object ref = npcRefPorPlayer.remove(ownerId);
        if (ref == null) {
            setError("Nenhum NPC registrado para este player.");
            return false;
        }

        boolean queued = HytaleBridge.worldExecute(worldObj, () -> {
            try {
                Object store = invokeNoArg(worldObj, "getEntityStore", "entityStore");
                if (store == null) {
                    setError("World.getEntityStore() não encontrado.");
                    return;
                }

                // Se for marker, não temos ref real; tenta fallbacks (limitado sem API garantida)
                if (ref instanceof FallbackMarker) {
                    // fallback leve: tenta "removeByOwnerUuid" se existir (raro), senão só avisa
                    boolean ok = tryRemoveByOwnerUuidIfExists(store, ownerId);
                    if (!ok) {
                        setError("Não há ref real para despawn nesta build (addEntity retornou void). " +
                                "Vamos ajustar no próximo passo se seu EntityStore expuser um método de busca.");
                    }
                    return;
                }

                // 1) store.removeEntity(ref)
                if (tryInvokeRemoveEntity(store, ref)) return;

                // 2) store.removeEntity(ref, reason) se houver
                Object removeReason = getEnumConstant(
                        "com.hypixel.hytale.server.core.universe.world.storage.EntityStore$RemoveReason",
                        "DESPAWN"
                );
                if (removeReason != null && tryInvokeRemoveEntity(store, ref, removeReason)) return;

                setError("Não achei um método removeEntity compatível para esse ref nesta build.");

            } catch (Throwable t) {
                setError("despawn() falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });

        if (!queued) {
            setError("world.execute falhou: " + HytaleBridge.getLastError());
            return false;
        }

        return true;
    }

    // =========================
    // Reflection helpers (local)
    // =========================

    private static Object invokeNoArg(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object createEntityHolder() {
        try {
            Class<?> entityStoreClass = Class.forName("com.hypixel.hytale.server.core.universe.world.storage.EntityStore");
            Field registryField = entityStoreClass.getField("REGISTRY");
            Object registry = registryField.get(null);
            if (registry == null) return null;

            Method newHolder = registry.getClass().getMethod("newHolder");
            return newHolder.invoke(registry);

        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getEnumConstant(String enumClassName, String constantName) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (!enumClass.isEnum()) return null;
            @SuppressWarnings("unchecked")
            Object constant = Enum.valueOf((Class<? extends Enum>) enumClass, constantName);
            return constant;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeAddEntityCaptureReturn(Object store, Object holder) {
        try {
            // tenta métodos addEntity com 1 parâmetro
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("addEntity")) continue;
                if (m.getParameterCount() != 1) continue;
                Object r = m.invoke(store, holder);
                return r; // pode ser null/void
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object invokeAddEntityCaptureReturn(Object store, Object holder, Object reason) {
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("addEntity")) continue;
                if (m.getParameterCount() != 2) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p[1].isInstance(reason)) {
                    Object r = m.invoke(store, holder, reason);
                    return r; // pode ser null/void
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean tryInvokeRemoveEntity(Object store, Object ref) {
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("removeEntity")) continue;
                if (m.getParameterCount() != 1) continue;
                m.invoke(store, ref);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
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

    private static boolean tryRemoveByOwnerUuidIfExists(Object store, UUID ownerId) {
        String[] names = { "removeByOwnerUuid", "removeEntitiesByOwnerUuid", "removeByUuid" };
        for (String name : names) {
            try {
                Method m = store.getClass().getMethod(name, UUID.class);
                m.invoke(store, ownerId);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    // =========================
    // Components (optional)
    // =========================

    private static void tryAddTransform(Object holder) {
        String[] classNames = {
                "com.hypixel.hytale.server.core.universe.world.entity.component.TransformComponent",
                "com.hypixel.hytale.server.core.universe.world.entity.components.TransformComponent"
        };
        Object comp = tryInstantiateNoArg(classNames);
        if (comp != null) invokeHolderAdd(holder, comp);
    }

    private static void tryAddUUIDComponent(Object holder, UUID ownerId) {
        String[] classNames = {
                "com.hypixel.hytale.server.core.universe.world.entity.component.UUIDComponent",
                "com.hypixel.hytale.server.core.universe.world.entity.components.UUIDComponent"
        };
        Object comp = tryInstantiateUUID(classNames, ownerId);
        if (comp != null) invokeHolderAdd(holder, comp);
    }

    private static void tryAddNetworkId(Object holder, Object store) {
        String[] classNames = {
                "com.hypixel.hytale.server.core.universe.world.entity.component.NetworkIdComponent",
                "com.hypixel.hytale.server.core.universe.world.entity.components.NetworkIdComponent"
        };

        Object externalData = invokeNoArg(store, "getExternalData", "externalData");
        if (externalData == null) return;

        Object networkId = invokeNoArg(externalData, "takeNextNetworkId", "nextNetworkId", "getNextNetworkId");
        if (networkId == null) return;

        Object comp = tryInstantiateSingleArg(classNames, networkId);
        if (comp != null) invokeHolderAdd(holder, comp);
    }

    private static Object tryInstantiateNoArg(String[] classNames) {
        for (String cn : classNames) {
            try {
                Class<?> c = Class.forName(cn);
                Constructor<?> ctor = c.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object tryInstantiateUUID(String[] classNames, UUID ownerId) {
        for (String cn : classNames) {
            try {
                Class<?> c = Class.forName(cn);
                try {
                    Constructor<?> ctor = c.getDeclaredConstructor(UUID.class);
                    ctor.setAccessible(true);
                    return ctor.newInstance(ownerId);
                } catch (Throwable ignoredCtor) {}
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object tryInstantiateSingleArg(String[] classNames, Object arg) {
        for (String cn : classNames) {
            try {
                Class<?> c = Class.forName(cn);
                for (Constructor<?> ctor : c.getDeclaredConstructors()) {
                    if (ctor.getParameterCount() != 1) continue;
                    ctor.setAccessible(true);
                    try {
                        return ctor.newInstance(arg);
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void invokeHolderAdd(Object holder, Object component) {
        try {
            for (Method m : holder.getClass().getMethods()) {
                if (!m.getName().equals("add")) continue;
                if (m.getParameterCount() != 1) continue;
                m.invoke(holder, component);
                return;
            }
        } catch (Throwable ignored) {}
    }

    // Marker interno: usado quando addEntity não retorna handle/ref
    private static final class FallbackMarker {
        final UUID ownerId;
        FallbackMarker(UUID ownerId) { this.ownerId = ownerId; }
    }
}
