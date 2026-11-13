package dev.rubin.silktrials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rubin.silktrials.SilkTrials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public class BlockItemMixin {
	@ModifyExpressionValue(
		method = "updateCustomBlockEntityTag",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityType;onlyOpCanSetNbt()Z")
	)
	private static boolean silkTrials$allowSilkTouchedSpawner(final boolean onlyOp, @Local(argsOnly = true) final ItemStack itemStack) {
		return onlyOp && !SilkTrials.isSilkTouchedSpawner(itemStack);
	}
}
