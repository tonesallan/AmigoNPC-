package br.tones.amigonpc.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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

      if (rec.lootingActive) {
         return Result.GATHERED;
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

      if (sectionAt(world, target.x, target.y, target.z) == null) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      BlockType current = blockTypeAt(world, target.x, target.y, target.z);
      if (current == null || current.getId() == null || !current.getId().equals(target.blockType.getId())) {
         clearTarget(rec);
         return Result.FOLLOWING;
      }

      if (!breakBlockAt(world, target.x, target.y, target.z)) {
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

      return Result.GATHERED;
   }

   // API de blocos do servidor 0.7.2: ChunkStore -> secao (Ref) -> BlockSection.
   // A secao e obtida pela coordenada de bloco; o id do bloco vem de BlockSection.get(x,y,z).
   private static Ref<ChunkStore> sectionAt(World world, int x, int y, int z) {
      ChunkStore chunkStore = world.getChunkStore();
      if (chunkStore == null) {
         return null;
      }

      Ref<ChunkStore> section = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
      return section != null && section.isValid() ? section : null;
   }

   private static BlockType blockTypeAt(World world, int x, int y, int z) {
      Ref<ChunkStore> section = sectionAt(world, x, y, z);
      if (section == null) {
         return null;
      }

      Store<ChunkStore> store = world.getChunkStore().getStore();
      BlockSection blockSection = (BlockSection)store.getComponent(section, BlockSection.getComponentType());
      if (blockSection == null) {
         return null;
      }

      return (BlockType)BlockType.getAssetMap().getAsset(blockSection.get(x, y, z));
   }

   // Quebra o bloco: id 0 / BlockType.EMPTY, flags 256 (com fisica de bloco), como o servidor faz em quebras normais.
   private static boolean breakBlockAt(World world, int x, int y, int z) {
      Ref<ChunkStore> section = sectionAt(world, x, y, z);
      if (section == null) {
         return false;
      }

      return BlockOperations.setBlock(world.getChunkStore(), section, x, y, z, 0, BlockType.EMPTY, 0, 0, 256);
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
               BlockType type;
               try {
                  type = blockTypeAt(world, x, y, z);
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
