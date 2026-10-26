package view

/**
 * =====================================================================
 * LESSON 03 - EX05: VÒNG LẶP `while`, `do-while`, NHÃN `label@` & BẪY `return@forEach`
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Vòng lặp `while` và `do-while` trong Kotlin hoạt động giống trong Java.
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
 *    - Muốn chỉ bỏ qua phần tử hiện tại trong `.forEach` (giống `continue`), trong Kotlin bạn CẦN viết
 *      **Local Return có nhãn**: `return@forEach`!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Khi duyệt danh sách đơn hàng bằng `.forEach` trong `@Service`, nếu muốn bỏ qua một đơn hàng không hợp lệ
 *      để xử lý tiếp đơn hàng sau, hãy nhớ dùng `return@forEach` (hoặc tốt hơn: dùng `.filter { ... }.forEach { ... }`
 *      hay vòng lặp `for (order in orders)` truyền thống với `continue`)!
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex05WhileBreakContinueLabels * ___ ---\n")

    // =================================================================
    // 1. Vòng lặp `while` và `do-while` (Mô phỏng cơ chế Retry kết nối Database)
    // =================================================================
    println("1. [while & do-while] Mo phong co che Retry ket noi dich vu thanh toan:")
    var attempt = 1
    val maxAttempts = 3
    var connected = false

    while (attempt <= maxAttempts && !connected) {
        println("   -> Thu ket noi lan thu $attempt/$maxAttempts...")
        if (attempt == 2) {
            connected = true
            println("   -> [THANH CONG] Da ket noi o lan thu $attempt!")
        }
        attempt++
    }
    check(connected)

    // do-while luôn chạy thân vòng lặp ít nhất 1 lần trước khi kiểm tra điều kiện
    var healthCheckCount = 0
    do {
        healthCheckCount++
    } while (healthCheckCount < 1)
    check(healthCheckCount == 1)
    println("   -> So lan chay health-check toi thieu voi do-while: $healthCheckCount\n")

    // =================================================================
    // 2. Labeled `break@outer` và `continue@outer` trong vòng lặp lồng nhau
    // =================================================================
    println("2. [Labeled break@ & continue@] Dieu khien vong lap long nhau chinh xac:")
    val warehouses = listOf("Kho Ha Noi", "Kho Da Nang", "Kho TP.HCM")
    val racksByWarehouse = mapOf(
        "Kho Ha Noi" to listOf("Ke A1 (Trong)", "Ke A2 (Loi cam bien)", "Ke A3 (Co hang)"),
        "Kho Da Nang" to listOf("Ke B1 (Trong)", "Ke B2 (Co hang TARGET-SKU)"),
        "Kho TP.HCM" to listOf("Ke C1 (Co hang TARGET-SKU)")
    )

    // Bài toán: Tìm kệ đầu tiên chứa "TARGET-SKU" trên toàn quốc; nếu gặp "Loi cam bien" thì bỏ qua kho đó sang kho kế tiếp!
    var foundLocation: String? = null
    warehouseLoop@ for (warehouse in warehouses) {
        val racks = racksByWarehouse[warehouse].orEmpty()
        for (rack in racks) {
            println("   -> Dang quet [$warehouse] - $rack")
            if ("Loi cam bien" in rack) {
                println("      [continue@warehouseLoop] Phat hien loi cam bien tai $warehouse -> Chuyen ngay sang kho tiep theo!")
                continue@warehouseLoop
            }
            if ("TARGET-SKU" in rack) {
                foundLocation = "$warehouse - $rack"
                println("      [break@warehouseLoop] DA TIM THAY TARGET-SKU tai [$foundLocation]! Dung toan bo qua trinh tim kiem!")
                break@warehouseLoop
            }
        }
    }
    check(foundLocation == "Kho Da Nang - Ke B2 (Co hang TARGET-SKU)")
    println()

    // =================================================================
    // 3. Sự khác biệt sống còn giữa `return@forEach` (Local) và `return` (Non-local)
    // =================================================================
    println("3. [return@forEach vs Non-local return] Xu ly lo giao dich voi `.forEach`:")
    processTransactionsWithForEach(listOf(100_000L, -50_000L, 250_000L, 999_999_999L, 300_000L))

    println("\n--- ___ * END: Ex05WhileBreakContinueLabels * ___ ---")
}

private fun processTransactionsWithForEach(amounts: List<Long>) {
    var processedCount = 0
    amounts.forEach { amount ->
        if (amount < 0) {
            // `return@forEach` đóng vai trò giống hệt `continue`: chỉ bỏ qua phần tử âm này và đi tiếp phần tử sau!
            println("   -> [return@forEach ~ continue] Bo qua giao dich am khong hop le: $amount")
            return@forEach
        }
        if (amount > 100_000_000L) {
            // `return` không có nhãn bên trong inline lambda `.forEach` sẽ THOÁT HẲN khỏi hàm `processTransactionsWithForEach`!
            println("   -> [Non-local return ~ break function] Phat hien giao dich vuot han muc bao mat ($amount) -> Dung ngay ham!")
            check(processedCount == 2)
            return
        }
        processedCount++
        println("   -> Da hach toan thanh cong giao dich: $amount VND")
    }
    // Dòng này sẽ KHÔNG được in ra nếu đã dính lệnh `return` (Non-local) ở trên!
    println("   -> Da duyet xong toan bo danh sach.")
}
