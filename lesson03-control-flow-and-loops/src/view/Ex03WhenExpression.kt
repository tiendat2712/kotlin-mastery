package view

/**
 * =====================================================================
 * LESSON 03 - EX03: BIỂU THỨC `when` — PHIÊN BẢN SIÊU CẤP CỦA `switch-case`
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java truyền thống, `switch-case` rất hạn chế: chỉ hỗ trợ số nguyên nhỏ, `char`, `String`, `enum`,
 *      và nguy hiểm nhất là cơ chế **Fall-through** (quên gõ `break;` là chạy tuốt xuống nhánh dưới!).
 *    - Trong Kotlin, `when` thay thế hoàn toàn `switch` với sức mạnh vượt trội:
 *      + **Không có Fall-through**: Khớp nhánh nào thì chỉ chạy đúng nhánh đó rồi thoát ngay, KHÔNG cần `break`!
 *      + Muốn gộp nhiều giá trị vào 1 nhánh? Dùng dấu phẩy `,` cực gọn: `200, 201, 204 -> ...`
 *      + Có thể kiểm tra miền giá trị (`in 400..499 ->`), kiểm tra kiểu dữ liệu kèm Smart Cast (`is String ->`),
 *        hoặc dùng `when` không có tham số (`when { ... }`) để thay thế chuỗi `if - else if` phức tạp.
 *      + Dưới tầng Bytecode: Nếu các nhánh là hằng số nguyên hoặc chuỗi, Kotlin dịch ra `tableswitch` / `lookupswitch`
 *        tốc độ $O(1)$ giống hệt Java; nếu là điều kiện phức tạp, nó dịch ra chuỗi `if-else` tối ưu.
 *
 * 2. EXHAUSTIVE `when` (KIỂM TRA VÉT CẠN MỌI TRƯỜNG HỢP LÚC BIÊN DỊCH):
 *    - Khi dùng `when` làm Expression (hoặc khi kiểm tra `enum class` / `sealed class`), trình biên dịch Kotlin
 *      bắt buộc bạn phải xử lý **đầy đủ 100% các trường hợp** (hoặc có nhánh `else`), giúp không bao giờ bỏ sót
 *      trạng thái mới khi nâng cấp hệ thống!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Dùng `when (val response = callExternalService())` (khai báo biến cục bộ ngay trong ngoặc tròn của `when`,
 *      ra đời từ Kotlin 1.3+) để giới hạn phạm vi biến `response` chỉ nằm gọn bên trong khối `when`,
 *      cực kỳ sạch sẽ khi xử lý kết quả gọi REST API hoặc kết quả thanh toán!
 * =====================================================================
 */

private enum class PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}

fun main() {
    println("--- ___ * START: Ex03WhenExpression * ___ ---\n")

    // =================================================================
    // 1. `when` có đối tượng (Subject): Gộp nhiều giá trị (`,`) và kiểm tra khoảng (`in`)
    // =================================================================
    println("1. [When with Subject] Phân loại mã phản hồi HTTP (Gộp giá trị & Range `in`):")
    for (httpCode in listOf(200, 201, 401, 404, 503)) {
        val message = when (httpCode) {
            200, 201, 204 -> "THÀNH CÔNG (2xx Success)"
            401, 403      -> "TỪ CHỐI TRUY CẬP (Auth Error)"
            in 400..499   -> "LỖI PHÍA CLIENT (4xx Client Error khác)"
            in 500..599   -> "LỖI HỆ THỐNG MÁY CHỦ (5xx Server Error)"
            else          -> "MÃ KHÔNG XÁC ĐỊNH ($httpCode)"
        }
        println("   -> HTTP $httpCode: $message")
    }
    println()

    // =================================================================
    // 2. Exhaustive `when` trên Enum (Không cần nhánh `else` vì đã liệt kê đủ mọi phần tử!)
    // =================================================================
    println("2. [Exhaustive Enum When] Trình biên dịch kiểm tra đủ 100% trạng thái của Enum:")
    val currentStatus = PaymentStatus.COMPLETED
    val actionNote = when (currentStatus) {
        PaymentStatus.PENDING   -> "Đang chờ cổng thanh toán xác nhận..."
        PaymentStatus.COMPLETED -> "Đã thu tiền thành công -> Chuyển sang bộ phận đóng gói!"
        PaymentStatus.FAILED    -> "Giao dịch thất bại -> Gửi thông báo thử lại."
        PaymentStatus.REFUNDED  -> "Đã hoàn tiền về ví khách hàng."
    }
    println("   -> Trạng thái [$currentStatus]: $actionNote\n")

    // =================================================================
    // 3. `when` kết hợp kiểm tra kiểu (`is`) và Smart Cast tự động
    // =================================================================
    println("3. [Type Matching + Smart Cast] Kiểm tra kiểu dữ liệu bên trong `when`:")
    val mixedInputs: List<Any?> = listOf("ORD-2026-99", 1_500_000L, listOf("CouponA", "CouponB"), null)
    for (item in mixedInputs) {
        val description = when (item) {
            null       -> "Giá trị bị NULL"
            is String  -> "Chuỗi mã đơn '${item.lowercase()}' (độ dài ${item.length})" // Smart cast thành String
            is Long    -> "Số tiền kiểu Long = ${item + 50_000L} (đã cộng phụ phí)"    // Smart cast thành Long
            is List<*> -> "Danh sách chứa ${item.size} phần tử: $item"                 // Smart cast thành List
            else       -> "Kiểu dữ liệu khác: ${item.javaClass.simpleName}"
        }
        println("   -> $description")
    }
    println()

    // =================================================================
    // 4. `when` KHÔNG có tham số (`when { ... }`) & Capturing variable `when (val x = ...)`
    // =================================================================
    println("4. [Subjectless When & Variable Capture] Đánh giá điều kiện phức hợp:")
    val creditScore = 760
    val monthlyIncome = 35_000_000L
    val hasBadDebt = false

    // Dùng `when { ... }` không truyền biến vào ngoặc tròn khi các điều kiện kết hợp nhiều biến khác nhau:
    val loanDecision = when {
        hasBadDebt -> "TỪ CHỐI (Có lịch sử nợ xấu)"
        creditScore >= 750 && monthlyIncome >= 30_000_000L -> "PHÊ DUYỆT HẠN MỨC VIP (Lãi suất ưu đãi 6.5%/năm)"
        creditScore >= 650 && monthlyIncome >= 15_000_000L -> "PHÊ DUYỆT HẠN MỨC TIÊU CHUẨN"
        else -> "CẦN THẨM ĐỊNH BỔ SUNG HỒ SƠ"
    }
    println("   -> Kết quả xét duyệt khoản vay: $loanDecision")

    // Bắt giữ giá trị trả về của hàm ngay trong `when (val result = ...)` (biến `apiLatencyMs` chỉ sống trong khối `when`):
    when (val apiLatencyMs = simulateGatewayPing()) {
        in 0..100   -> println("   -> [Gateway Ping] Tốc độ phản hồi cực nhanh: ${apiLatencyMs}ms")
        in 101..500 -> println("   -> [Gateway Ping] Tốc độ bình thường: ${apiLatencyMs}ms")
        else        -> println("   -> [Gateway Ping] Cảnh báo độ trễ cao: ${apiLatencyMs}ms")
    }

    println("\n--- ___ * END: Ex03WhenExpression * ___ ---")
}

private fun simulateGatewayPing(): Int = 45
