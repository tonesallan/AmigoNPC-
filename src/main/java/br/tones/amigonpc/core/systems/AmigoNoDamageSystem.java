package br.tones.amigonpc.core.systems;

import br.tones.amigonpc.core.AmigoNpcManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AmigoNoDamageSystem extends DamageEventSystem {
   @Nonnull
   public Query getQuery() {
      return Query.any();
   }

   @Nullable
   public SystemGroup getGroup() {
      return DamageModule.get().getFilterDamageGroup();
   }

   public void handle(
      int index,
      @Nonnull ArchetypeChunk<EntityStore> chunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> buffer,
      @Nonnull Damage damage
   ) {
      Object targetRef = chunk.getReferenceTo(index);
      if (AmigoNpcManager.getShared().isAmigoRef(targetRef)) {
         damage.setCancelled(true);
      }
   }
}
