package dev.rubin.silktrials.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rubin.silktrials.SilkTrials;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Shadow
	public abstract Block getBlock();

	@ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
	private float silkTrials$mineFaster(final float destroyTime) {
		return SilkTrials.isTrialBlock(this.getBlock()) ? SilkTrials.DESTROY_TIME : destroyTime;
	}
}
