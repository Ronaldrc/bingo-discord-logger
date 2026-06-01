package bingodiscordlogger;

import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link Utils#parseCsv(String)} — the published-Google-Sheet
 * CSV parser that turns sheet rows into the set of bingo item IDs.
 */
public class UtilsTest
{
	@Test
	public void parsesIdFromEachRowFirstColumn()
	{
		Set<Integer> ids = Utils.parseCsv("995,Coins\n526,Bones\n4151,Abyssal whip");

		assertEquals(3, ids.size());
		assertThat(ids, hasItems(995, 526, 4151));
	}

	@Test
	public void parsesBareIdsWithoutOtherColumns()
	{
		Set<Integer> ids = Utils.parseCsv("995\n526\n4151");

		assertEquals(3, ids.size());
		assertThat(ids, hasItems(995, 526, 4151));
	}

	@Test
	public void skipsHeaderRowAndNonNumericRows()
	{
		// First column is not an integer: header, a comment, and a blank-ish row.
		Set<Integer> ids = Utils.parseCsv("Item ID,Name\n995,Coins\n# comment\n,emptyfirstcol");

		assertEquals(1, ids.size());
		assertThat(ids, hasItems(995));
	}

	@Test
	public void trimsWhitespaceAroundIds()
	{
		Set<Integer> ids = Utils.parseCsv("  995  ,Coins\n\t526\t,Bones");

		assertEquals(2, ids.size());
		assertThat(ids, hasItems(995, 526));
	}

	@Test
	public void handlesCrlfLineEndings()
	{
		Set<Integer> ids = Utils.parseCsv("995,Coins\r\n526,Bones\r\n");

		assertEquals(2, ids.size());
		assertThat(ids, hasItems(995, 526));
	}

	@Test
	public void omitsBlankLines()
	{
		Set<Integer> ids = Utils.parseCsv("995\n\n\n526\n");

		assertEquals(2, ids.size());
		assertThat(ids, hasItems(995, 526));
	}

	@Test
	public void collapsesDuplicateIds()
	{
		Set<Integer> ids = Utils.parseCsv("995,Coins\n995,Coins again\n995");

		assertEquals(1, ids.size());
		assertThat(ids, hasItems(995));
	}

	@Test
	public void returnsEmptySetForEmptyInput()
	{
		assertTrue(Utils.parseCsv("").isEmpty());
		assertTrue(Utils.parseCsv("\n\n").isEmpty());
	}

	@Test
	public void skipsNonNumericFirstColumnButKeepsNumericRows()
	{
		Set<Integer> ids = Utils.parseCsv("abc,Coins\n995,Coins\nxyz");

		assertEquals(1, ids.size());
		assertThat(ids, hasItems(995));
	}

    @Test
    public void skipsNonNumericFirstRowButKeepsNumericRows()
    {
        Set<Integer> ids = Utils.parseCsv("id,items\nCoins\n995,Coins\nxyz");

        assertEquals(1, ids.size());
        assertThat(ids, hasItems(995));
    }
}
