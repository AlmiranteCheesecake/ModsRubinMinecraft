package dev.rubin.silktrials.test;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockState;

public class SilkTrialsGameTest {
	private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
	private static final BlockPos POS = FLOOR.above();

	private static final String BREEZE = "minecraft:trial_chamber/breeze/normal";
	private static final String BREEZE_SPAWNER =
		"{normal_config:'" + BREEZE + "',ominous_config:'minecraft:trial_chamber/breeze/ominous',"
			+ "cooldown_ends_at:999999999L,total_mobs_spawned:4,registered_players:[[I;1,2,3,4]]}";
	private static final String OMINOUS_LOOT = "minecraft:chests/trial_chambers/reward_ominous";
	private static final String OMINOUS_VAULT =
		"{config:{loot_table:'" + OMINOUS_LOOT + "',key_item:{id:'minecraft:ominous_trial_key',count:1}},"
			+ "server_data:{rewarded_players:[[I;1,2,3,4]]}}";

	@GameTest
	public void spawnerKeepsMobAndStartsFresh(final GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		placeWithData(helper, Blocks.TRIAL_SPAWNER.defaultBlockState(), BREEZE_SPAWNER);
		helper.assertTrue(BREEZE.equals(savedData(helper).getStringOr("normal_config", "")), "test setup: spawner should start with the breeze config");

		ItemStack drop = mine(helper, player, silkTouchPickaxe(helper));
		helper.assertTrue(drop.is(Items.TRIAL_SPAWNER), "silk touch should drop a trial spawner, got " + drop);

		place(helper, player, drop);
		helper.assertBlockPresent(Blocks.TRIAL_SPAWNER, POS);
		CompoundTag placed = savedData(helper);
		helper.assertTrue(BREEZE.equals(placed.getStringOr("normal_config", "")), "placed spawner lost its mob: " + placed);
		helper.assertTrue(placed.getLongOr("cooldown_ends_at", 0L) == 0L, "placed spawner kept its cooldown: " + placed);
		helper.assertTrue(placed.getIntOr("total_mobs_spawned", 0) == 0, "placed spawner kept its spawn count: " + placed);
		helper.assertTrue(placed.getListOrEmpty("registered_players").isEmpty(), "placed spawner kept its players: " + placed);

		// A spawner with a mob leaves "inactive" by itself; one without a mob never does.
		helper.succeedWhen(() -> helper.assertTrue(
			helper.getBlockState(POS).getValue(TrialSpawnerBlock.STATE) != TrialSpawnerState.INACTIVE, "placed spawner never activated"
		));
	}

