package bingodiscordlogger;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BingoDiscordLogger.GROUP)
public interface BingoDiscordLogger extends Config
{
    String GROUP = "bingodiscordlogger";

	@ConfigItem(
		keyName = "Bingo - Discord Logger",
		name = "Discord Webhook URL",
		description = "Enter the discord webhook URL you would to send content to."
	)
    String webhook();

    @ConfigItem(
            keyName = "sendScreenshot",
            name = "Send Screenshot",
            description = "Includes a screenshot when receiving the loot"
    )
    default boolean sendScreenshot()
    {
        return false;
    }

}
