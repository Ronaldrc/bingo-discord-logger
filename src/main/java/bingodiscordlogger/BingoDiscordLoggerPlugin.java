package bingodiscordlogger;

import java.time.Instant;
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
        if ("bingoListUrl".equals(event.getKey()))
        {
            // URL changed: reschedule (an empty URL stops polling) and refetch now.
            bingoItemList.scheduleRemoteRefresh();
        }
    }

    @Subscribe
    public void onLootReceived(LootReceived lootReceived)
    {
        // If item obtained via Player Kill, exit
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
        processLoot(lootReceived, currentIds);
    }


    /**
     * Formats loot data into an embedding then sends it via webhook to Discord.
     *
     * @param lootReceived      The loot data that will be processed
     * @param currentIds        The list of bingo-relevant item IDs
     */
    private void processLoot(LootReceived lootReceived, Set<Integer> currentIds)
    {
        // Build the structured payload for the bot
        WebhookBody.Payload payload = new WebhookBody.Payload();
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
            dropList.append(stack.getQuantity()).append(" x ")
                    .append(comp.getName());
        }

        if (payload.getItems().isEmpty())
        {
            log.debug("step payload getItems is empty");
            return;
        }

        // Create Discord embedding
        WebhookBody.Embed embed = new WebhookBody.Embed();
        embed.setColor(0x57F287);
        embed.setTitle("Bingo Loot");
        String description = payload.getPlayer() + " has looted:\n\n" + dropList + "\nFrom: " + payload.getSource();
        embed.setDescription(description);

        WebhookBody.Author author = new WebhookBody.Author();
        author.setName(payload.getPlayer());
        embed.setAuthor(author);

        if (config.sendScreenshot())
        {
            WebhookBody.Image image = new WebhookBody.Image();
            image.setUrl("attachment://image.png");
            embed.setImage(image);
        }

        // ISO 8601 in UTC. Discord renders this in each viewer's local timezone.
        // Example: "2026-05-28T19:43:27.000Z" -> "Today at 3:43 PM" for EST viewers.
        embed.setTimestamp(Instant.ofEpochMilli(payload.getTimestamp()).toString());

        WebhookBody body = new WebhookBody();
        body.getEmbeds().add(embed);
        body.setBingo(payload);

        webhookClient.send(body);
    }
}
