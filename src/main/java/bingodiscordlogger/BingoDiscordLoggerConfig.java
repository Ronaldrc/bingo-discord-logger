package bingodiscordlogger;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(BingoDiscordLoggerConfig.GROUP)
public interface BingoDiscordLoggerConfig extends Config
{
    String GROUP = "bingodiscordlogger";

	@ConfigItem(
		keyName = "bingoDiscordLogger",
		name = "Discord Webhook URL",
		description = "Enter the discord webhook URL you would to send content to."
	)
    String webhook();

    @ConfigSection(
        name = "Bingo Event",
        description = "Which items are part of the current bingo",
        position = 1
    )
    String bingoSection = "bingoSection";

    @ConfigItem(
            keyName = "sendScreenshot",
            name = "Send Screenshot",
            description = "Attach a screenshot once bingo drop is detected"
    )
    default boolean sendScreenshot()
    {
        return true;
    }

    @ConfigItem(
        keyName = "bingoListUrl",
        name = "Bingo List URL",
        description = "Link to a published Google Sheet (CSV) listing the bingo item IDs. "
            + "Everyone who uses the same link shares one centrally-managed list. "
            + "Leave blank to disable.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0,
        section = bingoSection
    )
    default String bingoListUrl()
    {
        return "";
    }
}
