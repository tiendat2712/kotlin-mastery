package view

/**
 * ============================================================================
 * BÀI 04: `typealias` vs `@JvmInline value class` (ZERO-OVERHEAD TYPE SAFETY!)
 * ============================================================================
 *
 * 1. BÀI TOÁN THỰC TẾ (PRIMITIVE OBSESSION):
 * ----------------------------------------------------------------------------
 * Giả sử bạn có hàm:
 *   `fun assignOrderToCustomer(customerId: String, productId: String, email: String)`
 * Vì cả 3 tham số đều là `String`, nếu lập trình viên truyền nhầm thứ tự:
 *   `assignOrderToCustomer(productId, customerId, email)`
 * -> Trình biên dịch Java/Kotlin hoàn toàn KHÔNG phát hiện ra lỗi! Hậu quả là ghi sai
 *    dữ liệu nghiêm trọng vào Database.
 *
 * 2. SO SÁNH `typealias` vs `@JvmInline value class` (UNDER THE HOOD / BYTECODE):
 * ----------------------------------------------------------------------------
 * A. `typealias`:
 *    `typealias CustomerIdAlias = String`
 *    - Chỉ đặt một "biệt danh" (tên gọi khác) cho một kiểu dữ liệu dài dòng (rất hữu ích
 *      cho kiểu phức tạp như `typealias OrderBuckets = Map<String, List<Order>>`).
 *    - KHÔNG tạo ra kiểu dữ liệu mới! Bạn vẫn truyền nhầm `String` thường hoặc `ProductIdAlias`
 *      vào chỗ của `CustomerIdAlias` mà trình biên dịch không hề cảnh báo.
 *
 * B. `@JvmInline value class` (Vũ khí bí mật của Kotlin):
 *    ```kotlin
 *    @JvmInline
 *    value class CustomerId(val value: String)
 *    ```
 *    - **Lúc biên dịch (Compile-time)**: `CustomerId` và `ProductId` là **2 kiểu dữ liệu
 *      hoàn toàn độc lập**. Truyền `ProductId` vào tham số `CustomerId` sẽ bị lỗi biên dịch ngay!
 *    - **Lúc chạy (Runtime / Bytecode / HEAP)**:
 *      Nếu dùng `data class CustomerId(val value: String)`, mỗi ID sẽ bọc thêm 1 đối tượng
 *      Wrapper trên HEAP (tốn thêm 16 bytes Object Header + con trỏ tham chiếu).
 *      Với `@JvmInline value class`, trình biên dịch Kotlin **BÓC VỎ (Unbox/Inline)** lớp bọc đó!
 *      Ở tầng Bytecode, hàm `fun findOrder(id: CustomerId)` được dịch thẳng thành
 *      `findOrder-<hash>(String id)` — **0% chi phí cấp phát bộ nhớ HEAP phụ trội**!
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong Domain-Driven Design (DDD) với Spring Boot + Kotlin (Jackson 2.15+ / Spring Boot 3.x):
 *   - `@JvmInline value class` được dùng cho `CustomerId`, `OrderId`, `Email`, `MoneyAmount`,
 *     vừa có thể chứa logic kiểm tra hợp lệ trong khối `init { require(...) }`, vừa chạy nhanh
 *     như kiểu nguyên thủy (`Long`, `String`).
 */

// 1. `typealias`: Làm gọn các kiểu dữ liệu lồng nhau hoặc kiểu hàm (Function Type)
private typealias UserRolePermissions = Map<String, Set<String>>

// 2. `@JvmInline value class`: An toàn kiểu dữ liệu tuyệt đối lúc Compile, 0% tốn thêm HEAP lúc Runtime!
@JvmInline
value class CustomerId(val value: String) {
    init {
        require(value.startsWith("CUST-")) { "CustomerId phai bat dau bang 'CUST-' (nhan duoc: $value)" }
    }
}

@JvmInline
value class ProductId(val value: String) {
    init {
        require(value.startsWith("PROD-")) { "ProductId phai bat dau bang 'PROD-' (nhan duoc: $value)" }
    }
}

@JvmInline
value class EmailAddress(val value: String) {
    init {
        require(value.contains("@") && value.contains(".")) { "Email khong hop le: $value" }
    }

    // Value class vẫn có thể có Computed Property và phương thức riêng!
    val domain: String
        get() = value.substringAfter("@")
}

private fun processPurchase(customerId: CustomerId, productId: ProductId, receiptEmail: EmailAddress) {
    println("  [Order Service] Khach hang '${customerId.value}' mua san pham '${productId.value}'")
    println("                  Gui hoa don ve '${receiptEmail.value}' (Domain: ${receiptEmail.domain})")
}

fun main() {
    println("--- ___ * START: Ex04ValueClassAndTypeAlias * ___ ---")

    // ========================================================================
    // 1. Sử dụng `typealias` để tăng độ dễ đọc cho cấu trúc dữ liệu phức tạp
    // ========================================================================
    println("\n[1] Su dung `typealias UserRolePermissions = Map<String, Set<String>>`:")
    val permissions: UserRolePermissions = mapOf(
        "ADMIN" to setOf("READ", "WRITE", "DELETE"),
        "AUDITOR" to setOf("READ")
    )
    println("  Quyen he thong: $permissions")

    // ========================================================================
    // 2. Sử dụng `@JvmInline value class` chống truyền nhầm tham số
    // ========================================================================
    println("\n[2] `@JvmInline value class` (Compile-time Type Safety + Zero Heap Overhead):")
    val custId = CustomerId("CUST-2026-01")
    val prodId = ProductId("PROD-MACBOOK-M4")
    val email = EmailAddress("developer@kotlinjourney.vn")

    processPurchase(custId, prodId, email)

    // Thử mở comment dòng dưới: Nếu lập trình viên truyền nhầm vị trí `prodId` và `custId`,
    // trình biên dịch sẽ chặn đứng ngay lập tức thay vì để lọt lỗi xuống Database!
    // processPurchase(prodId, custId, email) // ERROR: Argument type mismatch!

    println("\n[3] Validation tu dong ben trong `init` cua `@JvmInline value class`:")
    try {
        EmailAddress("invalid-email-without-at-sign")
    } catch (e: IllegalArgumentException) {
        println("  -> Da chan Email loi ngay luc khoi tao: ${e.message}")
    }

    println("\n--- ___ * FINISH: Ex04ValueClassAndTypeAlias * ___ ---")
}
