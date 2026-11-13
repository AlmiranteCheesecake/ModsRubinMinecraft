package dev.rubin.silktrials;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

/**
 * Names trial spawner and vault items after what they hold, e.g. "Trial Spawner (Breeze)" and
 * "Vault (Ominous Trial Key)". The name is read from the item's data each time, so it also covers
 * items dropped by earlier versions of the mod.
 */
public final class ItemNames {
	private ItemNames() {
	}

	public static Component withContents(final ItemStack stack, final Component name) {
		boolean vault = stack.is(Items.VAULT);
		if (!vault && !stack.is(Items.TRIAL_SPAWNER)) {
			return name;
		}

		TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data == null) {
			return name;
		}

		CompoundTag tag = data.copyTagWithoutId();
		Component contents = vault ? vaultKey(tag) : spawnerMob(tag);
		return contents == null ? name : Component.empty().append(name).append(" (").append(contents).append(")");
	}

	private static @Nullable Component spawnerMob(final CompoundTag tag) {
		// The mob the spawner had picked to spawn next; present on any spawner that has ticked.
		Component mob = entityName(tag.getCompoundOrEmpty("spawn_data"));
		if (mob != null) {
			return mob;
		}

		// Config written inline in the block instead of pointing at a registry entry.
		ListTag potentials = tag.getCompoundOrEmpty("normal_config").getListOrEmpty("spawn_potentials");
		if (!potentials.isEmpty() && potentials.get(0) instanceof CompoundTag first) {
			mob = entityName(first.getCompoundOrEmpty("data"));
			if (mob != null) {
				return mob;
			}
		}

		// Only the registry id is left, e.g. "minecraft:trial_chamber/breeze/normal".
		Identifier config = Identifier.tryParse(tag.getStringOr("normal_config", ""));
		return config == null || config.getPath().isEmpty() ? null : Component.literal(config.getPath());
	}

	private static @Nullable Component entityName(final CompoundTag spawnData) {
		Identifier id = Identifier.tryParse(spawnData.getCompoundOrEmpty("entity").getStringOr("id", ""));
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			return null;
		}

		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
		return type.getDescription();
	}

	private static @Nullable Component vaultKey(final CompoundTag tag) {
		Identifier id = Identifier.tryParse(tag.getCompoundOrEmpty("config").getCompoundOrEmpty("key_item").getStringOr("id", ""));
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			return null;
		}

		return new ItemStack(BuiltInRegistries.ITEM.getValue(id)).getItemName();
	}
}
