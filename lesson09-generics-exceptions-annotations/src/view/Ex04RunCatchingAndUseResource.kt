package view

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets

/**
 * BÀI 04: FUNCTIONAL ERROR HANDLING (`runCatching` / `Result<T>`) & RESOURCE MANAGEMENT (`.use`)
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - **`Result<T>` và `runCatching { ... }`**:
 *      + `Result<T>` trong Kotlin được khai báo là `@JvmInline value class Result<out T>(val value: Any?)`.
 *        Nhờ là `value class` (Inline Class), ở tầng Bytecode, khi thành công nó KHÔNG cấp phát thêm
 *        một đối tượng wrapper `Result` trên Heap mà lưu trực tiếp giá trị `T`! Chỉ khi thất bại nó mới
 *        bọc `Throwable` vào một đối tượng nội bộ `Result.Failure`.
 *      + `runCatching { block() }` thực chất là một `inline fun` bọc `try { Result.success(block()) } catch (e: Throwable) { Result.failure(e) }`.
 *      + Nhờ đó, bạn có thể xử lý lỗi theo phong cách **Functional Pipeline** (`.map`, `.recover`,
 *        `.onSuccess`, `.onFailure`, `.getOrDefault`, `.getOrElse`, `.fold`) thay vì lồng nhiều khối `try-catch`.
 *
 *    - **Quản lý tài nguyên tự động với `.use { ... }` (Thay thế `try-with-resources` của Java)**:
 *      + Trong Java (`D:\Java_Journey\lesson13-files`), bạn dùng cú pháp đặc biệt của ngôn ngữ:
 *        `try (BufferedReader br = new BufferedReader(...)) { ... }`
 *      + Kotlin KHÔNG CẦN thêm cú pháp riêng vào ngôn ngữ! Thay vào đó, Kotlin cung cấp hàm mở rộng
 *        `inline fun <T : Closeable?, R> T.use(block: (T) -> R): R` (và tương tự cho `AutoCloseable`).
 *      + Khi biên dịch ra Bytecode, `.use { ... }` được inline thành khối `try - catch - finally` chuẩn mực,
 *        tự động gọi `close()` và gọi cả `firstException.addSuppressed(closeException)` nếu cả thân hàm
 *        lẫn lúc đóng tài nguyên đều ném ngoại lệ — giống hệt 100% bytecode của Java `try-with-resources`!
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java (`lesson13-files`):
 *      ```java
 *      try (var reader = new BufferedReader(new StringReader(csv))) {
 *          return reader.lines().count();
 *      }
 *      ```
 *    - Kotlin:
 *      ```kotlin
 *      BufferedReader(StringReader(csv)).use { reader ->
 *          reader.lineSequence().count()
 *      }
 *      ```
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Khi gọi các dịch vụ bên ngoài không ổn định (Payment Gateway, Webhook, Redis Cache) trong Spring Boot,
 *      `runCatching { externalClient.fetchRate(currency) }.getOrElse { fallbackRate }` giúp code mạch lạc,
 *      dễ đọc hơn hẳn `try-catch` truyền thống.
 *    - Lưu ý nhỏ khi dùng `runCatching` trong **Kotlin Coroutines**: Nếu dùng bên trong `suspend fun`,
 *      cần tránh nuốt mất `CancellationException` (hoặc dùng `.onFailure { if (it is CancellationException) throw it }`).
 */

data class ExchangeRateQuote(val pair: String, val rate: Double)

// Hàm giả lập gọi API tỷ giá ngoại tệ có thể thất bại
fun fetchExchangeRateFromRemote(pair: String): ExchangeRateQuote {
    require(pair.contains("/")) { "Cặp tiền tệ không đúng định dạng XXX/YYY: '$pair'" }
    if (pair == "USD/VND") return ExchangeRateQuote(pair, 25_450.0)
    if (pair == "JPY/VND") return ExchangeRateQuote(pair, 168.5)
    throw IllegalStateException("Máy chủ tỷ giá đang bảo trì cho cặp tiền: $pair")
}

