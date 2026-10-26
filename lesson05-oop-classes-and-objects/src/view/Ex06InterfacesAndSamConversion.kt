package view

/**
 * ============================================================================
 * BÀI 06: INTERFACES, DIAMOND INHERITANCE CONFLICT & SAM (`fun interface`)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 * ----------------------------------------------------------------------------
 * A. Thuộc tính trừu tượng trong Interface (Abstract Properties in Interfaces):
 *    Khác với Java (biến trong interface luôn là hằng số `public static final`),
 *    Interface trong Kotlin có thể khai báo **Property trừu tượng** hoặc **Computed Property**:
 *      ```kotlin
 *      interface Taxable {
 *          val taxRate: Double // Không tạo field trên HEAP! Thực chất là khai báo `getTaxRate()`
 *          val taxDescription: String get() = "VAT ${(taxRate * 100).toInt()}%"
 *      }
 *      ```
 *    Class thực thi (`implements`) có thể override `val taxRate` ngay trong Primary Constructor!
 *
 * B. Giải quyết xung đột Đa kế thừa (Diamond Problem):
 *    Giống bài toán ở `D:\Java_Journey\lesson04-oop-features\src\view\inheritance\multiple\iinterface`,
 *    khi một class thực thi 2 interfaces (`PaymentGatewayA`, `PaymentGatewayB`) mà cả hai đều có
 *    default method trùng tên `fun processRefund(orderId: String)`:
 *    - Trong Java: Gọi `PaymentGatewayA.super.processRefund(orderId)`
 *    - Trong Kotlin: Gọi `super<PaymentGatewayA>.processRefund(orderId)` (cú pháp Angle Brackets
 *      rõ ràng, nhất quán với Generics).
 *
 * C. Functional Interface (`fun interface` - SAM Conversion):
 *    Trong Java 8+, bạn dùng `@FunctionalInterface` cho interface có 1 hàm trừu tượng duy nhất.
 *    Trong Kotlin, nếu bạn khai báo `interface DiscountPolicy` thông thường, bạn KHÔNG thể
 *    khởi tạo bằng Lambda `DiscountPolicy { ... }` mà phải viết `object : DiscountPolicy { ... }`.
 *    Để bật tính năng **SAM (Single Abstract Method) Conversion** cho Kotlin interface,
 *    hãy thêm từ khóa `fun` ở trước: **`fun interface DiscountPolicy`**!
 *
 * 2. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * - Trong Spring Security / Spring Web, rất nhiều cấu hình dùng SAM interface (như
 *   `AuthenticationManager`, `RequestMatcher`, `Converter<S, T>`).
 * - Khi tự thiết kế các Strategy Pattern trong Domain (ví dụ `FeeCalculator`, `PricingPolicy`),
 *   sử dụng `fun interface` giúp bạn viết Unit Test hoặc khai báo `@Bean` trong `@Configuration`
 *   cực kỳ gọn nhẹ chỉ bằng 1 biểu thức Lambda!
 */

// 1. Interface có cả abstract property, computed property và default method
private interface Taxable {
    val taxRate: Double // Abstract property -> yêu cầu class con cung cấp getter

    val taxDescription: String
        get() = "Thuế suất áp dụng: ${(taxRate * 100).toInt()}%"

    fun calculateTax(subtotal: Double): Double = subtotal * taxRate
}

// 2. Minh họa Diamond Inheritance Conflict (2 interface có cùng hàm default `auditTransaction`)
private interface DomesticGateway {
    fun auditTransaction(txId: String): String = "[NAPAS-VN] Đã đối soát giao dịch nội địa #$txId"
}

private interface InternationalGateway {
    fun auditTransaction(txId: String): String = "[VISA/MASTER] Đã đối soát giao dịch quốc tế #$txId"
}

private class UnifiedPaymentService(
    // Override abstract property `taxRate` của interface `Taxable` ngay trong Primary Constructor!
    override val taxRate: Double = 0.08
) : Taxable, DomesticGateway, InternationalGateway {

    // Khi 2 interface cha cùng có hàm `auditTransaction(txId)`, trình biên dịch BẮT BUỘC
    // class con phải override để chỉ rõ muốn dùng logic nào (hoặc kết hợp cả hai)
    override fun auditTransaction(txId: String): String {
        val domesticLog = super<DomesticGateway>.auditTransaction(txId)
        val internationalLog = super<InternationalGateway>.auditTransaction(txId)
        return "$domesticLog && $internationalLog"
    }
}

// 3. SAM (Single Abstract Method) với `fun interface`
private fun interface DiscountPolicy {
    fun applyDiscount(originalAmount: Double): Double
}

fun main() {
    println("--- ___ * START: Ex06InterfacesAndSamConversion * ___ ---")

    val paymentService = UnifiedPaymentService(taxRate = 0.10)

    // ========================================================================
    // 1. Abstract Property & Default Method trong Interface
    // ========================================================================
    println("\n[1] Abstract Property & Computed Property trong Interface:")
    val subtotal = 1_000.0
    println("  Mô tả thuế : ${paymentService.taxDescription}")
    println("  Tiền thuế  : ${paymentService.calculateTax(subtotal)} trên tổng $subtotal")

    // ========================================================================
    // 2. Giải quyết xung đột Đa kế thừa (`super<Interface>.method()`)
    // ========================================================================
    println("\n[2] Giải quyết Diamond Problem với `super<Interface>.method()`:")
    println("  Kết quả    : ${paymentService.auditTransaction("TX-2026-999")}")

    // ========================================================================
    // 3. Khởi tạo `fun interface` (SAM Conversion) bằng Lambda ngắn gọn
    // ========================================================================
    println("\n[3] Functional Interface (`fun interface`) & SAM Conversion:")
    val blackFridayPolicy = DiscountPolicy { amount -> amount * 0.75 } // Giảm 25%
    val vipFlatPolicy = DiscountPolicy { amount -> (amount - 150.0).coerceAtLeast(0.0) }

    println("  Giá gốc $subtotal -> Sau BlackFriday : ${blackFridayPolicy.applyDiscount(subtotal)}")
    println("  Giá gốc $subtotal -> Sau VIP Voucher : ${vipFlatPolicy.applyDiscount(subtotal)}")

    println("\n--- ___ * FINISH: Ex06InterfacesAndSamConversion * ___ ---")
}
