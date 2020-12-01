package ru.falseresync.banTrigger.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import ru.falseresync.banTrigger.PlayerBlockBreakEvents;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow public ServerWorld world;
    @Shadow public ServerPlayerEntity player;

    @Inject(
        method = "tryBreakBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir, BlockState state, BlockEntity entity, Block block) {
        boolean result = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(this.world, this.player, pos, state, entity);

        if (!result) {
            PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(this.world, this.player, pos, state, entity);

            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "tryBreakBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onBlockBroken(BlockPos pos, CallbackInfoReturnable<Boolean> cir, BlockState state, BlockEntity entity, Block block, boolean b1) {
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(this.world, this.player, pos, state, entity);
    }
}
