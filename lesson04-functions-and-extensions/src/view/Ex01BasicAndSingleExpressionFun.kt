package view

/**
 * =====================================================================
 * LESSON 04 - EX01: CÚ PHÁP HÀM CƠ BẢN & SINGLE-EXPRESSION FUNCTIONS
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Cú pháp khai báo hàm trong Kotlin theo phong cách Pascal:
 *      `fun tenHam(thamSo1: Kieu1, thamSo2: Kieu2): KieuTraVe { ... }`
 *    - **Đặc điểm cực kỳ an toàn của tham số hàm trong Kotlin**:
 *      + Mọi tham số truyền vào hàm (`amount: Double`) mặc định luôn là **`val` (Read-only)** trên Stack!
 *      + Trong Java, bạn có thể vô tình gán lại `amount = 0;` bên trong thân hàm nếu quên từ khóa `final`.
 *        Trong Kotlin, việc gán lại tham số hàm bị **CẤM HOÀN TOÀN** lúc biên dịch!
 *    - **Single-Expression Function (`fun square(x: Int) = x * x`)**:
 *      + Khi thân hàm chỉ gồm 1 biểu thức duy nhất (kể cả biểu thức `if-else` hay `when`), bạn có thể bỏ cặp
 *        ngoặc nhọn `{ ... }`, bỏ từ khóa `return`, và nối trực tiếp bằng dấu `=`.
 *      + Dưới tầng Bytecode, `fun square(x: Int) = x * x` được biên dịch ra phương thức JVM giống hệt 100%
 *        với hàm có `{ return x * x }`!
 *
 * 2. KHI NÀO NÊN GHI RÕ KIỂU TRẢ VỀ (EXPLICIT RETURN TYPE)?
 *    - Với hàm có thân `{ ... }` (Block body): bắt buộc phải ghi rõ `: KieuTraVe` (trừ khi trả về `Unit`).
 *    - Với hàm `= ...` (Single-expression): Kotlin cho phép ẩn kiểu trả về nhờ Type Inference, NHƯNG:
 *      **Best Practice trong dự án thực tế / Spring Boot**: Luôn ghi rõ kiểu trả về cho các hàm `public`
 *      tại tầng `@RestController` và `@Service` để hợp đồng API (API Contract) rõ ràng tuyệt đối và tránh
 *      vô tình làm lộ kiểu nội bộ (Internal Type) khi sửa biểu thức bên phải dấu `=`!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong `@RestController`, các endpoint đơn giản ủy quyền thẳng xuống `@Service` thường được viết dưới
 *      dạng Single-Expression Function cực kỳ thanh thoát:
 *      ```kotlin
 *      @GetMapping("/{id}")
 *      fun getUser(@PathVariable id: Long): UserDto = userService.findById(id)
 *      ```
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex01BasicAndSingleExpressionFun * ___ ---\n")

    // =================================================================
    // 1. Hàm tiêu chuẩn có thân khối `{ ... }` (Block-body function)
    // =================================================================
    println("1. [Standard Block-body Function] Hàm có thân `{ ... }` và kiểu trả về tường minh:")
    val grossPrice = 1_500_000.0
    val taxAmount = calculateTax(amount = grossPrice)
    println("   -> Giá trước thuế: $grossPrice | Thuế VAT (10%): $taxAmount\n")

    // =================================================================
    // 2. Hàm trả về `Unit` (Tương đương `void` trong Java, mặc định lược bỏ `: Unit`)
    // =================================================================
    println("2. [Unit Return Type] Hàm không trả về giá trị hữu ích (mặc định là `: Unit`):")
    printInvoiceHeader("INV-2026-0088")
    println()

    // =================================================================
    // 3. Single-Expression Functions (`fun ... = ...`)
    // =================================================================
    println("3. [Single-Expression Function] Hàm 1 biểu thức dùng dấu `=`:")
    println("   -> square(12) = ${square(12)}")
    println("   -> isVipCustomer(points = 1500) = ${isVipCustomer(1500)}")
    println("   -> classifyShippingZone(\"Hà Nội\")   = ${classifyShippingZone("Hà Nội")}")
    println("   -> classifyShippingZone(\"Đà Nẵng\") = ${classifyShippingZone("Đà Nẵng")}")

    println("\n--- ___ * END: Ex01BasicAndSingleExpressionFun * ___ ---")
}

// 1. Hàm chuẩn có thân khối `{ ... }`
fun calculateTax(amount: Double): Double {
    // amount = 0.0 // ❌ LỖI BIÊN DỊCH: Val cannot be reassigned (Tham số hàm luôn là `val`!)
    val vatRate = 0.10
    return amount * vatRate
}

// 2. Hàm trả về `Unit` (không cần ghi `: Unit`)
fun printInvoiceHeader(invoiceCode: String) {
    println("   -> [INVOICE HEADER] Mã hóa đơn: $invoiceCode (Trạng thái: ĐÃ XUẤT)")
}

// 3a. Single-expression function ngắn gọn
fun square(x: Int): Int = x * x

fun isVipCustomer(points: Int): Boolean = points >= 1_000

// 3b. Single-expression function kết hợp biểu thức `when` (rất phổ biến trong Spring Service/Mapper!)
fun classifyShippingZone(city: String): String = when (city.trim()) {
    "Hà Nội", "Hải Phòng", "Quảng Ninh" -> "MIEN_BAC_EXPRESS"
    "Hồ Chí Minh", "Bình Dương", "Đồng Nai" -> "MIEN_NAM_EXPRESS"
    else -> "LIEN_TINH_TIEU_CHUAN"
}
