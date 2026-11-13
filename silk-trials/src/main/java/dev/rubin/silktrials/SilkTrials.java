package dev.rubin.silktrials;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

public final class SilkTrials {
	/** custom_data key marking an item as dropped by this mod. */
	public static final String MARKER = "silk_trials";

	// Progress of the current fight. Left out so a placed spawner starts fresh instead of stuck in cooldown.
	private static final List<String> SPAWNER_RUNTIME_KEYS = List.of(
		"registered_players", "current_mobs", "cooldown_ends_at", "next_mob_spawns_at", "total_mobs_spawned", "ejecting_loot_table"
	);
	// Who already looted the vault and what it is ejecting. Only "config" (loot table and key item) is kept.
	private static final List<String> VAULT_RUNTIME_KEYS = List.of("server_data", "shared_data");

	private SilkTrials() {
	}

	public static void dropIfSilkTouched(
		final ServerLevel level, final BlockPos pos, final BlockState state, final @Nullable BlockEntity blockEntity, final ItemStack destroyedWith
	) {
		List<String> runtimeKeys;
		if (blockEntity instanceof TrialSpawnerBlockEntity) {
			runtimeKeys = SPAWNER_RUNTIME_KEYS;
		} else if (blockEntity instanceof VaultBlockEntity) {
			runtimeKeys = VAULT_RUNTIME_KEYS;
		} else {
			return;
		}

		Holder<Enchantment> silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		if (EnchantmentHelper.getItemEnchantmentLevel(silkTouch, destroyedWith) <= 0) {
			return;
		}

		CompoundTag tag = blockEntity.saveCustomOnly(level.registryAccess());
		runtimeKeys.forEach(tag::remove);

		ItemStack drop = new ItemStack(state.getBlock());
		drop.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.<BlockEntityType<?>>of(blockEntity.getType(), tag));
		if (state.getOptionalValue(BlockStateProperties.OMINOUS).orElse(false)) {
			drop.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(BlockStateProperties.OMINOUS, true));
		}
		CustomData.update(DataComponents.CUSTOM_DATA, drop, customData -> customData.putBoolean(MARKER, true));

		Block.popResource(level, pos, drop);
	}

	/**
	 * Trial spawner data on items is normally applied only for operators. Items dropped by this mod are
	 * exempt, and only for trial spawners, so the marker can't be used to place other op-only data.
	 */
	public static boolean isSilkTouchedSpawner(final ItemStack stack) {
		TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data == null || data.type() != BlockEntityTypes.TRIAL_SPAWNER) {
			return false;
		}

		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(MARKER);
	}
}
