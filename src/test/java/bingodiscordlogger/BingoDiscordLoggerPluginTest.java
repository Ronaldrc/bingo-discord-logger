package bingodiscordlogger;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BingoDiscordLoggerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BingoDiscordLoggerPlugin.class);
		RuneLite.main(args);
	}
}