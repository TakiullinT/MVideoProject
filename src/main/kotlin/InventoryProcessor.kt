import java.util.TreeMap
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class InventoryProcessor {
    private val inventory = TreeMap<String, TreeMap<String, Long>>()

    fun process(inputCsv: List<String>): List<String> {
        for ((index, line) in inputCsv.withIndex()) {
            if (line.isBlank()) continue
            val parts = line.trim().split("\\s*;\\s*".toRegex())
            val lineNumber = index + 1

            when (parts.size) {
                3 -> handleReceipt(parts, lineNumber)
                2 -> handleSale(parts, lineNumber)
                else -> logger.warning("Строка $lineNumber: Неверный формат операции -> $line")
            }
        }

        return generateOutput()
    }

    private fun handleReceipt(parts: List<String>, lineNumber : Int) {
        val groupId = parts[0]
        val itemId = parts[1]
        val quantity = parts[2].toLongOrNull()

        if (quantity == null || quantity <= 0) {
            logger.warning("Строка $lineNumber: Некорректное количество поступления.")
            return
        }

        val groupItems = inventory.getOrPut(groupId) { TreeMap() }
        var remainingQuantity = quantity
        val unknownDeficit = groupItems.getOrDefault("UNKNOWN_ITEM", 0L)

        if (unknownDeficit < 0) {
            val deficit = kotlin.math.abs(unknownDeficit)
            val coverAmount = minOf(deficit, remainingQuantity)
            val newUnknownBalance = unknownDeficit + coverAmount

            if (newUnknownBalance == 0L) {
                groupItems.remove("UNKNOWN_ITEM")
            } else {
                groupItems["UNKNOWN_ITEM"] = newUnknownBalance
            }

            remainingQuantity -= coverAmount
            logger.info("Строка $lineNumber: Погашен долг UNKNOWN_ITEM на $coverAmount шт. Остаток к зачислению: $remainingQuantity")
        }

        if (remainingQuantity > 0) {
            val currentQuantity = groupItems.getOrDefault(itemId, 0L)
            groupItems[itemId] = currentQuantity + remainingQuantity
        }

        logger.fine("Строка $lineNumber: Поступление $quantity штук товара $itemId (Группа: $groupId)")
    }

    private fun handleSale(parts: List<String>, lineNumber : Int) {
        val groupId = parts[0]
        val quantityToSell = parts[1].toLongOrNull()

        if (quantityToSell == null || quantityToSell <= 0) {
            logger.warning("Строка $lineNumber: Некорректное количество продажи. Пропуск.")
            return
        }

        logger.fine("Строка $lineNumber: Продажа $quantityToSell шт. (Группа: $groupId)")
        val groupItems = inventory.getOrPut(groupId) { TreeMap() }
        var remainingToSell = quantityToSell

        for ((itemId, currentQuantity) in groupItems.toList()) {
            if (remainingToSell == 0L) break
            if (currentQuantity > 0) {
                val sellAmount = minOf(currentQuantity, remainingToSell)
                groupItems[itemId] = currentQuantity - sellAmount
                remainingToSell -= sellAmount
            }
        }

        if (remainingToSell > 0) {
            val deficitItemId = if (groupItems.isNotEmpty()) groupItems.firstKey() else "UNKNOWN_ITEM"
            val currentQuantity = groupItems.getOrDefault(deficitItemId, 0L)

            groupItems[deficitItemId] = currentQuantity - remainingToSell
            logger.info("Строка $lineNumber: Дефицит $remainingToSell шт. в группе $groupId. Списано в минус с $deficitItemId")
        }
    }

    private fun generateOutput(): List<String> {
        val output = mutableListOf<String>()
        for ((groupId, items) in inventory) {
            for ((itemId, quantity) in items) {
                output.add("$groupId;$itemId;$quantity")
            }
        }

        return output
    }

    companion object {
        private val logger = Logger.getLogger(InventoryProcessor::class.java.name).apply {
            level  = Level.ALL
            useParentHandlers = false
            val handler = ConsoleHandler().apply {
                level = Level.ALL
                formatter = SimpleFormatter()
            }
            addHandler(handler)
        }
    }
}