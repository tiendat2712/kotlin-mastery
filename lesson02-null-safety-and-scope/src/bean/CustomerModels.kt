package bean

/**
 * =====================================================================
 * LESSON 02 - BEAN: CÁC MODEL DÙNG CHUNG CHO BÀI HỌC NULL SAFETY & SCOPE
 * =====================================================================
 *
 * Lưu ý thiết kế Nullability trong Domain Model của Kotlin:
 * - Những thuộc tính BẮT BUỘC phải có (Mandatory): khai báo kiểu Non-Null (`String`, `Int`, `Boolean`).
 * - Những thuộc tính TÙY CHỌN có thể vắng mặt (Optional): khai báo dấu hỏi chấm `?` (`String?`, `Address?`).
 * -> Ngay từ chữ ký của class, lập trình viên đã biết chính xác cột/trường nào trong Database có thể `NULL`!
 */

data class Address(
    val street: String?,
    val city: String?
)

data class Customer(
    val id: String,
    val name: String,
    val email: String?,
    val address: Address?
)

data class AccountConfig(
    var host: String = "",
    var port: Int = 8080,
    var useSsl: Boolean = false
)
