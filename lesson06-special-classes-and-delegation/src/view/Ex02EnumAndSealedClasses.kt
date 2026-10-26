package view

/**
 * ============================================================================
 * BÀI 02: `enum class`, `sealed class` & `sealed interface` (ADT)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * A. `enum class` & `Enum.entries` (Kotlin 1.9+):
 *    - Trong Java và Kotlin cũ, mỗi lần gọi `OrderStatus.values()`, JVM phải cấp phát
 *      một mảng mới (`OrderStatus[]`) trên HEAP (vì mảng trong JVM có thể bị sửa phần tử!).
 *    - Từ Kotlin 1.9+, thuộc tính `OrderStatus.entries` trả về một `EnumEntries<OrderStatus>`
 *      (danh sách bất biến được tạo sẵn 1 lần duy nhất), không tốn bộ nhớ cấp phát mảng mới!
 *
 * B. `sealed class` / `sealed interface` (Hệ phân cấp đóng - Algebraic Data Types):
 *    - Hãy coi `sealed class` là **`enum` nâng cấp siêu cấp**:
 *      + Trong `enum class`: Mỗi hằng số (`PENDING`, `SHIPPED`) chỉ có đúng **1 instance duy nhất**
 *        (Singleton) và mọi hằng số phải có cùng cấu trúc thuộc tính.
 *      + Trong `sealed class` / `sealed interface`: Tập hợp các kiểu con (subtypes) được cố định
 *        ngay lúc biên dịch (trong cùng package/module), NHƯNG mỗi kiểu con có thể là:
 *        - Một `data class` mang cấu trúc dữ liệu hoàn toàn khác nhau và tạo được nhiều instance trên HEAP!
 *        - Một `data object` (Singleton không mang trạng thái phụ, nên dùng `data object` để có
 *          hàm `toString()` đẹp mắt thay vì in địa chỉ bộ nhớ).
 *
 * C. Exhaustive `when` (Kiểm tra vét cạn không cần `else`):
 *    Vì trình biên dịch biết chính xác 100% tất cả các nhánh con của `sealed interface`,
 *    biểu thức `when (result)` KHÔNG CẦN nhánh `else`. Nếu ngày mai đồng nghiệp thêm một
 *    trạng thái mới vào `sealed interface`, trình biên dịch sẽ báo lỗi ngay tại tất cả các
 *    chỗ `when` chưa xử lý trạng thái mới đó!
 *
 * 2. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong Spring Boot + Kotlin hiện đại, `sealed interface` được dùng khắp nơi để mô hình hóa
 * kết quả nghiệp vụ (Domain Result / UseCase Output) thay vì lạm dụng `try-catch` Exception
 * (vốn rất tốn chi phí tạo StackTrace trên JVM):
 *   ```kotlin
 *   sealed interface PaymentResult {
 *       data class Success(val transactionId: String, val paidAmount: Double) : PaymentResult
 *       data class ValidationError(val field: String, val reason: String) : PaymentResult
 *       data object InsufficientBalance : PaymentResult
 *       data object GatewayTimeout : PaymentResult
 *   }
 *   ```
 * Ở tầng `@RestController`, bạn chỉ cần `when (val result = service.pay(req))` để ánh xạ
 * từng nhánh sang `ResponseEntity.ok(...)`, `badRequest()`, `status(422)` hay `status(504)`!
 */

// 1. Enum Class với thuộc tính, phương thức và `.entries`
private enum class PaymentMethod(val displayName: String, val feeRate: Double) {
    BANK_TRANSFER("Chuyen khoan QR Napas", 0.0),
    CREDIT_CARD("The tin dung Quoc te", 0.025),
    E_WALLET("Vi dien tu", 0.01);

    fun calculateFee(amount: Double): Double = amount * feeRate
}

// 2. Sealed Interface biểu diễn kết quả xử lý API / Nghiệp vụ (Type-safe State Hierarchy)
private sealed interface CheckoutResult<out T> {
    data class Success<T>(val data: T, val receiptCode: String) : CheckoutResult<T>
    data class ValidationError(val field: String, val message: String) : CheckoutResult<Nothing>
    data object Unauthorized : CheckoutResult<Nothing>
}

// Hàm xử lý kết quả sử dụng Exhaustive `when` + Smart Cast tự động (KHÔNG cần nhánh `else`!)
private fun <T> renderHttpResponse(result: CheckoutResult<T>): String = when (result) {
    is CheckoutResult.Success ->
        "HTTP 200 OK | Receipt=${result.receiptCode} | Payload=${result.data}"
    is CheckoutResult.ValidationError ->
        "HTTP 400 BAD REQUEST | Field='${result.field}' -> ${result.message}"
    CheckoutResult.Unauthorized ->
        "HTTP 401 UNAUTHORIZED | Vui long dang nhap lai de thanh toan"
}

fun main() {
    println("--- ___ * START: Ex02EnumAndSealedClasses * ___ ---")

    // ========================================================================
    // 1. Duyệt Enum bằng `.entries` (Kotlin 1.9+ Idiomatic)
    // ========================================================================
    println("\n[1] Danh sach phuong thuc thanh toan (`PaymentMethod.entries`):")
    val orderAmount = 2_000.0
    for (method in PaymentMethod.entries) {
        println("  - ${method.name} (${method.displayName}): Phi cho $orderAmount = ${method.calculateFee(orderAmount)}")
    }

    // ========================================================================
    // 2. Sealed Interface & Exhaustive `when` + Smart Cast
    // ========================================================================
    println("\n[2] Mo hinh hoa trang thai nghiep vu voi `sealed interface` & Exhaustive `when`:")
    val responses: List<CheckoutResult<String>> = listOf(
        CheckoutResult.Success(data = "Order #9001 Confirmed", receiptCode = "RCP-2026-88"),
        CheckoutResult.ValidationError(field = "cardNumber", message = "So the phai du 16 chu so"),
        CheckoutResult.Unauthorized
    )

    for (res in responses) {
        println("  -> ${renderHttpResponse(res)}")
    }

    println("\n--- ___ * FINISH: Ex02EnumAndSealedClasses * ___ ---")
}
