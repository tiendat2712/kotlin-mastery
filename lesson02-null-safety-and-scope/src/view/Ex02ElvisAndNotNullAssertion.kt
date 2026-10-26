package view

import bean.Address
import bean.Customer

/**
 * =====================================================================
 * LESSON 02 - EX02: TOÁN TỬ ELVIS (`?:`) & NOT-NULL ASSERTION (`!!`)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - **Toán tử Elvis (`?:`)**:
 *      + Tên gọi "Elvis" vì khi xoay ngang `?:` trông giống mái tóc bồng bềnh của ca sĩ Elvis Presley.
 *      + Cú pháp: `val result = leftExpression ?: rightExpression`
 *      + Nguyên lý hoạt động: Nếu `leftExpression` **KHÁC `null`**, trả về ngay giá trị đó (đồng thời thu hẹp kiểu
 *        từ `T?` về Non-Null `T`!). Nếu `leftExpression` **BẰNG `null`**, mới tính toán và trả về `rightExpression`.
 *    - **Tại sao bên phải `?:` có thể viết `return` hoặc `throw Exception`?**
 *      + Như đã học ở Lesson 01 (`Ex03`), biểu thức `throw ...` và `return ...` trong Kotlin có kiểu là `Nothing`
 *        (kiểu con của mọi kiểu).
 *      + Do đó, `val customer: Customer = findById(id) ?: throw IllegalArgumentException(...)` hợp lệ 100% về mặt
 *        kiểu dữ liệu và giúp biến `customer` trở thành Non-Null `Customer` ngay từ dòng đó!
 *    - **Toán tử Not-Null Assertion (`!!`)**:
 *      + Ép buộc chuyển `T?` thành `T`. Dưới Bytecode, nó chèn `Intrinsics.checkNotNull(obj)`.
 *      + Nếu lúc runtime biến mang giá trị `null`, nó lập tức ném `NullPointerException` ngay tại dòng có dấu `!!`!
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java 8+ `Optional`:
 *      ```java
 *      Customer customer = customerRepo.findById(id)
 *          .orElseThrow(() -> new IllegalArgumentException("Customer " + id + " not found"));
 *      ```
 *    - Kotlin Elvis (`?:`) — ngắn gọn, không tốn chi phí tạo đối tượng `Optional` và Lambda `Supplier`:
 *      ```kotlin
 *      val customer = findCustomer(id)
 *          ?: throw IllegalArgumentException("Customer $id not found")
 *      ```
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Cặp bài trùng `?: throw ResponseStatusException(HttpStatus.NOT_FOUND, ...)` hoặc
 *      `?: throw EntityNotFoundException(...)` là **Idiom số 1** xuất hiện ở hầu hết mọi hàm `@Service`
 *      trong các dự án Spring Boot Kotlin thực tế!
 *    - Tuyệt đối **TRÁNH dùng `!!`** trong code nghiệp vụ Production (Code Review thường cấm `!!` trừ trong Unit Test).
 * =====================================================================
 */

// Giả lập Database trong bộ nhớ của tầng Repository
private val mockCustomerDb = mapOf(
    "CUST-100" to Customer(
        id = "CUST-100",
        name = "Phạm Minh Tuấn",
        email = "tuan.pham@kotlin.vn",
        address = Address(street = "01 Đại Cồ Việt", city = "Hà Nội")
    ),
    "CUST-200" to Customer(
        id = "CUST-200",
        name = "Hoàng Thu Trang",
        email = null,
        address = null
    )
)

// Hàm Repository trả về `Customer?` thay vì `Optional<Customer>`
private fun findCustomer(id: String): Customer? = mockCustomerDb[id]

fun main() {
    println("--- ___ * START: Ex02ElvisAndNotNullAssertion * ___ ---\n")

    // =================================================================
    // 1. Dùng Elvis `?:` để cung cấp giá trị mặc định (Default Value)
    // =================================================================
    println("1. [Elvis Default Value] Chuyển kiểu Nullable (String?) về Non-Null (String):")
    val customerWithNulls: Customer? = findCustomer("CUST-200")

    // Chú ý kiểu của `city` và `email` đã trở thành `String` (Non-Null), KHÔNG còn dấu `?` nữa!
    val city: String = customerWithNulls?.address?.city ?: "Unknown City"
    val email: String = customerWithNulls?.email ?: "no-reply@default.local"

    println("   -> Khách hàng: ${customerWithNulls?.name}")
    println("   -> Thành phố (đã qua Elvis `?:`): $city")
    println("   -> Email (đã qua Elvis `?:`):     $email\n")

    // =================================================================
    // 2. [Spring Kotlin Idiom] Kết hợp Elvis `?:` với `return` và `throw`
    // =================================================================
    println("2. [Spring Service Idiom] Dùng Elvis `?:` để Guard Clause (return sớm hoặc throw Exception):")

    // 2a. Trường hợp tìm thấy khách hàng
    val validCustomer = getVerifiedCustomerOrThrow("CUST-100")
    println("   -> Tìm thấy khách hàng hợp lệ: ${validCustomer.name} (ID: ${validCustomer.id})")

    // 2b. Trường hợp dùng `?: return` để thoát hàm sớm an toàn
    sendWelcomeNotification(customerId = "CUST-200") // Khách hàng không có email -> thoát sớm
    sendWelcomeNotification(customerId = "CUST-100") // Khách hàng có email -> gửi thành công

    // 2c. Trường hợp không tìm thấy -> ném IllegalArgumentException qua `?: throw`
    try {
        getVerifiedCustomerOrThrow("CUST-999")
    } catch (ex: IllegalArgumentException) {
        println("   -> [Bắt ngoại lệ từ `?: throw`]: ${ex.message}\n")
    }

    // =================================================================
    // 3. Toán tử `!!` (Not-Null Assertion) và tại sao nó là Anti-Pattern
    // =================================================================
    println("3. [Not-Null Assertion `!!`] Cách hoạt động và hậu quả khi lạm dụng:")
    val definitelyPresent: String? = "Dữ liệu chắc chắn không null"
    println("   -> Gọi `definitelyPresent!!.length`: ${definitelyPresent!!.length}")

    val unexpectedNull: String? = null
    try {
        // Dòng dưới đây cố tình dùng `!!` trên biến null để minh họa lỗi NullPointerException
        val len = unexpectedNull!!.length
        println("   -> Độ dài: $len")
    } catch (npe: NullPointerException) {
        println("   -> [CẢNH BÁO] Dùng `unexpectedNull!!` đã làm nổ NullPointerException!")
        println("   => Quy tắc Production: Luôn ưu tiên `?.`, `?:`, hoặc Smart Cast thay vì `!!`.")
    }

    println("\n--- ___ * END: Ex02ElvisAndNotNullAssertion * ___ ---")
}

// Mô phỏng hàm Service chuẩn trong Spring Boot Kotlin
private fun getVerifiedCustomerOrThrow(id: String): Customer {
    // Biến `customer` ở vế trái có kiểu `Customer` (Non-Null) nhờ vế phải `throw ...` có kiểu `Nothing`!
    val customer: Customer = findCustomer(id)
        ?: throw IllegalArgumentException("Customer $id not found in database!")
    return customer
}

private fun sendWelcomeNotification(customerId: String) {
    val customer = findCustomer(customerId) ?: return
    val targetEmail = customer.email ?: run {
        println("   -> [Skip] Bỏ qua gửi mail cho ${customer.name} vì chưa đăng ký email.")
        return
    }
    println("   -> [Sent] Đã gửi email chào mừng tới $targetEmail (${customer.name})!")
}
