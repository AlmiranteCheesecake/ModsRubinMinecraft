package dev.rubin.silktrials.mixin;

import dev.rubin.silktrials.SilkTrials;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {
	@Inject(at = @At("HEAD"), method = "playerDestroy")
	private void silkTrials$dropWithData(
		final ServerLevel level,
		final ServerPlayer player,
		final BlockPos pos,
		final BlockState state,
		final @Nullable BlockEntity blockEntity,
		final ItemStack destroyedWith,
		final CallbackInfo info
	) {
		SilkTrials.dropIfSilkTouched(level, pos, state, blockEntity, destroyedWith);
	}
}
