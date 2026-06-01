package bingodiscordlogger;

import com.google.common.base.Splitter;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared, stateless helpers for the bingo plugin. Kept separate from the classes that
 * own threading/IO so the pure logic here can be exercised directly in unit tests.
 */
final class Utils
{
	private Utils()
	{
	}

	/**
	 * Parse a published-Google-Sheet CSV listing the bingo item IDs. Each row's first
	 * column is the item ID; any row whose first column isn't an integer (header, blank,
	 * comment) is skipped.
	 */
	static Set<Integer> parseCsv(String csv)
	{
		Set<Integer> ids = new HashSet<>();
		for (String line : Splitter.on('\n').omitEmptyStrings().trimResults().split(csv))
		{
			int comma = line.indexOf(',');
			String first = (comma >= 0 ? line.substring(0, comma) : line).trim();
			try
			{
				ids.add(Integer.parseInt(first));
			}
			catch (NumberFormatException e)
			{
				// header / comment / blank row - ignore
			}
		}
		return ids;
	}
}
