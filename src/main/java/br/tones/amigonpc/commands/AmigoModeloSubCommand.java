package br.tones.amigonpc.commands;

import br.tones.amigonpc.core.AmigoPersistence;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmigoModeloSubCommand extends AbstractPlayerCommand {
   private final RequiredArg modelArg;
   private final OptionalArg scaleArg;

   protected boolean canGeneratePermission() {
      return false;
   }

   public AmigoModeloSubCommand() {
      super("modelo", "Define o modelo (aparência) do AmigoNPC");
      this.modelArg = this.withRequiredArg("modelId", "Model asset id (ex.: npc.male_head_01)", ArgTypes.STRING);
      this.scaleArg = this.withOptionalArg("scale", "Escala do modelo (padrão: 1.0)", ArgTypes.DOUBLE);
   }

   protected void execute(CommandContext ctx, Store store, Ref playerEntityRef, PlayerRef playerRef, World world) {
      String modelId = (String)this.modelArg.get(ctx);
      double scale = (double)1.0F;
      if (this.scaleArg.provided(ctx)) {
         Object v = this.scaleArg.get(ctx);
         if (v instanceof Double) {
            Double d = (Double)v;
            scale = d;
         }
      }

      AmigoPersistence.saveModel(playerRef.getUuid(), modelId, scale);
      ctx.sendMessage(Message.raw("§a[AmigoNPC] Modelo salvo: §f" + modelId + "§a (scale=" + scale + ")"));
      ctx.sendMessage(Message.raw("§7[AmigoNPC] Para aplicar: use §f/amigo despawn§7 e depois §f/amigo spawn§7."));
   }
}
