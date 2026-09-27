package br.tones.amigonpc.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Compatibility implementation for the original CurseForge 2.0.1 JAR.
 *
 * The original plugin already invokes this helper BEFORE each public command is
 * registered. Update 6 removed canGeneratePermission=false as the way to declare
 * public commands; requireNoPermission() must now be called explicitly.
 *
 * This class intentionally changes permission declaration only. It does not alter
 * command execution or any AmigoNPC gameplay behavior.
 */
public final class CommandAccess {
    private CommandAccess() {
    }

    public static void makePublicRecursive(Object commandObject) {
        if (!(commandObject instanceof AbstractCommand command)) {
            return;
        }

        makePublic(command);

        for (Map.Entry<String, AbstractCommand> entry : command.getSubCommands().entrySet()) {
            AbstractCommand child = entry.getValue();
            if (child != null) {
                makePublicRecursive(child);
            }
        }
    }

    private static void makePublic(AbstractCommand command) {
        // Update 6+: commands that everyone may run must explicitly call
        // requireNoPermission() before completeRegistration().
        try {
            Method method = AbstractCommand.class.getMethod("requireNoPermission");
            method.invoke(command);
            return;
        } catch (NoSuchMethodException ignored) {
            // Older Hytale API: fall through to the legacy permission field.
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                "Could not declare command public before registration: " + command.getName(),
                ex
            );
        }

        // Compatibility with older builds where null permission represented public.
        Class<?> type = AbstractCommand.class;
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
                    "Could not clear legacy permission for command: " + command.getName(),
                    ex
                );
            }
        }

        throw new IllegalStateException(
            "No supported permission API found for command: " + command.getName()
        );
    }
}
