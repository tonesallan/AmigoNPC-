package br.tones.amigonpc.core.events;

import br.tones.amigonpc.api.events.NpcExperienceGainedEvent;
import br.tones.amigonpc.api.events.NpcExperienceGainedListener;
import br.tones.amigonpc.api.events.NpcLevelUpEvent;
import br.tones.amigonpc.api.events.NpcLevelUpListener;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AmigoEventBus {
   private static final CopyOnWriteArrayList<NpcExperienceGainedListener> XP_LISTENERS = new CopyOnWriteArrayList<>();
   private static final CopyOnWriteArrayList<NpcLevelUpListener> LEVEL_LISTENERS = new CopyOnWriteArrayList<>();

   private AmigoEventBus() {
   }

   public static void register(Object listener) {
      if (listener != null) {
         if (listener instanceof NpcExperienceGainedListener l && !XP_LISTENERS.contains(l)) {
            XP_LISTENERS.add(l);
         }

         if (listener instanceof NpcLevelUpListener l && !LEVEL_LISTENERS.contains(l)) {
            LEVEL_LISTENERS.add(l);
         }
      }
   }

   public static void unregister(Object listener) {
      if (listener != null) {
         if (listener instanceof NpcExperienceGainedListener l) {
            XP_LISTENERS.remove(l);
         }

         if (listener instanceof NpcLevelUpListener l) {
            LEVEL_LISTENERS.remove(l);
         }
      }
   }

   public static void post(Object event) {
      if (event != null) {
         if (event instanceof NpcExperienceGainedEvent e) {
            for (NpcExperienceGainedListener l : XP_LISTENERS) {
               try {
                  l.onNpcExperienceGained(e);
               } catch (Throwable var5) {
               }
            }
         } else {
            if (event instanceof NpcLevelUpEvent e) {
               for (NpcLevelUpListener l : LEVEL_LISTENERS) {
                  try {
                     l.onNpcLevelUp(e);
                  } catch (Throwable var6) {
                  }
               }
            }
         }
      }
   }
}
