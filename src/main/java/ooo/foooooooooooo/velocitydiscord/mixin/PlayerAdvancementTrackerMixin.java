package ooo.foooooooooooo.velocitydiscord.mixin;


import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import ooo.foooooooooooo.velocitydiscord.events.AdvancementCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {
  @Shadow
  private ServerPlayerEntity owner;

  @Inject(
    method = "grantCriterion", at = @At(
    value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
  )
  )
  public void onAdvancement(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
    AdvancementCallback.EVENT.invoker().getAdvancement(this.owner, advancement);
  }
}