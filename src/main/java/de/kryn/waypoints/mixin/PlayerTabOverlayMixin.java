package de.kryn.waypoints.mixin;

import de.kryn.waypoints.share.RelayClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gelbes Symbol hinter Spielern in der Tab-Liste, die Kryn's Waypoints nutzen. */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void kryns_waypoints$markModUsers(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        if (RelayClient.hasMod(info.getProfile().name())) {
            cir.setReturnValue(cir.getReturnValue().copy().append(Component.literal(" ✦").withStyle(ChatFormatting.YELLOW)));
        }
    }
}
