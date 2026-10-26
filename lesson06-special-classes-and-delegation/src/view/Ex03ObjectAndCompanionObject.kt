package view

/**
 * ============================================================================
 * BÀI 03: `object` DECLARATION, OBJECT EXPRESSION & `companion object`
 * ============================================================================
 *
 * Từ khóa `object` trong Kotlin đóng 3 vai trò hoàn toàn khác nhau nhưng đều xoay quanh
 * việc **"vừa định nghĩa cấu trúc, vừa khởi tạo ngay 1 thực thể (instance) duy nhất"**:
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * A. `object` Declaration (Thread-safe Singleton Pattern có sẵn):
 *    Khi bạn viết `object AppConfig { ... }`, trình biên dịch Kotlin sinh ra:
 *      ```java
 *      public final class AppConfig {
 *          public static final AppConfig INSTANCE = new AppConfig();
 *          private AppConfig() {}
 *          ...
 *      }
 *      ```
 *    Nhờ cơ chế nạp lớp (ClassLoader initialization lock) của JVM, `INSTANCE` được
 *    khởi tạo **Lazy** (khi được truy cập lần đầu) và **Đảm bảo 100% Thread-Safe**
 *    mà bạn không cần viết Double-Checked Locking phức tạp như trong Java!
 *
 * B. Object Expression (`object : InterfaceOrClass { ... }`):
 *    Thay thế cho **Anonymous Inner Class** (`new Comparator<String>() { ... }`) của Java.
 *    Điểm mạnh hơn Java: Một Object Expression trong Kotlin có thể kế thừa/thực thi
 *    **nhiều interface cùng lúc** (`object : Runnable, AutoCloseable { ... }`).
 *
 * C. `companion object` (Người bạn đồng hành thay thế `static` của Java):
 *    Kotlin không có từ khóa `static`. Các hằng số và Factory Method gắn với class
 *    được đặt trong `companion object`.
 *    - `const val MAX_RETRY = 3`: Hằng số biên dịch (Compile-time constant, chỉ áp dụng cho
 *      kiểu nguyên thủy và `String`), được inline thẳng giá trị vào nơi gọi (`public static final`).
 *    - Hàm Factory `fun of(...)`: Giúp tạo đối tượng có tên hàm rõ nghĩa và kiểm tra hợp lệ
 *      trước khi gọi `private constructor`.
 *
 * 2. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Làm sao để khai báo Logger (`SLF4J`) chuẩn trong Spring Boot + Kotlin khi không có `static`?
 * Có 2 cách phổ biến nhất trong các dự án thực tế:
 *   - Cách 1 (Đặt trong `companion object` - khởi tạo 1 lần cho cả class):
 *     ```kotlin
 *     companion object {
 *         private val log = LoggerFactory.getLogger(OrderService::class.java)
 *     }
 *     ```
 *   - Cách 2 (Đối với các Spring `@Service` vốn đã là Singleton Bean):
 *     Khai báo thẳng thuộc tính trong class:
 *     `private val log = LoggerFactory.getLogger(javaClass)`
 */

// 1. `object` Declaration: Singleton tuyệt đối an toàn đa luồng trên JVM
private object CurrencyConverter {
    private const val USD_TO_VND_RATE = 25_400.0

    fun usdToVnd(usd: Double): Double = usd * USD_TO_VND_RATE
}

// 2. Class có `private constructor` và `companion object` (Static Factory Method Pattern)
private class ApiToken private constructor(
    val rawToken: String,
    val maxRetry: Int
) {
    companion object {
        // Compile-time constant: được dịch thành `public static final int DEFAULT_MAX_RETRY = 3;`
        const val DEFAULT_MAX_RETRY: Int = 3
        private const val REQUIRED_PREFIX = "sk_live_"

        // Tương đương cách khai báo SLF4J Logger trong Spring Boot Kotlin
        private val loggerName: String = ApiToken::class.java.simpleName

        // Static Factory Method thay thế cho việc gọi constructor trực tiếp
        fun of(rawKey: String, retry: Int = DEFAULT_MAX_RETRY): ApiToken {
            println("    [Logger: $loggerName] Đang khởi tạo ApiToken qua companion object factory...")
            require(rawKey.startsWith(REQUIRED_PREFIX)) {
                "Token phải bắt đầu bằng '$REQUIRED_PREFIX'"
            }
            return ApiToken(rawKey, retry)
        }
    }

    override fun toString(): String = "ApiToken(masked='${rawToken.take(10)}***', maxRetry=$maxRetry)"
}

private interface RetryListener {
    fun onAttempt(attempt: Int)
    fun onExhausted()
}

fun main() {
    println("--- ___ * START: Ex03ObjectAndCompanionObject * ___ ---")

    // ========================================================================
    // 1. Sử dụng `object` Singleton
    // ========================================================================
    println("\n[1] `object` Declaration (Thread-safe Singleton):")
    val vnd = CurrencyConverter.usdToVnd(100.0)
    println("  100 USD = $vnd VND")

    // ========================================================================
    // 2. Sử dụng `companion object` (Constants & Factory Method)
    // ========================================================================
    println("\n[2] `companion object` (const val & Static Factory Method `of`):")
    println("  Hằng số ApiToken.DEFAULT_MAX_RETRY = ${ApiToken.DEFAULT_MAX_RETRY}")
    val token = ApiToken.of("sk_live_9876543210abcdef")
    println("  Đã tạo token: $token")

    // ========================================================================
    // 3. Sử dụng Object Expression (Anonymous Inner Class có nhiều phương thức)
    // ========================================================================
    println("\n[3] Object Expression (`object : RetryListener` cho interface có nhiều hàm):")
    val customListener = object : RetryListener {
        override fun onAttempt(attempt: Int) {
            println("    [Anonymous Object] Đang thử lại lần thứ #$attempt...")
        }

        override fun onExhausted() {
            println("    [Anonymous Object] Đã hết số lần thử lại (${ApiToken.DEFAULT_MAX_RETRY} lần)!")
        }
    }
    customListener.onAttempt(1)
    customListener.onAttempt(2)
    customListener.onExhausted()

    println("\n--- ___ * FINISH: Ex03ObjectAndCompanionObject * ___ ---")
}
