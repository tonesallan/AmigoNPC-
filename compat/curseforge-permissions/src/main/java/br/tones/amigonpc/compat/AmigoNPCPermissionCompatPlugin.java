package br.tones.amigonpc.compat;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

public final class AmigoNPCPermissionCompatPlugin extends JavaPlugin {
    public static final PluginManifest MANIFEST = PluginManifest.corePlugin(AmigoNPCPermissionCompatPlugin.class).build();

    private static final Set<String> PUBLIC_AMIGO_SUBCOMMANDS = Set.of(
        "spawn",
        "despawn",
        "modelo",
        "modelooff",
        "defender",
        "defende",
        "log",
        "hud",
        "tipo",
        "tipos"
    );

    public AmigoNPCPermissionCompatPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Apply before command registration is finalized so current Hytale
        // does not bake an implicit permission into these legacy commands.
        patchPublicCommands();
    }

    @Override
    protected void start() {
        // Safety pass for load-order differences between Hytale builds.
        patchPublicCommands();
    }

    private void patchPublicCommands() {
        Map<String, AbstractCommand> commands = CommandManager.get().getCommandRegistration();

        AbstractCommand amigo = commands.get("amigo");
        if (amigo != null) {
            clearPermission(amigo);

            for (Map.Entry<String, AbstractCommand> entry : amigo.getSubCommands().entrySet()) {
                if (PUBLIC_AMIGO_SUBCOMMANDS.contains(entry.getKey().toLowerCase())) {
                    clearPermission(entry.getValue());
                }
            }
        }

        AbstractCommand loot = commands.get("loot");
        if (loot != null) {
            clearPermission(loot);
        }

        AbstractCommand autoLoot = commands.get("autoloot");
        if (autoLoot != null) {
            clearPermission(autoLoot);
            for (AbstractCommand child : autoLoot.getSubCommands().values()) {
                clearPermission(child);
            }
        }
    }

    private static void clearPermission(AbstractCommand command) {
        // Current Hytale explicitly requires requireNoPermission() for public commands.
        // Use reflection so this compatibility shim can still compile against older
        // server API snapshots that did not expose the method yet.
        try {
            command.getClass().getMethod("requireNoPermission").invoke(command);
            return;
        } catch (NoSuchMethodException ignored) {
            // Fall back to the legacy field approach below.
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                "Could not invoke requireNoPermission for " + command.getName(), ex
            );
        }

        Class<?> type = command.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("permission");
                field.setAccessible(true);
                field.set(command, null);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(
                    "Could not clear legacy command permission for " + command.getName(), ex
                );
            }
        }

        throw new IllegalStateException(
            "No supported public-command permission mechanism found for " + command.getName()
        );
    }
}
