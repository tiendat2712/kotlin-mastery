package view

/**
 * =====================================================================
 * LESSON 04 - EX03: TRẢ VỀ NHIỀU GIÁ TRỊ (`Pair`, `Triple`, `data class` & DESTRUCTURING)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java, một phương thức chỉ trả về được 1 giá trị duy nhất. Muốn trả về 2 giá trị (ví dụ: phần nguyên
 *      và phần dư của phép chia), lập trình viên Java phải trả về mảng `int[]` (mất an toàn kiểu) hoặc tạo class phụ.
 *    - Kotlin cung cấp sẵn 2 lớp dữ liệu tổng quát (Generic) trong thư viện chuẩn:
 *      + **`Pair<A, B>`**: Lưu 2 giá trị (`first` và `second`). Khởi tạo nhanh bằng hàm infix `a to b`.
 *      + **`Triple<A, B, C>`**: Lưu 3 giá trị (`first`, `second`, `third`).
 *    - **Destructuring Declarations (`val (a, b) = ...`)**:
 *      + Khi bạn viết `val (quotient, remainder) = divide(17, 5)`, trình biên dịch Kotlin dịch ra Bytecode thành:
 *        ```java
 *        Pair<Integer, Integer> tmp = divide(17, 5);
 *        int quotient = tmp.component1().intValue();
 *        int remainder = tmp.component2().intValue();
 *        ```
 *      + Cơ chế này dựa trên các hàm `operator fun component1()`, `component2()`... được Kotlin tự động sinh ra
 *        cho `Pair`, `Triple`, và mọi **`data class`**!
 *
 * 2. KHI NÀO DÙNG `Pair`/`Triple` VÀ KHI NÀO PHẢI DÙNG `data class`?
 *    - `Pair` / `Triple` rất tiện cho các phép biến đổi cục bộ ngắn hạn (như trong pipeline `.map { ... }`).
 *    - **NHƯNG trong Public API / tầng `@Service` của Production**:
 *      Hãy luôn ưu tiên tạo một **`data class`** có tên rõ nghĩa (ví dụ: `data class StatsResult(val min: Int, val max: Int, val avg: Double)`)!
 *      Lý do:
 *      1) Đọc `result.min` và `result.avg` dễ hiểu hơn gấp vạn lần so với `result.first` và `result.third`!
 *      2) Với `Int`, `Pair<Int, Int>` phải **Box** thành `Integer` trên Heap (vì `Pair<A, B>` dùng Generic),
 *         trong khi `data class` có thuộc tính `val min: Int` lưu thẳng kiểu nguyên thủy `int` tốc độ cao!
 * =====================================================================
 */

// Best Practice cho Production Code: Dùng data class có tên trường rõ nghĩa thay vì Triple<Int, Int, Double>
private data class OrderStatsResult(
    val minOrder: Int,
    val maxOrder: Int,
    val avgOrder: Double
)

fun main() {
    println("--- ___ * START: Ex03ReturnMultipleValues * ___ ---\n")

    // =================================================================
    // 1. Trả về 2 giá trị bằng `Pair<A, B>` và giải nén (Destructuring)
    // =================================================================
    println("1. [Pair & Destructuring] Trả về 2 giá trị từ hàm và bóc tách trực tiếp:")
    val divisionPair: Pair<Int, Int> = divideWithRemainder(dividend = 17, divisor = 5)
    println("   -> Truy cập qua thuộc tính Pair: first=${divisionPair.first}, second=${divisionPair.second}")

    // Giải nén (Destructuring) ngay lập tức ra 2 biến có tên rõ ràng:
    val (quotient, remainder) = divideWithRemainder(dividend = 17, divisor = 5)
    println("   -> Giải nén Destructuring: 17 chia 5 được thương = $quotient, dư = $remainder\n")

    // =================================================================
    // 2. Trả về 3 giá trị bằng `Triple<A, B, C>` (và dấu `_` bỏ qua thành phần không cần dùng)
    // =================================================================
    println("2. [Triple & Underscore `_`] Trả về 3 giá trị và bỏ qua biến không dùng bằng dấu `_`:")
    val (host, port, protocol) = parseEndpoint("https://api.kotlinjourney.vn:8443")
    println("   -> Bóc tách đủ 3 phần: protocol=$protocol | host=$host | port=$port")

    // Nếu chỉ cần lấy `host` và `port`, dùng dấu gạch dưới `_` ở vị trí thứ 3 để trình biên dịch không gọi `component3()`!
    val (onlyHost, onlyPort, _) = parseEndpoint("http://localhost:8080")
    println("   -> Bỏ qua phần tử thứ 3 bằng `_`: onlyHost=$onlyHost, onlyPort=$onlyPort\n")

    // =================================================================
    // 3. [Production Best Practice] Dùng `data class` thay cho `Triple`
    // =================================================================
    println("3. [Production Best Practice] Tại sao `data class` vượt trội hơn `Triple` ở tầng Nghiệp vụ:")
    val orderValues = listOf(150_000, 300_000, 450_000, 900_000)
    val stats: OrderStatsResult = calculateOrderStats(orderValues)

    // Vừa có thể truy cập bằng tên thuộc tính tường minh (.minOrder, .maxOrder, .avgOrder)...
    println("   -> Truy cập theo tên thuộc tính: min=${stats.minOrder}, max=${stats.maxOrder}, avg=${stats.avgOrder}")

    // ...lại vừa hỗ trợ Destructuring giống hệt Triple nhờ `data class` tự sinh hàm component1(), component2(), component3()!
    val (minVal, maxVal, avgVal) = stats
    println("   -> Giải nén từ data class:       min=$minVal, max=$maxVal, avg=$avgVal")

    println("\n--- ___ * END: Ex03ReturnMultipleValues * ___ ---")
}

// Trả về Pair bằng toán tử infix `to`
private fun divideWithRemainder(dividend: Int, divisor: Int): Pair<Int, Int> {
    val quotient = dividend / divisor
    val remainder = dividend % divisor
    return quotient to remainder // Tương đương Pair(quotient, remainder)
}

// Trả về Triple
private fun parseEndpoint(url: String): Triple<String, Int, String> {
    val protocol = url.substringBefore("://")
    val hostAndPort = url.substringAfter("://")
    val host = hostAndPort.substringBefore(":")
    val port = hostAndPort.substringAfter(":", missingDelimiterValue = "80").toInt()
    return Triple(host, port, protocol)
}

// Trả về custom data class (Không bị Boxing kiểu nguyên thủy int/double!)
private fun calculateOrderStats(values: List<Int>): OrderStatsResult {
    return OrderStatsResult(
        minOrder = values.minOrNull() ?: 0,
        maxOrder = values.maxOrNull() ?: 0,
        avgOrder = values.average()
    )
}
