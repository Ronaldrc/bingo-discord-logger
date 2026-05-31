package bingodiscordlogger;

import java.time.Instant;
import com.google.common.base.Strings;
import com.google.inject.Provides;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "Bingo Discord Logger",
        description = "Sends bingo-relevant drops to a Discord webhook",
        tags = {"bingo", "clan", "loot", "webhook", "discord"}
)
public class BingoDiscordLoggerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private BingoDiscordLoggerConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private BingoItemList bingoItemList;

    @Inject
    private DiscordWebhookClient webhookClient;

    @Override
    protected void startUp() throws Exception
    {
        log.debug("BingoDiscordLogger started!");
        bingoItemList.start();
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug("BingoDiscordLogger stopped!");
        bingoItemList.stop();
    }

    @Provides
    BingoDiscordLoggerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BingoDiscordLoggerConfig.class);
    }

    private String getPlayerName()
    {
        return client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "Unknown";
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!BingoDiscordLoggerConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }
        switch (event.getKey())
        {
            case "bingoItemIds":
                bingoItemList.reloadManualItemIds();
                break;
            case "bingoListUrl":
                // URL changed: reschedule (an empty URL stops polling) and refetch now.
                bingoItemList.scheduleRemoteRefresh();
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onLootReceived(LootReceived lootReceived)
    {
        // If item obtained via Player Kill, return
        LootRecordType type = lootReceived.getType();
        if (type == LootRecordType.PLAYER)
        {
            return;
        }

        // No bingo items configured - nothing to match against
        Set<Integer> currentIds = bingoItemList.getItemIds();
        if (currentIds.isEmpty())
        {
            return;
        }

        // Send valid bingo loot to webhook
        log.debug("Detected onLootReceived! - onLootReceived sub was reached");
        log.debug("Inside onLootReceived! - Object metadata \n{}", lootReceived.getMetadata().toString());

        processLoot(lootReceived, currentIds);
    }

    private void processLoot(LootReceived lootReceived, Set<Integer> currentIds)
    {
        log.debug("Detected processLoot! - processLoot was reached");

        // Build the structured payload for the bot
        WebhookBody.Payload payload = new WebhookBody.Payload();
        payload.setEventId(config.eventId());
        payload.setPlayer(getPlayerName());
        payload.setSource(lootReceived.getName());
        payload.setSourceType(lootReceived.getType().name());
        payload.setCombatLevel(lootReceived.getCombatLevel());
        payload.setTimestamp(System.currentTimeMillis());

        // Build a description listing each matched bingo item
        StringBuilder dropList = new StringBuilder();
        for (ItemStack stack : lootReceived.getItems())
        {
            int canonicalId = itemManager.canonicalize(stack.getId());
            if (!currentIds.contains(canonicalId))
            {
                continue;
            }

            ItemComposition comp = itemManager.getItemComposition(canonicalId);

            WebhookBody.Payload.Item item = new WebhookBody.Payload.Item();
            item.setId(canonicalId);
            item.setName(comp.getName());
            item.setQuantity(stack.getQuantity());
            payload.getItems().add(item);

            if (dropList.length() > 0)
            {
                dropList.append("\n");
            }
            // Bold the item name so it stands out in the embed body
            dropList.append("**").append(stack.getQuantity()).append(" x ")
                    .append(comp.getName()).append("**");
        }

        if (payload.getItems().isEmpty())
        {
            return;
        }

        // Build the rich embed
        WebhookBody.Embed embed = new WebhookBody.Embed();
        // 0x57F287 is Discord's "online green" - good visibility in light and dark mode.
        // Try 0x5865F2 for Discord blurple, 0xFEE75C for yellow, 0xED4245 for red.
        embed.setColor(0x57F287);
        embed.setTitle("Bingo Drop");
        embed.setDescription(dropList.toString());

        // Author block: player name in the top-left
        WebhookBody.Author author = new WebhookBody.Author();
        author.setName(payload.getPlayer());
        embed.setAuthor(author);

        // Two side-by-side fields below the description
        WebhookBody.Field sourceField = new WebhookBody.Field();
        sourceField.setName("Source");
        sourceField.setValue(payload.getSource());
        sourceField.setInline(true);
        embed.getFields().add(sourceField);

        WebhookBody.Field typeField = new WebhookBody.Field();
        typeField.setName("Type");
        typeField.setValue(prettySourceType(payload.getSourceType()));
        typeField.setInline(true);
        embed.getFields().add(typeField);

        // Embed the screenshot inside the card if we're sending one.
        // "attachment://image.png" tells Discord to look at the file we uploaded
        // in the same multipart POST under the field name matching this filename.
        if (config.sendScreenshot())
        {
            WebhookBody.Image image = new WebhookBody.Image();
            image.setUrl("attachment://image.png");
            embed.setImage(image);
        }

        // Footer with the event ID, useful for distinguishing concurrent bingos
        if (!Strings.isNullOrEmpty(payload.getEventId()))
        {
            WebhookBody.Footer footer = new WebhookBody.Footer();
            footer.setText("Event: " + payload.getEventId());
            embed.setFooter(footer);
        }

        // ISO 8601 in UTC. Discord renders this in each viewer's local timezone.
        // Example: "2026-05-28T19:43:27.000Z" -> "Today at 3:43 PM" for EST viewers.
        embed.setTimestamp(Instant.ofEpochMilli(payload.getTimestamp()).toString());

        WebhookBody body = new WebhookBody();
        body.getEmbeds().add(embed);
        body.setBingo(payload);

        webhookClient.send(body);
    }

    /**
     * Convert the raw LootRecordType name (e.g. "NPC", "PICKPOCKET") into something
     * a human would want to read in the embed.
     */
    private static String prettySourceType(String type)
    {
        if (type == null) return "Unknown";
        switch (type)
        {
            case "NPC":        return "NPC Kill";
            case "EVENT":      return "Chest / Event";
            case "PICKPOCKET": return "Pickpocket";
            case "PLAYER":     return "PvP";
            default:           return "Unknown";
        }
    }
}
