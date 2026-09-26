package br.tones.amigonpc.core.lifecycle;

import br.tones.amigonpc.core.AmigoNpcManager;
import br.tones.amigonpc.core.downed.ManualReviveService;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.function.Supplier;
import org.joml.Vector3d;

public final class ManualReviveCoordinator {
   private static final double MAX_REVIVE_DISTANCE = 3.5;
   private final Supplier<EventRegistry> eventRegistrySupplier;
   private PacketFilter packetFilter;

   public ManualReviveCoordinator(Supplier<EventRegistry> eventRegistrySupplier) {
      this.eventRegistrySupplier = eventRegistrySupplier;
   }

   @SuppressWarnings("removal")
   public void register() {
      EventRegistry registry = this.eventRegistrySupplier.get();
      if (registry == null) {
         return;
      }

      registry.registerGlobal(PlayerInteractEvent.class, this::onInteract);
      if (this.packetFilter == null) {
         this.packetFilter = PacketAdapters.registerInbound((PlayerPacketWatcher)this::onInboundPacket);
      }
   }

   @SuppressWarnings("removal")
   private void onInteract(PlayerInteractEvent event) {
      try {
         if (event == null || event.getActionType() != InteractionType.Use || event.getTargetRef() == null) {
            return;
         }

         UUID npcOwnerId = AmigoNpcManager.getShared().getOwnerFromRef(event.getTargetRef());
         if (npcOwnerId == null || !AmigoNpcManager.getShared().isDowned(npcOwnerId)) {
            return;
         }

         Player player = event.getPlayer();
         PlayerRef playerRef = player == null ? null : player.getPlayerRef();
         Entity target = event.getTargetEntity();
         if (playerRef == null || target == null || !isNearEnough(player, target)) {
            if (playerRef != null) {
               ManualReviveService.getShared().cancel(playerRef.getUuid());
            }
            return;
         }

         event.setCancelled(true);
         ManualReviveService.getShared().beginOrContinue(playerRef, npcOwnerId);
      } catch (Throwable ignored) {
      }
   }

   private void onInboundPacket(PlayerRef playerRef, Packet packet) {
      try {
         if (!(packet instanceof SyncInteractionChains chains) || chains.updates == null) {
            return;
         }

         for (SyncInteractionChain chain : chains.updates) {
            if (chain != null && chain.interactionType == InteractionType.Use) {
               ManualReviveService.getShared().onUseChain(playerRef, chain);
            }
         }
      } catch (Throwable ignored) {
      }
   }

   private static boolean isNearEnough(Player player, Entity target) {
      try {
         TransformComponent playerTransform = player.getTransformComponent();
         TransformComponent targetTransform = target.getTransformComponent();
         if (playerTransform == null || targetTransform == null) {
            return false;
         }

         Vector3d a = playerTransform.getPosition();
         Vector3d b = targetTransform.getPosition();
         if (a == null || b == null) {
            return false;
         }

         double dx = a.x() - b.x();
         double dy = a.y() - b.y();
         double dz = a.z() - b.z();
         return dx * dx + dy * dy + dz * dz <= MAX_REVIVE_DISTANCE * MAX_REVIVE_DISTANCE;
      } catch (Throwable ignored) {
         return false;
      }
   }

   public void shutdown() {
      PacketFilter filter = this.packetFilter;
      this.packetFilter = null;
      if (filter != null) {
         try {
            PacketAdapters.deregisterInbound(filter);
         } catch (Throwable ignored) {
         }
      }

      ManualReviveService.getShared().shutdown();
   }
}
