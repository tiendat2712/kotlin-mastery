package view

/**
 * =====================================================================
 * LESSON 04 - EX02: DEFAULT ARGUMENTS, NAMED ARGUMENTS & VARARG (`*`)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java (`Ex01OverloadDemo.java` và `Ex02BuilderDemo.java` ở `D:\Java_Journey`):
 *      + Vì Java KHÔNG có tham số mặc định (Default Arguments), bạn buộc phải viết 3-4 hàm trùng tên
 *        (Telescoping Method Overloading) gọi chồng chéo nhau, hoặc phải viết cả một class `Builder` dài hàng chục dòng!
 *      + Ngoài ra, khi gọi hàm có nhiều tham số cùng kiểu `createUser("An", "Nguyễn", true, false, true)`,
 *        người đọc code Java không thể biết `true, false, true` nghĩa là gì!
 *    - Trong Kotlin:
 *      + **Default Arguments (`page: Int = 1, size: Int = 20`)**: Cho phép gán giá trị mặc định ngay tại nơi khai báo hàm.
 *        Dưới tầng Bytecode, Kotlin sinh ra một hàm tổng hợp `findOrders$default(..., int mask, Object marker)`
 *        sử dụng bitmask để biết tham số nào bị khuyết và tự động điền giá trị mặc định!
 *      + **Named Arguments (`findOrders(size = 50, status = "DELIVERED")`)**: Cho phép chỉ định rõ tên tham số
 *        khi gọi hàm và **đổi thứ tự tham số tùy ý** mà không sợ truyền nhầm!
 *      + **`vararg` và toán tử Spread (`*`)**: Tương đương `String... tags` của Java, nhưng khi truyền một mảng có sẵn
 *        vào vị trí `vararg`, Kotlin yêu cầu dùng dấu sao `*array` (Spread operator) để phân biệt rõ ràng!
 *
 * 2. SO SÁNH JAVA BUILDER PATTERN VS KOTLIN NAMED + DEFAULT ARGUMENTS:
 *    - Một `data class` hoặc hàm khởi tạo trong Kotlin có Default + Named Arguments **thay thế hoàn toàn 100%
 *      nhu cầu viết Builder Pattern thủ công hay dùng `@Builder` của Lombok** trong Java!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Khi viết hàm dựng dữ liệu mẫu trong Test (Test Object Mother / Fixture):
 *      `fun createTestOrder(id: Long = 1L, status: String = "PAID", total: Long = 100_000L): Order`
 *      Mỗi bài test chỉ cần ghi đè đúng trường nó quan tâm: `createTestOrder(status = "CANCELLED")`!
 * =====================================================================
 */

// Thay thế hoàn toàn Builder Pattern (`Ex02BuilderDemo.java`) chỉ bằng 1 Constructor có Default Values!
private data class HttpClientOptions(
    val baseUrl: String,
    val connectTimeoutMs: Int = 5_000,
    val readTimeoutMs: Int = 10_000,
    val retryOnFailure: Boolean = true,
    val enableGzip: Boolean = true,
    val maxConnections: Int = 50
)

fun main() {
    println("--- ___ * START: Ex02NamedAndDefaultArguments * ___ ---\n")

    // =================================================================
    // 1. Default Arguments: Khai tử Telescoping Overloads của Java
    // =================================================================
    println("1. [Default Arguments] Gọi cùng 1 hàm với số lượng đối số linh hoạt:")
    // Gọi không truyền gì -> lấy toàn bộ mặc định (status="ALL", page=1, size=20, sortBy="createdAt")
    findOrders()

    // Chỉ truyền tham số đầu tiên
    findOrders("PENDING")

    // Truyền 2 tham số đầu
    findOrders("PAID", 2)
    println()

    // =================================================================
    // 2. Named Arguments: Gọi đích danh tên tham số & Đổi thứ tự tùy ý
    // =================================================================
    println("2. [Named Arguments] Chỉ định rõ tên tham số — Tự giải thích nghĩa (Self-documenting):")
    // Bỏ qua `page`, chỉ truyền `size` và `status` theo thứ tự đảo ngược vẫn chạy chính xác 100%!
    findOrders(
        size = 50,
        status = "DELIVERED",
        sortBy = "totalAmount"
    )
    println()

    // =================================================================
    // 3. Khai tử Java Builder Pattern bằng Named + Default Arguments
    // =================================================================
    println("3. [Goodbye Java Builder] Khởi tạo Object nhiều tham số cực kỳ tường minh:")
    val customClient = HttpClientOptions(
        baseUrl = "https://api.payment.vn/v2",
        readTimeoutMs = 30_000,
        retryOnFailure = false
        // connectTimeoutMs, enableGzip, maxConnections tự động nhận giá trị mặc định!
    )
    println("   -> Cấu hình HTTP Client: $customClient\n")

    // =================================================================
    // 4. Tham số `vararg` và Toán tử trải mảng Spread Operator (`*`)
    // =================================================================
    println("4. [vararg & Spread Operator `*`] Truyền số lượng đối số tùy ý và giải nén mảng:")
    // 4a. Truyền rời từng chuỗi
    logAuditTags(prefix = "ORDER_SERVICE", "CREATE", "PAYMENT_VERIFIED", "INVENTORY_LOCKED")

    // 4b. Khi đã có sẵn một Array<String>, dùng dấu `*` (Spread Operator) để trải từng phần tử vào `vararg`
    // (Có thể kết hợp cả mảng trải `*` lẫn các phần tử rời trong cùng 1 lời gọi hàm!)
    val securityTags = arrayOf("JWT_AUTH", "RATE_LIMITED")
    logAuditTags(prefix = "API_GATEWAY", "INCOMING_REQ", *securityTags, "RESPONSE_200")

    println("\n--- ___ * END: Ex02NamedAndDefaultArguments * ___ ---")
}

private fun findOrders(
    status: String = "ALL",
    page: Int = 1,
    size: Int = 20,
    sortBy: String = "createdAt"
) {
    println("   -> [Query Orders] status=$status | page=$page | size=$size | sortBy=$sortBy")
}

private fun logAuditTags(prefix: String, vararg tags: String) {
    // Bên trong hàm, `tags` có kiểu là `Array<out String>`
    println("   -> [$prefix] (${tags.size} tags): ${tags.joinToString(separator = " -> ")}")
}
