package br.tones.amigonpc.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Camada de compatibilidade (Bridge) para evitar quebrar com variação de API entre builds.
 *
 * Regras:
 * - NÃO importar classes ECS diretamente (TransformComponent, etc.)
 * - Tudo via reflexão
 * - Se algo não existir, falha de forma segura (sem quebrar compilação)
 */
public final class HytaleBridge {

    private HytaleBridge() {}

    // Guarda o último erro (para debug no chat/console depois, se você quiser)
    private static volatile String LAST_ERROR;

    public static String getLastError() {
        return LAST_ERROR;
    }

    private static void setError(String msg) {
        LAST_ERROR = msg;
    }

    /**
     * Tenta obter o World a partir do sender do CommandContext.
     * Retorna null se não conseguir.
     */
    public static Object tryGetWorldFromCommandContext(Object commandContext) {
        try {
            if (commandContext == null) return null;

            Method senderMethod = commandContext.getClass().getMethod("sender");
            Object sender = senderMethod.invoke(commandContext);
            if (sender == null) return null;

            // Nomes comuns em builds diferentes
            String[] candidates = { "getWorld", "world", "getCurrentWorld", "getPlayerWorld" };

            for (String name : candidates) {
                try {
                    Method m = sender.getClass().getMethod(name);
                    Object world = m.invoke(sender);
                    if (world != null) return world;
                } catch (Throwable ignored) {}
            }

            return null;
        } catch (Throwable t) {
            setError("tryGetWorldFromCommandContext falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return null;
        }
    }

    /**
     * Executa uma task no contexto do World.
     * Tenta world.execute(Runnable) e alternativas comuns.
     */
    public static boolean worldExecute(Object world, Runnable task) {
        if (world == null || task == null) return false;

        // 1) world.execute(Runnable)
        try {
            Method m = world.getClass().getMethod("execute", Runnable.class);
            m.invoke(world, task);
            return true;
        } catch (Throwable ignored) {}

        // 2) world.run(Runnable) (fallback)
        try {
            Method m = world.getClass().getMethod("run", Runnable.class);
            m.invoke(world, task);
            return true;
        } catch (Throwable ignored) {}

        setError("Não achei método world.execute/run compatível nesta build.");
        return false;
    }

    /**
     * Spawn ECS compatível por reflexão.
     *
     * @param worldObj objeto World (não tipado para evitar imports frágeis)
     * @param ownerId UUID do dono
     * @return true se disparou spawn; false se falhou
     */
    public static boolean spawnBasicNpc(Object worldObj, UUID ownerId) {
        if (worldObj == null) {
            setError("World é null (não foi possível obter world do player).");
            return false;
        }
        if (ownerId == null) {
            setError("ownerId é null.");
            return false;
        }

        return worldExecute(worldObj, () -> {
            try {
                // World.getEntityStore()
                Object store = invokeNoArg(worldObj, "getEntityStore", "entityStore");
                if (store == null) {
                    setError("World.getEntityStore() não encontrado.");
                    return;
                }

                // Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder()
                Object holder = createEntityHolder();
                if (holder == null) {
                    setError("Não consegui criar Holder via EntityStore.REGISTRY.newHolder().");
                    return;
                }

                // Adiciona componentes mínimos SE existirem (sem quebrar se não existir)
                tryAddTransform(holder);
                tryAddUUIDComponent(holder, ownerId);
                tryAddNetworkId(holder, store);

                // store.addEntity(holder, AddReason.SPAWN)
                Object addReasonSpawn = getEnumConstant(
                        "com.hypixel.hytale.server.core.universe.world.storage.EntityStore$AddReason",
                        "SPAWN"
                );

                if (addReasonSpawn == null) {
                    // Se não existir AddReason, tenta addEntity(holder) sem reason
                    boolean ok = invokeAddEntityWithoutReason(store, holder);
                    if (!ok) setError("Não consegui chamar addEntity(holder[, reason]) nesta build.");
                    return;
                }

                boolean ok = invokeAddEntityWithReason(store, holder, addReasonSpawn);
                if (!ok) setError("Não consegui chamar addEntity(holder, AddReason.SPAWN) nesta build.");

            } catch (Throwable t) {
                setError("spawnBasicNpc falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    // ----------------------------
    // Helpers: Holder / Registry
    // ----------------------------

    private static Object createEntityHolder() {
        try {
            Class<?> entityStoreClass = Class.forName("com.hypixel.hytale.server.core.universe.world.storage.EntityStore");
            Field registryField = entityStoreClass.getField("REGISTRY");
            Object registry = registryField.get(null);
            if (registry == null) return null;

            // registry.newHolder()
            Method newHolder = registry.getClass().getMethod("newHolder");
            return newHolder.invoke(registry);

        } catch (Throwable t) {
            setError("createEntityHolder falhou: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return null;
        }
    }

    private static boolean invokeAddEntityWithReason(Object store, Object holder, Object addReasonSpawn) {
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("addEntity")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[1].isInstance(addReasonSpawn)) {
                    m.invoke(store, holder, addReasonSpawn);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean invokeAddEntityWithoutReason(Object store, Object holder) {
        try {
            Method m = store.getClass().getMethod("addEntity", holder.getClass());
            m.invoke(store, holder);
            return true;
        } catch (Throwable ignored) {}

        // tenta por varredura (caso o Holder seja interface/superclasse)
        try {
            for (Method m : store.getClass().getMethods()) {
                if (!m.getName().equals("addEntity")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1) {
                    m.invoke(store, holder);
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    // ----------------------------
    // Helpers: Components (opcionais)
    // ----------------------------

    private static void tryAddTransform(Object holder) {
        // Possíveis nomes (dependendo da build)
        String[] classNames = {
                "com.hypixel.hytale.server.core.universe.world.entity.component.TransformComponent",
                "com.hypixel.hytale.server.core.universe.world.entity.components.TransformComponent"
        };

        Object comp = tryInstantiateFirstExisting(classNames);
        if (comp != null) invokeHolderAdd(holder, comp);
    }

    private static void tryAddUUIDComponent(Object holder, UUID ownerId) {
        String[] classNames = {
                "com.hypixel.hytale.server.core.universe.world.entity.component.UUIDComponent",
                "com.hypixel.hytale.server.core.universe.world.entity.components.UUIDComponent"
        };

        // tenta construtor UUIDComponent(UUID)
        Object comp = tryInstantiateUUIDComponent(classNames, ownerId);
        if (comp != null) {
            invokeHolderAdd(holder, comp);
        }
    }

    private static void tryAddNetworkId(Object holder, Object store) {
        String[] classNames = {
                "com.hypixel.hytale.server.core.universe.world.entity.component.NetworkIdComponent",
                "com.hypixel.hytale.server.core.universe.world.entity.components.NetworkIdComponent"
        };

        Object networkId = null;

        try {
            Object externalData = invokeNoArg(store, "getExternalData", "externalData");
            if (externalData != null) {
                networkId = invokeNoArg(externalData, "takeNextNetworkId", "nextNetworkId", "getNextNetworkId");
            }
        } catch (Throwable ignored) {}

        if (networkId == null) return;

        Object comp = tryInstantiateSingleArgFirstExisting(classNames, networkId);
        if (comp != null) invokeHolderAdd(holder, comp);
    }

    private static void invokeHolderAdd(Object holder, Object component) {
        try {
            // holder.add(component)
            for (Method m : holder.getClass().getMethods()) {
                if (!m.getName().equals("add")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1) {
                    m.invoke(holder, component);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    // ----------------------------
    // Generic reflection helpers
    // ----------------------------

    private static Object invokeNoArg(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Throwable ignored) {}
        }
        return null;
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

    private static Object tryInstantiateFirstExisting(String[] classNames) {
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

    private static Object tryInstantiateUUIDComponent(String[] classNames, UUID ownerId) {
        for (String cn : classNames) {
            try {
                Class<?> c = Class.forName(cn);

                // 1) ctor(UUID)
                try {
                    Constructor<?> ctor = c.getDeclaredConstructor(UUID.class);
                    ctor.setAccessible(true);
                    return ctor.newInstance(ownerId);
                } catch (Throwable ignoredCtor) {}

                // 2) ctor() e tenta set/field (se existir)
                try {
                    Constructor<?> ctor = c.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    Object obj = ctor.newInstance();

                    // tenta método setUuid(UUID) / setId(UUID) / setValue(UUID)
                    String[] setters = { "setUuid", "setId", "setValue" };
                    for (String s : setters) {
                        try {
                            Method m = c.getMethod(s, UUID.class);
                            m.invoke(obj, ownerId);
                            return obj;
                        } catch (Throwable ignoredSetter) {}
                    }

                    // tenta field uuid/id/value
                    String[] fields = { "uuid", "id", "value" };
                    for (String f : fields) {
                        try {
                            Field field = c.getDeclaredField(f);
                            field.setAccessible(true);
                            field.set(obj, ownerId);
                            return obj;
                        } catch (Throwable ignoredField) {}
                    }
                } catch (Throwable ignoredCtor2) {}

            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object tryInstantiateSingleArgFirstExisting(String[] classNames, Object arg) {
        for (String cn : classNames) {
            try {
                Class<?> c = Class.forName(cn);

                // tenta achar um ctor de 1 argumento compatível
                for (Constructor<?> ctor : c.getDeclaredConstructors()) {
                    Class<?>[] p = ctor.getParameterTypes();
                    if (p.length == 1 && (arg == null || p[0].isInstance(arg) || isPrimitiveWrapperMatch(p[0], arg.getClass()))) {
                        ctor.setAccessible(true);
                        return ctor.newInstance(arg);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static boolean isPrimitiveWrapperMatch(Class<?> paramType, Class<?> argType) {
        // Ajuda em casos tipo param long / arg Long, etc.
        if (!paramType.isPrimitive()) return false;
        if (paramType == int.class && argType == Integer.class) return true;
        if (paramType == long.class && argType == Long.class) return true;
        if (paramType == boolean.class && argType == Boolean.class) return true;
        if (paramType == double.class && argType == Double.class) return true;
        if (paramType == float.class && argType == Float.class) return true;
        if (paramType == short.class && argType == Short.class) return true;
        if (paramType == byte.class && argType == Byte.class) return true;
        if (paramType == char.class && argType == Character.class) return true;
        return false;
    }
}
