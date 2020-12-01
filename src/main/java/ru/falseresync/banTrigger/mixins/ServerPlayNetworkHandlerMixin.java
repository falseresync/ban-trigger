package ru.falseresync.banTrigger.mixins;

import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.fabricmc.fabric.impl.networking.PacketTypes;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.falseresync.banTrigger.BanTrigger;

import java.io.IOException;

import static ru.falseresync.banTrigger.BanTrigger.CONTAINER_OPENS;
import static ru.falseresync.banTrigger.BanTrigger.EXECUTOR_SERVICE;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(
        method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V",
        at = @At("HEAD")
    )
    private void onSendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener, CallbackInfo ci) {
        if (packet instanceof CustomPayloadS2CPacket) {
            CustomPayloadS2CPacket cPacket = (CustomPayloadS2CPacket) packet;
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            try {
                cPacket.write(buf);
            } catch (IOException ignored) {}

            Identifier id = buf.readIdentifier();

            System.out.println(id.toString());

            if (id.equals(PacketTypes.OPEN_CONTAINER)) {
                HitResult hit = player.getCameraEntity().rayTrace(20.0D, 0.0F, false);
                BlockPos pos;
                if (hit.getType() == HitResult.Type.BLOCK) {
                    pos = ((BlockHitResult) hit).getBlockPos();
                } else {
                    pos = player.getBlockPos();
                }
                EXECUTOR_SERVICE.submit(() -> {
                    try {
                        CONTAINER_OPENS.put(new BanTrigger.LogEntry(pos.getX(), pos.getY(), pos.getZ(), player.getUuid().toString()));
                    } catch (InterruptedException ignored) {
                    }
                });
            }
        }
    }
}
