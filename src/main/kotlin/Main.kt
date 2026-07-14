import java.io.File

fun main(args : Array<String>) {
    if (args.size < 2) {
        System.err.println("Ошибка: Неверное количество аргументов.")
        System.err.println("Использование: ./gradlew run --args=\"input.csv output.csv\"")
        return
    }

    val input = File(args[0])
    val output = File(args[1])

    if (!input.exists()) {
        System.err.println("Ошибка: Файл ${input.absolutePath} не найден!")
        return
    }

    val processor = InventoryProcessor()
    val lines = input.readLines()
    val result = processor.process(lines)

    output.writeText(result.joinToString("\n"))
    println("Обработка завершена. Результат сохранен в ${output.absolutePath}")
}