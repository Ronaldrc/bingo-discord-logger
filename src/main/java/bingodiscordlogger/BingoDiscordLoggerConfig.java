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
            keyName = "sendScreenshot",
            name = "Send Screenshot",
            description = "Attach a screenshot once bingo drop is detected",
            position = 1
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
        position = 3
    )
    default String bingoListUrl()
    {
        return "";
    }

    @ConfigItem(
            keyName = "bingoDiscordLogger",
            name = "Discord Webhook URL",
            description = "Enter the discord webhook URL you would to send content to.",
            position = 2
    )
    String webhook();
}
