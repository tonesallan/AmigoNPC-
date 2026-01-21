package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

public final class AmigoCommand extends AbstractCommand {

    private final AmigoService service;

    public AmigoCommand(AmigoService service) {
        super("amigo", "Comandos do AmigoNPC");
        this.service = service;
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        String sub = firstArg(ctx);

        if (sub == null || sub.isBlank()) {
            ctx.sendMessage(Message.raw("§7[AmigoNPC] Use: §f/amigo spawn §7| §f/amigo despawn"));
            return CompletableFuture.completedFuture(null);
        }

        switch (sub.toLowerCase()) {
            case "spawn" -> {
                service.spawn(ctx);
                return CompletableFuture.completedFuture(null);
            }
            case "despawn" -> {
                service.despawn(ctx);
                return CompletableFuture.completedFuture(null);
            }
            default -> {
                ctx.sendMessage(Message.raw("§7[AmigoNPC] Subcomando inválido. Use: §f/amigo spawn §7| §f/amigo despawn"));
                return CompletableFuture.completedFuture(null);
            }
        }
    }

    /**
     * Pega o primeiro argumento do comando sem depender de um método fixo do CommandContext.
     * Tenta alguns nomes comuns e cai em null se não achar.
     */
    private static String firstArg(CommandContext ctx) {
        // tenta métodos comuns: args(), arguments(), getArguments(), rawArgs(), etc.
        String[] candidates = {
                "args", "arguments", "getArguments", "getArgs", "rawArguments", "getRawArguments"
        };

        for (String name : candidates) {
            try {
                Method m = ctx.getClass().getMethod(name);
                Object result = m.invoke(ctx);

                if (result instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    return first == null ? null : String.valueOf(first);
                }
                if (result instanceof String[] arr && arr.length > 0) {
                    return arr[0];
                }
            } catch (Throwable ignored) {
                // tenta o próximo
            }
        }

        return null;
    }
}
