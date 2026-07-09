package brozzerlogger;

import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(BrozzerLoggerConfig.GROUP)
public interface BrozzerLoggerConfig extends Config
{
    String GROUP = "brozzerLogger";

    @Getter
    public enum TeamColor
    {
        RED(0xE6194B),
        ORANGE(0xF58231),
        YELLOW(0xFFE119),
        GREEN(0x3CB44B),
        BLUE(0x4363D8),
        PURPLE(0x911EB4),
        GRAY(0x808080);

        private final int rgb;

        TeamColor(int rgb) { this.rgb = rgb; }

    }

    @ConfigSection(
        name = "Discord/Bingo URLs",
        description = "URLs for discord webhook and bingo item id list",
        position = 0
    )
    String urls = "urls";

    @ConfigItem(
        keyName = "discordWebhookUrl",
        name = "Discord Webhook URL",
        description = "Enter the discord webhook URL to send content to.",
        section = urls
    )
    String webhook();

    @ConfigItem(
        keyName = "bingoListUrl",
        name = "Bingo List URL",
        description = "Link to a published Google Sheet (CSV) listing the bingo item IDs. "
            + "Everyone who uses the same link shares one centrally-managed list. "
            + "Leave blank to disable.",
        section = urls
    )
    default String bingoListUrl()
    {
        return "";
    }

    @ConfigItem(
        keyName = "name",
        name = "Team Name",
        description = "Team name for your team's Discord embeds. Defaults to 'Bingo Loot' if blank.",
        position = 1
    )
    default String teamName()
    {
        return "Bingo Loot";
    }

    @ConfigItem(
        keyName = "color",
        name = "Team Color",
        description = "Color used for your team's Discord embeds. Defaults to gray if unset.",
        position = 2
    )
    default TeamColor teamColor()
    {
        return TeamColor.GRAY;
    }

    @ConfigItem(
        keyName = "sendScreenshot",
        name = "Send Screenshot",
        description = "Attach a screenshot once bingo drop is detected",
        position = 3
    )
    default boolean sendScreenshot()
    {
        return true;
    }
}
