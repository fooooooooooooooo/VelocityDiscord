package ooo.foooooooooooo.velocitydiscord.events;


import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.network.ServerPlayerEntity;

public interface AdvancementCallback {
  Event<AdvancementCallback> EVENT = EventFactory.createArrayBacked(AdvancementCallback.class,
    (listeners) -> (player, advancement) -> {
      for (AdvancementCallback listener : listeners) {
        listener.getAdvancement(player, advancement);
      }
    }
  );

  void getAdvancement(ServerPlayerEntity player, Advancement advancement);
}
