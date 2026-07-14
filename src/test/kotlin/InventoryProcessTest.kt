import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class InventoryProcessTest {
    private val processor = InventoryProcessor()

    @Test
    fun `test simple receipt`() {
        val input  = listOf("G1;ItemA;10")
        val output = processor.process(input)
        assertEquals(listOf("G1;ItemA;10"), output)
    }

    @Test
    fun `test sequential sale with rank priority`() {
        val input = listOf(
            "G1;ItemB;5",
            "G1;ItemA;5",
            "G1;7"
        )

        val output = processor.process(input)
        assertEquals(listOf("G1;ItemA;0", "G1;ItemB;3"), output)
    }

    @Test
    fun `test deficit sale when not enough total stock`() {
        val input = listOf(
            "G1;ItemB;2",
            "G1;ItemA;2",
            "G1;6",
        )
        val output = processor.process(input)
        assertEquals(listOf("G1;ItemA;-2", "G1;ItemB;0"), output)
    }

    @Test
    fun `test combined operations across multiple groups`() {
        val input = listOf(
            "Group1;Item1;10",
            "Group2;ItemX;5",
            "Group1;4",
            "Group1;Item2;5",
            "Group1;7"
        )
        val output = processor.process(input)

        val expected = listOf(
            "Group1;Item1;0",
            "Group1;Item2;4",
            "Group2;ItemX;5",
        )
        assertEquals(expected, output)
    }

    @Test
    fun `should correctly resolve UNKNOWN_ITEM deficit when real item arrives`() {
        val inputLines = listOf(
            "G1;5",
            "G1;ItemA;10"
        )
        val expectedLines = listOf(
            "G1;ItemA;5"
        )

        val processor = InventoryProcessor()
        val actualLines = processor.process(inputLines)

        assertEquals(expectedLines, actualLines)
    }

    @Test
    fun `ignore blank and malformed lines and non-numeric quantities`() {
        val input = listOf(
            "G1;ItemA;10",
            "",
            "G1;ItemB;xyz",
            "BAD;LINE;TOO;MANY",
            "G1;ItemB;5",
            "G1;abc"
        )

        val output = processor.process(input)
        val expected = listOf("G1;ItemA;10", "G1;ItemB;5")
        assertEquals(expected, output)
    }

    @Test
    fun `zero and negative quantities are ignored`() {
        val input = listOf(
            "G1;ItemA;0",
            "G1;ItemA;-5",
            "G1;ItemA;10",
            "G1;0",
            "G1;-3"
        )

        val output = processor.process(input)
        val expected = listOf("G1;ItemA;10")
        assertEquals(expected, output)
    }

    @Test
    fun `sale on empty group creates UNKNOWN_ITEM negative balance`() {
        val input = listOf("G1;5")
        val output = processor.process(input)
        val expected = listOf("G1;UNKNOWN_ITEM;-5")
        assertEquals(expected, output)
    }

    @Test
    fun `partial cover of UNKNOWN_ITEM by later receipt`() {
        val input = listOf(
            "G1;7",
            "G1;ItemA;3"
        )

        val output = processor.process(input)
        val expected = listOf("G1;UNKNOWN_ITEM;-4")
        assertEquals(expected, output)
    }

    @Test
    fun `output is sorted by group and item names (TreeMap ordering)`() {
        val input = listOf(
            "B;z;1",
            "A;a;2"
        )

        val output = processor.process(input)
        val expected = listOf("A;a;2", "B;z;1")
        assertEquals(expected, output)
    }
}