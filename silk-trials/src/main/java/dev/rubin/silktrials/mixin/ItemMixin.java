package dev.rubin.silktrials.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rubin.silktrials.ItemNames;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public class ItemMixin {
	@ModifyReturnValue(method = "getName", at = @At("RETURN"))
	private Component silkTrials$nameAfterContents(final Component name, final ItemStack itemStack) {
		return ItemNames.withContents(itemStack, name);
	}
}
