package br.tones.amigonpc.core;

import br.tones.amigonpc.core.xp.sources.NpcCollectXpSource;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.joml.Vector3d;

final class NpcGatheringSupport {
   private static final long SCAN_INTERVAL_MILLIS = 1200L;
   private static final int HORIZONTAL_RADIUS = 3;
   private static final int VERTICAL_RADIUS = 2;

   private NpcGatheringSupport() {
   }

   static Result tick(
      World world,
      UUID ownerId,
      AmigoNpcManager.NpcRecord rec,
      Vector3d npcPos,
      long now,
      boolean inCombat,
      NpcGatheringSupport.FullBackpackNotifier fullBackpackNotifier
   ) {
      if (rec == null || ownerId == null || world == null || npcPos == null || rec.downed) {
         return Result.IDLE;
      }

      if (inCombat) {
         clearTarget(rec);
         return Result.COMBAT;
      }

      if (!rec.autoLootEnabled) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      if (rec.lootingActive) {
         return Result.GATHERING;
      }

      SimpleItemContainer bag = rec.backpack;
      if (bag == null) {
         bag = AmigoPersistence.loadBackpack(ownerId);
         rec.backpack = bag;
      }

      if (bag == null) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      if (NpcBackpackLootSupport.isBackpackCompletelyFull(bag)) {
         rec.lootPausedInventoryFull = true;
         clearTarget(rec);
         fullBackpackNotifier.notify(rec, ownerId, world, now);
         return Result.FULL;
      }

      if (now < rec.nextGatherScanMillis) {
         return Result.FOLLOWING;
      }

      rec.nextGatherScanMillis = now + SCAN_INTERVAL_MILLIS;
      try {
         if (world.getGameplayConfig() == null
            || world.getGameplayConfig().getWorldConfig() == null
            || !world.getGameplayConfig().getWorldConfig().isBlockBreakingAllowed()
            || !world.getGameplayConfig().getWorldConfig().isBlockGatheringAllowed()) {
            clearTarget(rec);
            return Result.FOLLOWING;
         }
      } catch (Throwable ignored) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      ResourceBlock target = findNearestResource(world, npcPos);
      if (target == null) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      List<ItemStack> drops = dropsFor(target.blockType);
      if (drops == null || drops.isEmpty()) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      if (!bag.canAddItemStacks(drops)) {
         rec.lootPausedInventoryFull = true;
         clearTarget(rec);
         fullBackpackNotifier.notify(rec, ownerId, world, now);
         return Result.FULL;
      }

      WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(target.x, target.z));
      if (chunk == null) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      BlockType current = chunk.getBlockType(target.x, target.y, target.z);
      if (current == null || current.getId() == null || !current.getId().equals(target.blockType.getId())) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      if (!chunk.breakBlock(target.x, target.y, target.z)) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      bag.addItemStacks(drops);
      rec.backpackDirty = true;
      if (rec.nextBackpackSaveMillis <= now) {
         rec.nextBackpackSaveMillis = now + 500L;
      }
      rec.lootPausedInventoryFull = false;
      rec.gatherTargetX = target.x;
      rec.gatherTargetY = target.y;
      rec.gatherTargetZ = target.z;

      try {
         NpcCollectXpSource.onPickup(ownerId, 1, world.getName());
      } catch (Throwable ignored) {
      }

      return Result.GATHERED;
   }

   private static ResourceBlock findNearestResource(World world, Vector3d npcPos) {
      int cx = (int)Math.floor(npcPos.x());
      int cy = (int)Math.floor(npcPos.y());
      int cz = (int)Math.floor(npcPos.z());
      ResourceBlock best = null;
      double bestDistanceSq = Double.POSITIVE_INFINITY;

      for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
         int y = cy + dy;
         for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
            for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
               if (dx == 0 && dy == 0 && dz == 0) {
                  continue;
               }

               double distanceSq = dx * dx + dy * dy + dz * dz;
               if (distanceSq > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS || distanceSq >= bestDistanceSq) {
                  continue;
               }

               int x = cx + dx;
               int z = cz + dz;
               WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
               if (chunk == null) {
                  continue;
               }

               BlockType type;
               try {
                  type = chunk.getBlockType(x, y, z);
               } catch (Throwable ignored) {
                  continue;
               }

               if (!isNaturalResource(type)) {
                  continue;
               }

               best = new ResourceBlock(x, y, z, type);
               bestDistanceSq = distanceSq;
            }
         }
      }

      return best;
   }

   private static boolean isNaturalResource(BlockType type) {
      if (type == null || type == BlockType.EMPTY || type.isUnknown() || type.getGathering() == null) {
         return false;
      }

      BlockGathering gathering = type.getGathering();
      BlockBreakingDropType breaking = gathering.getBreaking();
      if (breaking == null || breaking.getQuantity() <= 0) {
         return false;
      }

      String id = type.getId() == null ? "" : type.getId().toLowerCase(Locale.ROOT);
      String group = type.getGroup() == null ? "" : type.getGroup().toLowerCase(Locale.ROOT);
      String gatherType = breaking.getGatherType() == null ? "" : breaking.getGatherType().toLowerCase(Locale.ROOT);

      return id.startsWith("ore_")
         || id.contains("_ore_")
         || id.endsWith("_ore")
         || id.contains("_trunk")
         || id.contains("rock_crystal")
         || id.contains("_crystal_")
         || id.contains("mineral")
         || id.contains("resource_deposit")
         || group.contains("ores")
         || group.contains("natural-ore")
         || gatherType.contains("ore");
   }

   private static List<ItemStack> dropsFor(BlockType type) {
      try {
         BlockBreakingDropType breaking = type.getGathering().getBreaking();
         return BlockHarvestUtils.getDrops(
            type,
            Math.max(1, breaking.getQuantity()),
            breaking.getItemId(),
            breaking.getDropListId()
         );
      } catch (Throwable ignored) {
         return List.of();
      }
   }

   private static void clearTarget(AmigoNpcManager.NpcRecord rec) {
      rec.gatherTargetX = Integer.MIN_VALUE;
      rec.gatherTargetY = Integer.MIN_VALUE;
      rec.gatherTargetZ = Integer.MIN_VALUE;
   }

   enum Result {
      COMBAT,
      GATHERED,
      FULL,
      FOLLOWING,
      IDLE
   }

   @FunctionalInterface
   interface FullBackpackNotifier {
      void notify(AmigoNpcManager.NpcRecord rec, UUID ownerId, Object worldObj, long now);
   }

   private static final class ResourceBlock {
      final int x;
      final int y;
      final int z;
      final BlockType blockType;

      ResourceBlock(int x, int y, int z, BlockType blockType) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.blockType = blockType;
      }
   }
}