fun main() {
    println("--- ___ * START EX04: RUNCATCHING & USE RESOURCE * ___ ---\n")

    // =========================================================================
    // 1. FUNCTIONAL ERROR HANDLING VỚI `runCatching` VÀ `Result<T>`
    // =========================================================================
    println("1. Xử lý lỗi dạng chuỗi (Pipeline) với `runCatching` & `Result<T>`:")

    // Tình huống A: Thành công -> biến đổi kết quả bằng `.map` -> lấy giá trị
    val amountInVndResult: Result<Double> = runCatching {
        fetchExchangeRateFromRemote("USD/VND")
    }.map { quote ->
        // Chỉ chạy khi thành công!
        100.0 * quote.rate
    }.onSuccess { totalVnd ->
        println("  - [onSuccess] Quy đổi 100 USD thành công: $totalVnd VNĐ")
    }.onFailure { ex ->
        println("  - [onFailure] Lỗi: ${ex.message}")
    }
    println("  - Giá trị lấy từ Result (getOrDefault): ${amountInVndResult.getOrDefault(0.0)}")

    // Tình huống B: Thất bại khi gọi API -> dùng `getOrElse` để cung cấp giá trị dự phòng (Fallback)
    val eurRate: Double = runCatching {
        fetchExchangeRateFromRemote("EUR/VND").rate
    }.onFailure { ex ->
        println("  - [Cảnh báo Cache/API] Không lấy được EUR/VND (${ex.message}) -> Dùng tỷ giá dự phòng!")
    }.getOrElse {
        27_500.0 // Giá trị fallback khi có lỗi
    }
    println("  - Tỷ giá EUR/VND sau khi fallback: $eurRate")

    // Tình huống C: Dùng `.fold(onSuccess = ..., onFailure = ...)` để quy về chuỗi thông báo cho UI/API
    val apiMessage: String = runCatching {
        fetchExchangeRateFromRemote("INVALID_PAIR")
    }.fold(
        onSuccess = { quote -> "Thành công: 1 ${quote.pair} = ${quote.rate}" },
        onFailure = { ex -> "Thất bại (${ex::class.simpleName}): ${ex.message}" }
    )
    println("  - Kết quả `.fold(...)`: $apiMessage")

    // =========================================================================
    // 2. QUẢN LÝ TÀI NGUYÊN TỰ ĐỘNG VỚI `.use { ... }` (THAY THẾ TRY-WITH-RESOURCES)
    // =========================================================================
    println("\n2. Quản lý tài nguyên tự động với `.use { ... }` (AutoCloseable / Closeable):")

    val rawCsvData = """
        # Danh sách đơn hàng xuất kho
        O001,504000
        O002,997500
        
        O003,300000
    """.trimIndent()

    // BufferedReader sẽ TỰ ĐỘNG được gọi `.close()` ngay khi thoát khỏi khối `.use { ... }`
    // kể cả khi có return sớm hay ném ngoại lệ bên trong!
    val validOrderSummary: List<String> = BufferedReader(StringReader(rawCsvData)).use { reader ->
        // Lưu ý: `reader.lineSequence()` là lazy Sequence, nên PHẢI tiêu thụ hết (toList/sumOf)
        // BÊN TRONG khối `.use` trước khi reader bị đóng!
        reader.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                val parts = line.split(",")
                "Đơn ${parts[0]} -> ${parts[1]} VNĐ"
            }
            .toList()
    }
    println("  - Đọc dữ liệu bằng BufferedReader.use { ... }: $validOrderSummary")

    // Minh họa kết hợp `.use` với ByteArrayOutputStream để ghi dữ liệu nhị phân trong bộ nhớ
    val exportedBytes: ByteArray = ByteArrayOutputStream().use { outputStream ->
        val header = "REPORT_HEADER: NOVA_SHOP_2026\n"
        outputStream.write(header.toByteArray(StandardCharsets.UTF_8))
        validOrderSummary.forEach { line ->
            outputStream.write("$line\n".toByteArray(StandardCharsets.UTF_8))
        }
        outputStream.toByteArray()
    }
    println("  - Đã xuất ra ByteArrayOutputStream.use (${exportedBytes.size} bytes):")
    println(String(exportedBytes, StandardCharsets.UTF_8).prependIndent("    | "))

    println("\n--- ___ * END EX04 * ___ ---")
}
