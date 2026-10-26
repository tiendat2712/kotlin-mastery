package view

/**
 * =====================================================================
 * LESSON 03 - EX03: BIỂU THỨC `when` — PHIÊN BẢN NÂNG CẤP CỦA `switch-case`
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java truyền thống (trước Java 14 Switch Expressions), `switch-case` khá hạn chế: chỉ hỗ trợ số nguyên nhỏ,
 *      `char`, `String`, `enum`, và dễ gây lỗi bởi cơ chế **Fall-through** (quên gõ `break;` là chạy tuốt xuống nhánh dưới!).
 *    - Trong Kotlin, `when` thay thế `switch` với nhiều ưu điểm:
 *      + **Không có Fall-through**: Khớp nhánh nào thì chỉ chạy đúng nhánh đó rồi thoát ngay, KHÔNG cần `break`!
 *      + Muốn gộp nhiều giá trị vào 1 nhánh? Dùng dấu phẩy `,` rất gọn: `200, 201, 204 -> ...`
 *      + Có thể kiểm tra miền giá trị (`in 400..499 ->`), kiểm tra kiểu dữ liệu kèm Smart Cast (`is String ->`),
 *        hoặc dùng `when` không có tham số (`when { ... }`) để thay thế chuỗi `if - else if` phức tạp.
 *      + Dưới tầng Bytecode: Nếu các nhánh là hằng số nguyên hoặc chuỗi, Kotlin dịch ra `tableswitch` / `lookupswitch`
 *        tốc độ $O(1)$ giống Java; nếu là điều kiện phức tạp, nó dịch ra chuỗi `if-else` tối ưu.
 *
 * 2. EXHAUSTIVE `when` (KIỂM TRA VÉT CẠN MỌI TRƯỜNG HỢP LÚC BIÊN DỊCH):
 *    - Khi dùng `when` làm Expression (hoặc khi kiểm tra `enum class` / `sealed class`), trình biên dịch Kotlin
 *      yêu cầu bạn phải xử lý **đầy đủ tất cả các trường hợp** (hoặc có nhánh `else`), giúp tránh bỏ sót
 *      trạng thái mới khi nâng cấp hệ thống!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Dùng `when (val response = callExternalService())` (khai báo biến cục bộ ngay trong ngoặc tròn của `when`,
 *      ra đời từ Kotlin 1.3+) để giới hạn phạm vi biến `response` chỉ nằm gọn bên trong khối `when`,
 *      rất sạch sẽ khi xử lý kết quả gọi REST API hoặc kết quả thanh toán!
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
    println("1. [When with Subject] Phan loai ma phan hoi HTTP (Gop gia tri & Range `in`):")
    for (httpCode in listOf(200, 201, 401, 404, 503)) {
        val message = when (httpCode) {
            200, 201, 204 -> "THANH CONG (2xx Success)"
            401, 403      -> "TU CHOI TRUY CAP (Auth Error)"
            in 400..499   -> "LOI PHIA CLIENT (4xx Client Error khac)"
            in 500..599   -> "LOI HE THONG MAY CHU (5xx Server Error)"
            else          -> "MA KHONG XAC DINH ($httpCode)"
        }
        println("   -> HTTP $httpCode: $message")
    }
    println()

    // =================================================================
    // 2. Exhaustive `when` trên Enum (Không cần nhánh `else` vì đã liệt kê đủ mọi phần tử!)
    // =================================================================
    println("2. [Exhaustive Enum When] Trinh bien dich kiem tra du cac trang thai cua Enum:")
    val currentStatus = PaymentStatus.COMPLETED
    val actionNote = when (currentStatus) {
        PaymentStatus.PENDING   -> "Dang cho cong thanh toan xac nhan..."
        PaymentStatus.COMPLETED -> "Da thu tien thanh cong -> Chuyen sang bo phan dong goi!"
        PaymentStatus.FAILED    -> "Giao dich that bai -> Gui thong bao thu lai."
        PaymentStatus.REFUNDED  -> "Da hoan tien ve vi khach hang."
    }
    println("   -> Trang thai [$currentStatus]: $actionNote\n")

    // =================================================================
    // 3. `when` kết hợp kiểm tra kiểu (`is`) và Smart Cast tự động
    // =================================================================
    println("3. [Type Matching + Smart Cast] Kiem tra kieu du lieu ben trong `when`:")
    val mixedInputs: List<Any?> = listOf("ORD-2026-99", 1_500_000L, listOf("CouponA", "CouponB"), null)
    for (item in mixedInputs) {
        val description = when (item) {
            null       -> "Gia tri bi NULL"
            is String  -> "Chuoi ma don '${item.lowercase()}' (do dai ${item.length})" // Smart cast thành String
            is Long    -> "So tien kieu Long = ${item + 50_000L} (da cong phu phi)"    // Smart cast thành Long
            is List<*> -> "Danh sach chua ${item.size} phan tu: $item"                 // Smart cast thành List
            else       -> "Kieu du lieu khac: ${item.javaClass.simpleName}"
        }
        println("   -> $description")
    }
    println()

    // =================================================================
    // 4. `when` KHÔNG có tham số (`when { ... }`) & Capturing variable `when (val x = ...)`
    // =================================================================
    println("4. [Subjectless When & Variable Capture] Danh gia dieu kien phuc hop:")
    val creditScore = 760
    val monthlyIncome = 35_000_000L
    val hasBadDebt = false

    // Dùng `when { ... }` không truyền biến vào ngoặc tròn khi các điều kiện kết hợp nhiều biến khác nhau:
    val loanDecision = when {
        hasBadDebt -> "TU CHOI (Co lich su no xau)"
        creditScore >= 750 && monthlyIncome >= 30_000_000L -> "PHE DUYET HAN MUC VIP (Lai suat uu dai 6.5%/nam)"
        creditScore >= 650 && monthlyIncome >= 15_000_000L -> "PHE DUYET HAN MUC TIEU CHUAN"
        else -> "CAN THAM DINH BO SUNG HO SO"
    }
    check(loanDecision.startsWith("PHE DUYET HAN MUC VIP"))
    println("   -> Ket qua xet duyet khoan vay: $loanDecision")

    // Bắt giữ giá trị trả về của hàm ngay trong `when (val result = ...)` (biến `apiLatencyMs` chỉ sống trong khối `when`):
    when (val apiLatencyMs = simulateGatewayPing()) {
        in 0..100   -> println("   -> [Gateway Ping] Toc do phan hoi nhanh: ${apiLatencyMs}ms")
        in 101..500 -> println("   -> [Gateway Ping] Toc do binh thuong: ${apiLatencyMs}ms")
        else        -> println("   -> [Gateway Ping] Canh bao do tre cao: ${apiLatencyMs}ms")
    }

    println("\n--- ___ * END: Ex03WhenExpression * ___ ---")
}

private fun simulateGatewayPing(): Int = 45
