package bingodiscordlogger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Behavioural tests for the core matching logic in
 * {@link BingoDiscordLoggerPlugin#onLootReceived(LootReceived)}:
 * PvP loot is dropped, an empty bingo set is a no-op, only items whose
 * {@code canonicalize}d ID is in the set are forwarded, and the captured
 * {@link WebhookBody} carries the right structured data.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BingoLootMatchingTest
{
	@Mock
	private Client client;

	@Mock
	private BingoDiscordLoggerConfig config;

	@Mock
	private ItemManager itemManager;

	@Mock
	private BingoItemList bingoItemList;

	@Mock
	private DiscordWebhookClient webhookClient;

	@Mock
	private Player localPlayer;

	@Captor
	private ArgumentCaptor<WebhookBody> bodyCaptor;

	@InjectMocks
	private BingoDiscordLoggerPlugin plugin;

	@Before
	public void setUp()
	{
		lenient().when(client.getLocalPlayer()).thenReturn(localPlayer);
		lenient().when(localPlayer.getName()).thenReturn("Zezima");
		// By default item IDs are already canonical (identity); overridden where noted.
		lenient().when(itemManager.canonicalize(anyInt())).thenAnswer(inv -> inv.getArgument(0));
		lenient().when(config.sendScreenshot()).thenReturn(false);
	}

	private static LootReceived loot(LootRecordType type, ItemStack... items)
	{
		Collection<ItemStack> stacks = Arrays.asList(items);
		return new LootReceived("Abyssal demon", 124, type, stacks, items.length, "meta");
	}

	private void stubBingoSet(Integer... ids)
	{
		Set<Integer> set = ids.length == 0
				? Collections.emptySet()
				: new HashSet<>(Arrays.asList(ids));
		when(bingoItemList.getItemIds()).thenReturn(set);
	}

	private void stubItemName(int id, String name)
	{
		ItemComposition comp = org.mockito.Mockito.mock(ItemComposition.class);
		lenient().when(comp.getName()).thenReturn(name);
		when(itemManager.getItemComposition(id)).thenReturn(comp);
	}

	@Test
	public void playerKillLootIsIgnored()
	{
		plugin.onLootReceived(loot(LootRecordType.PLAYER, new ItemStack(4151, 1)));

		verifyNoInteractions(webhookClient);
	}

	@Test
	public void emptyBingoSetIsANoOp()
	{
		stubBingoSet(); // empty

		plugin.onLootReceived(loot(LootRecordType.NPC, new ItemStack(4151, 1)));

		verifyNoInteractions(webhookClient);
	}

	@Test
	public void noMatchingItemsDoesNotSend()
	{
		stubBingoSet(4151);

		// Only a non-bingo item dropped.
		plugin.onLootReceived(loot(LootRecordType.NPC, new ItemStack(995, 100)));

		verifyNoInteractions(webhookClient);
	}

	@Test
	public void matchingItemBuildsAndSendsPayload()
	{
		stubBingoSet(4151);
		stubItemName(4151, "Abyssal whip");

		// Whip matches; coins do not.
		plugin.onLootReceived(loot(LootRecordType.NPC,
				new ItemStack(4151, 2), new ItemStack(995, 100)));

		verify(webhookClient).send(bodyCaptor.capture());
		WebhookBody body = bodyCaptor.getValue();

		WebhookBody.Payload bingo = body.getBingo();
		assertEquals("Zezima", bingo.getPlayer());
		assertEquals("Abyssal demon", bingo.getSource());
		assertEquals("NPC", bingo.getSourceType());
		assertEquals(124, bingo.getCombatLevel());

		assertEquals("only the matching item is included", 1, bingo.getItems().size());
		WebhookBody.Payload.Item item = bingo.getItems().get(0);
		assertEquals(4151, item.getId());
		assertEquals("Abyssal whip", item.getName());
		assertEquals(2, item.getQuantity());

		assertEquals(1, body.getEmbeds().size());
		WebhookBody.Embed embed = body.getEmbeds().get(0);
		assertEquals("Bingo Loot", embed.getTitle());
		assertTrue(embed.getDescription().contains("**2 x Abyssal whip**"));
		assertEquals("Zezima", embed.getAuthor().getName());
		// Screenshot disabled -> no attachment image referenced.
		assertNull(embed.getImage());
	}

	@Test
	public void notedVariantIsCanonicalizedBeforeMatching()
	{
		stubBingoSet(4151);
		stubItemName(4151, "Abyssal whip");
		// 4152 is the noted form; canonicalize resolves it to the un-noted 4151.
		when(itemManager.canonicalize(4152)).thenReturn(4151);

		plugin.onLootReceived(loot(LootRecordType.NPC, new ItemStack(4152, 1)));

		verify(webhookClient).send(bodyCaptor.capture());
		WebhookBody.Payload.Item item = bodyCaptor.getValue().getBingo().getItems().get(0);
		assertEquals(4151, item.getId());
		assertEquals("Abyssal whip", item.getName());
	}

	@Test
	public void screenshotEnabledReferencesAttachmentImage()
	{
		when(config.sendScreenshot()).thenReturn(true);
		stubBingoSet(4151);
		stubItemName(4151, "Abyssal whip");

		plugin.onLootReceived(loot(LootRecordType.NPC, new ItemStack(4151, 1)));

		verify(webhookClient).send(bodyCaptor.capture());
		WebhookBody.Image image = bodyCaptor.getValue().getEmbeds().get(0).getImage();
		assertEquals("attachment://image.png", image.getUrl());
	}
}
