package view

import bean.SampleRepository
import bean.Trader
import bean.Transaction

/**
 * ============================================================================
 * BÀI 05: GIẢI TRỌN VẸN 10 CÂU HỎI `Ex01TransactionApp.java` BẰNG KOTLIN PIPELINES!
 * ============================================================================
 *
 * Hãy đặt file này cạnh `D:\Java_Journey\lesson11-colection\src\view\exercise\Ex01TransactionApp.java`
 * (dài 145 dòng với hàng chục vòng lặp `for`, `if`, biến tạm `ArrayList`, `HashSet`, `TreeSet`):
 *
 * BẢNG SO SÁNH TRỰC DIỆN JAVA (`Ex01TransactionApp.java`) vs KOTLIN:
 * ----------------------------------------------------------------------------
 * - Mỗi câu hỏi trong Java mất từ **8 đến 15 dòng code** (khởi tạo list tạm, lặp `for`,
 *   kiểm tra `if`, gọi `.sort(comparing(...))`).
 * - Trong Kotlin, mỗi câu hỏi chỉ còn đúng **1 ĐẾN 2 DÒNG CODE** đọc xuôi tự nhiên như
 *   một câu truy vấn SQL / tiếng Anh, hoàn toàn bất biến (Immutable) và an toàn Null!
 */

private fun <E> generate(prefix: String, collection: Collection<E>) {
    println("$prefix --> ")
    for (e in collection) {
        println("  + $e")
    }
    println()
}

private fun generate(prefix: String, value: Any?) {
    println("$prefix --> $value\n")
}

fun main() {
    val transactions: List<Transaction> = SampleRepository.getTransactions()

    println("--- ___ * START: Ex05Exercise_TransactionApp * ___ ---\n")

    // ========================================================================
    // 1. Find all transactions in the year 2011 and sort them by value (small to high).
    // [Java: 10 dòng tạo ArrayList + vòng for + .sort(comparing(Transaction::getValue))]
    // ========================================================================
    val transactionsIn2011 = transactions
        .filter { it.year == 2011 }
        .sortedBy { it.value }
    generate("1. Find all transactions in the year 2011 and value ASC", transactionsIn2011)

    // ========================================================================
    // 2. Find all transactions have value greater than 300 and sort them by trader’s city.
    // [Java: 10 dòng tạo ArrayList + vòng for + .sort(comparing(t -> t.getTrader().getCity()))]
    // ========================================================================
    val transactionsGreater300 = transactions
        .filter { it.value > 300 }
        .sortedBy { it.trader.city }
    generate("2. Find all transactions have value > 300 sorted by trader's city", transactionsGreater300)

    // ========================================================================
    // 3. What are all the unique cities where the traders work?
    // [Java: 6 dòng tạo HashSet + vòng for add từng city]
    // ========================================================================
    val uniqueCities: Set<String> = transactions
        .map { it.trader.city }
        .toSet()
    generate("3. Unique cities where traders work", uniqueCities)

    // ========================================================================
    // 4. Find all traders from Cambridge and sort them by name desc.
    // [Java: 9 dòng tạo ArrayList + vòng for + .sort(comparing(Trader::getName, reverseOrder()))]
    // Lưu ý: Thêm `.distinct()` để không bị lặp lại cùng 1 Trader nếu họ có nhiều giao dịch!
    // ========================================================================
    val tradersInCambridge: List<Trader> = transactions
        .map { it.trader }
        .filter { it.city == "Cambridge" }
        .distinct()
        .sortedByDescending { it.name }
    generate("4. Traders in Cambridge sorted by name (DESC)", tradersInCambridge)

    // ========================================================================
    // 5. Return a string of all traders’ names sorted alphabetically.
    // [Java: 7 dòng tạo TreeSet + vòng for + String.join(", ", traderNames)]
    // ========================================================================
    val sortedNamesStr: String = transactions
        .map { it.trader.name }
        .distinct()
        .sorted()
        .joinToString(", ")
    generate("5. All traders' names sorted alphabetically", sortedNamesStr)

    // ========================================================================
    // 6. Are any traders based in Milan?
    // [Java: 9 dòng biến cờ boolean anyInMilan = false + vòng for + if + break]
    // ========================================================================
    val anyInMilan: Boolean = transactions.any { it.trader.city == "Milan" }
    generate("6. Are any traders based in Milan?", anyInMilan)

    // ========================================================================
    // 7. Count the number of unique traders in Milan.
    // [Java: 8 dòng tạo HashSet<Trader> + vòng for + milanTraders.size()]
    // ========================================================================
    val milanTraderCount: Int = transactions
        .map { it.trader }
        .distinct()
        .count { it.city == "Milan" }
    generate("7. Number of traders in Milan", milanTraderCount)

    // ========================================================================
    // 8. Print all transactions’ values from the traders living in Cambridge.
    // [Java: 8 dòng tạo ArrayList<Integer> + vòng for + if]
    // ========================================================================
    val cambridgeValues: List<Int> = transactions
        .filter { it.trader.city == "Cambridge" }
        .map { it.value }
    generate("8. Transactions' values from traders living in Cambridge", cambridgeValues)

    // ========================================================================
    // 9. What’s the highest value of all the transactions?
    // [Java: 11 dòng gán Integer.MIN_VALUE + kiểm tra !isEmpty() + vòng for so sánh]
    // ========================================================================
    val highestValue: Int? = transactions.maxOfOrNull { it.value }
    generate("9. Highest value of all transactions", highestValue)

    // ========================================================================
    // 10. Find the transaction with the smallest value.
    // [Java: 11 dòng gán smallestTransaction = null + vòng for so sánh]
    // ========================================================================
    val smallestTransaction: Transaction? = transactions.minByOrNull { it.value }
    generate("10. Transaction with the smallest value", smallestTransaction)

    println("--- ___ * FINISH: Ex05Exercise_TransactionApp * ___ ---")
}
