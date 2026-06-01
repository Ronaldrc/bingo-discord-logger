package bingodiscordlogger;

import com.google.gson.JsonObject;
import org.junit.Test;

import static net.runelite.http.api.RuneLiteAPI.GSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Locks the dual-purpose {@link WebhookBody} JSON contract: a single {@code payload_json}
 * carries both Discord's rendered {@code embeds} and the custom top-level {@code bingo}
 * machine-readable payload. Serialized with {@code RuneLiteAPI.GSON}, exactly as the
 * webhook client does. Field names here must stay in sync with what Discord / the
 * downstream bot expect.
 */
public class WebhookBodyTest
{
	private static WebhookBody sampleBody()
	{
		WebhookBody.Payload.Item item = new WebhookBody.Payload.Item();
		item.setId(4151);
		item.setName("Abyssal whip");
		item.setQuantity(2);

		WebhookBody.Payload payload = new WebhookBody.Payload();
		payload.setPlayer("Zezima");
		payload.setSource("Abyssal demon");
		payload.setSourceType("NPC");
		payload.setCombatLevel(124);
		payload.setTimestamp(1_700_000_000_000L);
		payload.getItems().add(item);

		WebhookBody.Author author = new WebhookBody.Author();
		author.setName("Zezima");
		author.setIconUrl("https://example.com/avatar.png");

		WebhookBody.Field field = new WebhookBody.Field();
		field.setValue("From: Abyssal demon");
		field.setInline(true);

		WebhookBody.Image image = new WebhookBody.Image();
		image.setUrl("attachment://image.png");

		WebhookBody.Embed embed = new WebhookBody.Embed();
		embed.setColor(0x57F287);
		embed.setTitle("Bingo Loot");
		embed.setDescription("**2 x Abyssal whip**");
		embed.setAuthor(author);
		embed.setImage(image);
		embed.getFields().add(field);
		embed.setTimestamp("2023-11-14T22:13:20Z");

		WebhookBody body = new WebhookBody();
		body.getEmbeds().add(embed);
		body.setBingo(payload);
		return body;
	}

	@Test
	public void serializesTopLevelBingoPayloadForTheBot()
	{
		JsonObject json = GSON.toJsonTree(sampleBody()).getAsJsonObject();

		assertTrue("custom 'bingo' field must ride along at top level", json.has("bingo"));

		JsonObject bingo = json.getAsJsonObject("bingo");
		assertEquals("Zezima", bingo.get("player").getAsString());
		assertEquals("Abyssal demon", bingo.get("source").getAsString());
		assertEquals("NPC", bingo.get("sourceType").getAsString());
		assertEquals(124, bingo.get("combatLevel").getAsInt());
		assertEquals(1_700_000_000_000L, bingo.get("timestamp").getAsLong());

		assertEquals(1, bingo.getAsJsonArray("items").size());
		JsonObject item = bingo.getAsJsonArray("items").get(0).getAsJsonObject();
		assertEquals(4151, item.get("id").getAsInt());
		assertEquals("Abyssal whip", item.get("name").getAsString());
		assertEquals(2, item.get("quantity").getAsInt());
	}

	@Test
	public void serializesEmbedsForDiscord()
	{
		JsonObject json = GSON.toJsonTree(sampleBody()).getAsJsonObject();

		assertEquals(1, json.getAsJsonArray("embeds").size());
		JsonObject embed = json.getAsJsonArray("embeds").get(0).getAsJsonObject();
		assertEquals("Bingo Loot", embed.get("title").getAsString());
		assertEquals("**2 x Abyssal whip**", embed.get("description").getAsString());
		assertEquals(0x57F287, embed.get("color").getAsInt());
		assertEquals("2023-11-14T22:13:20Z", embed.get("timestamp").getAsString());
	}

	@Test
	public void usesDiscordsSnakeCaseIconUrlFieldName()
	{
		JsonObject json = GSON.toJsonTree(sampleBody()).getAsJsonObject();
		JsonObject author = json.getAsJsonArray("embeds").get(0).getAsJsonObject()
				.getAsJsonObject("author");

		assertEquals("Zezima", author.get("name").getAsString());
		// @SerializedName("icon_url") — Discord rejects the camelCase form.
		assertTrue(author.has("icon_url"));
		assertFalse(author.has("iconUrl"));
		assertEquals("https://example.com/avatar.png", author.get("icon_url").getAsString());
	}

	@Test
	public void serializesInlineFieldAndAttachmentImage()
	{
		JsonObject embed = GSON.toJsonTree(sampleBody()).getAsJsonObject()
				.getAsJsonArray("embeds").get(0).getAsJsonObject();

		JsonObject field = embed.getAsJsonArray("fields").get(0).getAsJsonObject();
		assertEquals("From: Abyssal demon", field.get("value").getAsString());
		assertTrue(field.get("inline").getAsBoolean());

		assertEquals("attachment://image.png",
				embed.getAsJsonObject("image").get("url").getAsString());
	}
}
