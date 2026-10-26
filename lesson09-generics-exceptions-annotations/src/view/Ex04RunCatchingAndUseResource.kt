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
 *      dễ đọc hơn `try-catch` truyền thống.
 *    - Lưu ý quan trọng khi dùng `runCatching` trong **Kotlin Coroutines**: Vì `runCatching` bắt mọi `Throwable`
 *      (bao gồm cả `CancellationException`), nếu dùng bên trong `suspend fun`, bạn phải ném lại
 *      `CancellationException` (`.onFailure { if (it is CancellationException) throw it }`) để không phá vỡ
 *      cơ chế hủy Coroutine!
 */

data class ExchangeRateQuote(val pair: String, val rate: Double)

// Hàm giả lập gọi API tỷ giá ngoại tệ có thể thất bại
fun fetchExchangeRateFromRemote(pair: String): ExchangeRateQuote {
    require(pair.contains("/")) { "Cap tien te khong dung dinh dang XXX/YYY: '$pair'" }
    if (pair == "USD/VND") return ExchangeRateQuote(pair, 25_450.0)
    if (pair == "JPY/VND") return ExchangeRateQuote(pair, 168.5)
    throw IllegalStateException("May chu ty gia dang bao tri cho cap tien: $pair")
}

fun main() {
    println("--- ___ * START EX04: RUNCATCHING & USE RESOURCE * ___ ---\n")

    // =========================================================================
    // 1. FUNCTIONAL ERROR HANDLING VỚI `runCatching` VÀ `Result<T>`
    // =========================================================================
    println("1. Xu ly loi dang chuoi (Pipeline) voi `runCatching` & `Result<T>`:")

    // Tình huống A: Thành công -> biến đổi kết quả bằng `.map` -> lấy giá trị
    val amountInVndResult: Result<Double> = runCatching {
        fetchExchangeRateFromRemote("USD/VND")
    }.map { quote ->
        // Chỉ chạy khi thành công!
        100.0 * quote.rate
    }.onSuccess { totalVnd ->
        println("  - [onSuccess] Quy doi 100 USD thanh cong: $totalVnd VND")
    }.onFailure { ex ->
        println("  - [onFailure] Loi: ${ex.message}")
    }
    println("  - Gia tri lay tu Result (getOrDefault): ${amountInVndResult.getOrDefault(0.0)}")
    check(amountInVndResult.getOrNull() == 2_545_000.0)

    // Tình huống B: Thất bại khi gọi API -> dùng `getOrElse` để cung cấp giá trị dự phòng (Fallback)
    val eurRate: Double = runCatching {
        fetchExchangeRateFromRemote("EUR/VND").rate
    }.onFailure { ex ->
        println("  - [Canh bao Cache/API] Khong lay duoc EUR/VND (${ex.message}) -> Dung ty gia du phong!")
    }.getOrElse {
        27_500.0 // Giá trị fallback khi có lỗi
    }
    println("  - Ty gia EUR/VND sau khi fallback: $eurRate")
    check(eurRate == 27_500.0)

    // Tình huống C: Dùng `.fold(onSuccess = ..., onFailure = ...)` để quy về chuỗi thông báo cho UI/API
    val apiMessage: String = runCatching {
        fetchExchangeRateFromRemote("INVALID_PAIR")
    }.fold(
        onSuccess = { quote -> "Thanh cong: 1 ${quote.pair} = ${quote.rate}" },
        onFailure = { ex -> "That bai (${ex::class.simpleName}): ${ex.message}" }
    )
    println("  - Ket qua `.fold(...)`: $apiMessage")

    // =========================================================================
    // 2. QUẢN LÝ TÀI NGUYÊN TỰ ĐỘNG VỚI `.use { ... }` (THAY THẾ TRY-WITH-RESOURCES)
    // =========================================================================
    println("\n2. Quan ly tai nguyen tu dong voi `.use { ... }` (AutoCloseable / Closeable):")

    val rawCsvData = """
        # Danh sach don hang xuat kho
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
                "Don ${parts[0]} -> ${parts[1]} VND"
            }
            .toList()
    }
    println("  - Doc du lieu bang BufferedReader.use { ... }: $validOrderSummary")
    check(validOrderSummary.size == 3)

    // Minh họa kết hợp `.use` với ByteArrayOutputStream để ghi dữ liệu nhị phân trong bộ nhớ
    val exportedBytes: ByteArray = ByteArrayOutputStream().use { outputStream ->
        val header = "REPORT_HEADER: NOVA_SHOP_2026\n"
        outputStream.write(header.toByteArray(StandardCharsets.UTF_8))
        validOrderSummary.forEach { line ->
            outputStream.write("$line\n".toByteArray(StandardCharsets.UTF_8))
        }
        outputStream.toByteArray()
    }
    println("  - Da xuat ra ByteArrayOutputStream.use (${exportedBytes.size} bytes):")
    println(String(exportedBytes, StandardCharsets.UTF_8).prependIndent("    | "))

    println("\n--- ___ * END EX04 * ___ ---")
}
