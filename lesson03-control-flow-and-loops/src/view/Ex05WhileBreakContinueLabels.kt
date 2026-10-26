package view

/**
 * =====================================================================
 * LESSON 03 - EX05: VÒNG LẶP `while`, `do-while`, NHÃN `label@` & BẪY `return@forEach`
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Vòng lặp `while` và `do-while` trong Kotlin hoạt động giống hệt 100% trong Java.
 *    - **Labeled Break / Continue (`tenNhan@` ... `break@tenNhan` / `continue@tenNhan`)**:
 *      + Trong vòng lặp lồng nhau (2 vòng `for` trong-ngoài), lệnh `break` thông thường chỉ thoát khỏi
 *        vòng lặp **bên trong cùng**.
 *      + Khi gắn nhãn `outer@ for (...)`, bạn có thể gọi `break@outer` để **thoát thẳng ra khỏi cả vòng lặp ngoài**,
 *        hoặc `continue@outer` để nhảy sang lượt tiếp theo của vòng lặp ngoài ngay lập tức!
 *
 * 2. CẨN TRỌNG ĐẶC BIỆT KHI CHUYỂN TỪ JAVA SANG KOTLIN: `return` BÊN TRONG `.forEach { ... }`!
 *    - Trong Java: Bên trong `list.forEach(item -> { if (...) return; })`, chữ `return` chỉ kết thúc
 *      lượt chạy hiện tại của Lambda (tương đương `continue` trong vòng lặp `for`).
 *    - Trong Kotlin: Vì `.forEach { ... }` là một **`inline` function**, nên nếu bạn viết chữ `return`
 *      trơn bên trong `{ ... }`, nó là **Non-local Return** — nghĩa là nó **THOÁT LUÔN KHỎI HÀM BAO NGOÀI (`main` / hàm Service)**!
 *    - Muốn chỉ bỏ qua phần tử hiện tại trong `.forEach` (giống `continue`), trong Kotlin bạn PHẢI viết
 *      **Local Return có nhãn**: `return@forEach`!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Khi duyệt danh sách đơn hàng bằng `.forEach` trong `@Service`, nếu muốn bỏ qua một đơn hàng không hợp lệ
 *      để xử lý tiếp đơn hàng sau, hãy nhớ dùng `return@forEach` (hoặc tốt hơn nữa: dùng `.filter { ... }.forEach { ... }`
 *      hoặc vòng lặp `for (order in orders)` truyền thống với `continue`)!
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex05WhileBreakContinueLabels * ___ ---\n")

    // =================================================================
    // 1. Vòng lặp `while` và `do-while` (Mô phỏng cơ chế Retry kết nối Database)
    // =================================================================
    println("1. [while & do-while] Mô phỏng cơ chế Retry kết nối dịch vụ thanh toán:")
    var attempt = 1
    val maxAttempts = 3
    var connected = false

    while (attempt <= maxAttempts && !connected) {
        println("   -> Thử kết nối lần thứ $attempt/$maxAttempts...")
        if (attempt == 2) {
            connected = true
            println("   -> [THÀNH CÔNG] Đã kết nối ở lần thử thứ $attempt!")
        }
        attempt++
    }

    // do-while luôn chạy thân vòng lặp ít nhất 1 lần trước khi kiểm tra điều kiện
    var healthCheckCount = 0
    do {
        healthCheckCount++
    } while (healthCheckCount < 1)
    println("   -> Số lần chạy health-check tối thiểu với do-while: $healthCheckCount\n")

    // =================================================================
    // 2. Labeled `break@outer` và `continue@outer` trong vòng lặp lồng nhau
    // =================================================================
    println("2. [Labeled break@ & continue@] Điều khiển vòng lặp lồng nhau chính xác:")
    val warehouses = listOf("Kho Hà Nội", "Kho Đà Nẵng", "Kho TP.HCM")
    val racksByWarehouse = mapOf(
        "Kho Hà Nội" to listOf("Kệ A1 (Trống)", "Kệ A2 (Lỗi cảm biến)", "Kệ A3 (Có hàng)"),
        "Kho Đà Nẵng" to listOf("Kệ B1 (Trống)", "Kệ B2 (Có hàng TARGET-SKU)"),
        "Kho TP.HCM" to listOf("Kệ C1 (Có hàng TARGET-SKU)")
    )

    // Bài toán: Tìm kệ đầu tiên chứa "TARGET-SKU" trên toàn quốc; nếu gặp "Lỗi cảm biến" thì bỏ qua kho đó sang kho kế tiếp!
    warehouseLoop@ for (warehouse in warehouses) {
        val racks = racksByWarehouse[warehouse].orEmpty()
        for (rack in racks) {
            println("   -> Đang quét [$warehouse] - $rack")
            if ("Lỗi cảm biến" in rack) {
                println("      [continue@warehouseLoop] Phát hiện lỗi cảm biến tại $warehouse -> Chuyển ngay sang kho tiếp theo!")
                continue@warehouseLoop
            }
            if ("TARGET-SKU" in rack) {
                println("      [break@warehouseLoop] ĐÃ TÌM THẤY TARGET-SKU tại [$warehouse - $rack]! Dừng toàn bộ quá trình tìm kiếm!")
                break@warehouseLoop
            }
        }
    }
    println()

    // =================================================================
    // 3. Sự khác biệt sống còn giữa `return@forEach` (Local) và `return` (Non-local)
    // =================================================================
    println("3. [return@forEach vs Non-local return] Xử lý lô giao dịch với `.forEach`:")
    processTransactionsWithForEach(listOf(100_000L, -50_000L, 250_000L, 999_999_999L, 300_000L))

    println("\n--- ___ * END: Ex05WhileBreakContinueLabels * ___ ---")
}

private fun processTransactionsWithForEach(amounts: List<Long>) {
    amounts.forEach { amount ->
        if (amount < 0) {
            // `return@forEach` đóng vai trò giống hệt `continue`: chỉ bỏ qua phần tử âm này và đi tiếp phần tử sau!
            println("   -> [return@forEach ~ continue] Bỏ qua giao dịch âm không hợp lệ: $amount")
            return@forEach
        }
        if (amount > 100_000_000L) {
            // `return` không có nhãn bên trong inline lambda `.forEach` sẽ THOÁT HẲN khỏi hàm `processTransactionsWithForEach`!
            println("   -> [Non-local return ~ break function] Phát hiện giao dịch vượt hạn mức bảo mật ($amount) -> Dừng ngay hàm!")
            return
        }
        println("   -> Đã hạch toán thành công giao dịch: $amount VND")
    }
    // Dòng này sẽ KHÔNG được in ra nếu đã dính lệnh `return` (Non-local) ở trên!
    println("   -> Đã duyệt xong toàn bộ danh sách.")
}