	@GameTest
	public void spawnerWithoutSilkTouchDropsNothing(final GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		placeWithData(helper, Blocks.TRIAL_SPAWNER.defaultBlockState(), BREEZE_SPAWNER);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		player.gameMode.destroyBlock(helper.absolutePos(POS));

		helper.assertBlockPresent(Blocks.AIR, POS);
		helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(), "a plain pickaxe should drop nothing");
		helper.succeed();
	}

	// Control for the test above: without this mod's marker, survival players still can't place spawner data.
	@GameTest
	public void foreignSpawnerItemStillNeedsOp(final GameTestHelper helper) throws CommandSyntaxException {
		ServerPlayer player = survivalPlayer(helper);
		helper.setBlock(FLOOR, Blocks.STONE);
		ItemStack forged = new ItemStack(Items.TRIAL_SPAWNER);
		forged.set(
			DataComponents.BLOCK_ENTITY_DATA,
			TypedEntityData.<BlockEntityType<?>>of(BlockEntityTypes.TRIAL_SPAWNER, TagParser.parseCompoundFully(BREEZE_SPAWNER))
		);

		place(helper, player, forged);
		helper.assertBlockPresent(Blocks.TRIAL_SPAWNER, POS);
		CompoundTag placed = savedData(helper);
		helper.assertTrue(!BREEZE.equals(placed.getStringOr("normal_config", "")), "an item not dropped by the mod bypassed the op check: " + placed);
		helper.succeed();
	}

	@GameTest
	public void ominousVaultKeepsLootAndForgetsPlayers(final GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		placeWithData(helper, Blocks.VAULT.defaultBlockState().setValue(VaultBlock.OMINOUS, true), OMINOUS_VAULT);
		helper.assertTrue(
			!savedData(helper).getCompoundOrEmpty("server_data").getListOrEmpty("rewarded_players").isEmpty(),
			"test setup: vault should start with a rewarded player"
		);

		ItemStack drop = mine(helper, player, silkTouchPickaxe(helper));
		helper.assertTrue(drop.is(Items.VAULT), "silk touch should drop a vault, got " + drop);

		place(helper, player, drop);
		helper.assertBlockPresent(Blocks.VAULT, POS);
		helper.assertBlockProperty(POS, VaultBlock.OMINOUS, true);
		CompoundTag placed = savedData(helper);
		CompoundTag config = placed.getCompoundOrEmpty("config");
		helper.assertTrue(OMINOUS_LOOT.equals(config.getStringOr("loot_table", "")), "placed vault lost its loot table: " + placed);
		helper.assertTrue(
			"minecraft:ominous_trial_key".equals(config.getCompoundOrEmpty("key_item").getStringOr("id", "")), "placed vault lost its key item: " + placed
		);
		helper.assertTrue(
			placed.getCompoundOrEmpty("server_data").getListOrEmpty("rewarded_players").isEmpty(), "placed vault still remembers who opened it: " + placed
		);
		helper.succeed();
	}

	@GameTest
	public void trialBlocksMineFasterWithPickaxe(final GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		// Mining is 5x slower in mid-air, where the mock player spawns.
		player.setOnGround(true);
		BlockPos pos = helper.absolutePos(POS);
		for (BlockState state : List.of(Blocks.TRIAL_SPAWNER.defaultBlockState(), Blocks.VAULT.defaultBlockState())) {
			helper.setBlock(POS, state);

			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			int byHand = Math.round(1.0F / state.getDestroyProgress(player, helper.getLevel(), pos));
			helper.assertTrue(byHand == 480, state.getBlock() + " should take 480 ticks by hand, takes " + byHand);

			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
			int withPickaxe = Math.round(1.0F / state.getDestroyProgress(player, helper.getLevel(), pos));
			helper.assertTrue(withPickaxe == 60, state.getBlock() + " should take 60 ticks with a diamond pickaxe, takes " + withPickaxe);
		}

		float obsidian = Blocks.OBSIDIAN.defaultBlockState().getDestroySpeed(helper.getLevel(), pos);
		helper.assertTrue(obsidian == 50.0F, "other blocks must keep their hardness, obsidian has " + obsidian);
		helper.succeed();
	}

	private static void placeWithData(final GameTestHelper helper, final BlockState state, final String snbt) {
		helper.setBlock(FLOOR, Blocks.STONE);
		helper.setBlock(POS, state);
		BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(POS));
		try {
			TypedEntityData.<BlockEntityType<?>>of(blockEntity.getType(), TagParser.parseCompoundFully(snbt))
				.loadInto(blockEntity, helper.getLevel().registryAccess());
		} catch (CommandSyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private static CompoundTag savedData(final GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		return level.getBlockEntity(helper.absolutePos(POS)).saveCustomOnly(level.registryAccess());
	}

	private static ItemStack mine(final GameTestHelper helper, final ServerPlayer player, final ItemStack tool) {
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(helper.absolutePos(POS));
		helper.assertBlockPresent(Blocks.AIR, POS);

		List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM);
		helper.assertTrue(drops.size() == 1, "expected exactly one dropped item, got " + drops.size());
		ItemStack drop = drops.getFirst().getItem().copy();
		drops.getFirst().discard();
		return drop;
	}

	// placeAt reads the item's data from the player's hand, not from the stack it is given.
	private static void place(final GameTestHelper helper, final ServerPlayer player, final ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		helper.placeAt(player, stack, FLOOR, Direction.UP);
	}

	private static ItemStack silkTouchPickaxe(final GameTestHelper helper) {
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		return pickaxe;
	}

	// GameTestHelper's own mock player is always creative, which never gets block drops.
	private static ServerPlayer survivalPlayer(final GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "silk-trials-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		return player;
	}
}
