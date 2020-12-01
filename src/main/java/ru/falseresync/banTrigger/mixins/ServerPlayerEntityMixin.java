package ru.falseresync.banTrigger.mixins;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.falseresync.banTrigger.BanTrigger;

import java.util.OptionalInt;

import static ru.falseresync.banTrigger.BanTrigger.CONTAINER_OPENS;
import static ru.falseresync.banTrigger.BanTrigger.EXECUTOR_SERVICE;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(
        method = "openHandledScreen(Lnet/minecraft/screen/NamedScreenHandlerFactory;)Ljava/util/OptionalInt;",
        at = @At(value = "RETURN", ordinal = 2)
    )
    private void onOpenHandledScreen(NamedScreenHandlerFactory factory, CallbackInfoReturnable<OptionalInt> cir) {
        if (factory instanceof BlockEntity) {
            BlockEntity be = (BlockEntity) factory;
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            BlockPos pos = be.getPos();
            EXECUTOR_SERVICE.submit(() -> {
                try {
                    CONTAINER_OPENS.put(new BanTrigger.LogEntry(pos.getX(), pos.getY(), pos.getZ(), player.getUuid().toString()));
                } catch (InterruptedException ignored) {
                }
            });
        }
    }
}
