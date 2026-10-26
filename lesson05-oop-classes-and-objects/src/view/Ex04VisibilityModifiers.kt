package view

/**
 * ============================================================================
 * BÀI 04: VISIBILITY MODIFIERS (PHẠM VI TRUY CẬP TRONG KOTLIN vs JAVA)
 * ============================================================================
 *
 * 1. BẢNG SO SÁNH TRỰC DIỆN JAVA (`lesson03-oop`) vs KOTLIN:
 * ----------------------------------------------------------------------------
 * +---------------+----------------------------------+-----------------------------------------+
 * | Modifier      | Java Core                        | Kotlin Core                             |
 * +---------------+----------------------------------+-----------------------------------------+
 * | Mặc định      | package-private (cùng package)   | `public` (mọi nơi đều thấy!)            |
 * | `public`      | Mọi nơi                          | Mọi nơi (không cần gõ từ khóa `public`) |
 * | `private`     | Nội bộ trong Class               | Nội bộ trong Class HOẶC nội bộ trong    |
 * |               | (Top-level class không private)  | cùng 1 file `.kt` (Top-level `private`) |
 * | `protected`   | Class con + CẢ CÙNG PACKAGE!     | CHỈ Class con (Subclass ONLY)!          |
 * |               | (Điểm yếu bảo mật của Java)      | Class cùng package nhưng không kế thừa  |
 * |               |                                  | sẽ KHÔNG truy cập được `protected`!     |
 * | `internal`    | (Không có)                       | Mọi nơi bên trong CÙNG 1 MODULE         |
 * |               |                                  | (cùng Gradle subproject / IntelliJ mod) |
 * +---------------+----------------------------------+-----------------------------------------+
 *
 * 2. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 * ----------------------------------------------------------------------------
 * - `private class` ở cấp độ file (Top-level):
 *   JVM Bytecode thực chất không có khái niệm class `private` ở top-level, nên `kotlinc`
 *   biên dịch nó thành package-private ở mức JVM nhưng trình biên dịch Kotlin khóa chặt
 *   không cho file `.kt` khác trong cùng package gọi tới.
 * - `internal` (Mức độ Module):
 *   Vì JVM Bytecode không có modifier `internal`, Kotlin dịch nó thành `public` ở Bytecode
 *   nhưng thực hiện **Name Mangling** (đổi tên hàm kèm tên module, ví dụ:
 *   `internal fun calculateFee()` -> `calculateFee$lesson05_oop_classes_and_objects()`)
 *   để ngăn tình trạng vô tình gọi nhầm từ module khác và tránh xung đột khi kế thừa!
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * Trong kiến trúc **Clean Architecture / Modular Monolith** với Spring Boot + Gradle Multi-Module:
 *   - `internal class JpaOrderRepositoryImpl` hoặc `internal fun mapToEntity(...)` là
 *     vũ khí tối thượng!
 *   - Trong Java, nếu bạn tách các package `controller`, `service`, `repository` bên trong
 *     module `order-module`, bạn buộc phải để class là `public` để `service` gọi được
 *     `repository` -> nhưng điều đó lại làm lộ `repository` ra cho các module khác
 *     (`payment-module`) chọc thẳng vào!
 *   - Trong Kotlin, bạn đánh dấu `internal`: các package bên trong `order-module` gọi nhau
 *     thoải mái, nhưng `payment-module` ở ngoài nhìn vào hoàn toàn bị ẩn!
 */

// 1. `public` là mặc định trong Kotlin (không cần viết từ khóa `public`)
open class PaymentProcessor(val merchantId: String) {

    // Chỉ truy cập được ngay bên trong class PaymentProcessor
    private val secretSigningKey: String = "HMAC-SHA256-KEY-9988"

    // CHỈ truy cập được từ chính PaymentProcessor và các class con kế thừa nó (`StripeProcessor`).
    // Khác với Java: Một hàm khác đứng cùng package `view` nhưng KHÔNG kế thừa PaymentProcessor
    // cũng KHÔNG được phép gọi `buildAuthHeader()`!
    protected fun buildAuthHeader(amount: Double): String {
        val signature = "${merchantId}_${amount}_${secretSigningKey.takeLast(4)}"
        return "Bearer $signature"
    }

    // Truy cập được ở bất kỳ file nào nằm trong module `lesson05-oop-classes-and-objects`
    internal fun auditInternalLog(message: String) {
        println("    [INTERNAL MODULE AUDIT] merchant=$merchantId | $message")
    }
}

// 2. `private` ở cấp độ File (Top-level private class):
// Chỉ tồn tại và hiển thị bên trong duy nhất file `Ex04VisibilityModifiers.kt` này!
private class StripePaymentProcessor(merchantId: String) : PaymentProcessor(merchantId) {

    fun chargeCard(amount: Double): String {
        // Hợp lệ: Class con gọi được hàm `protected` của class cha
        val authHeader = buildAuthHeader(amount)
        // Hợp lệ: Cùng module nên gọi được hàm `internal`
        auditInternalLog("Charging $$amount via Stripe")
        return "SUCCESS [Header=$authHeader]"
    }
}

fun main() {
    println("--- ___ * START: Ex04VisibilityModifiers * ___ ---")

    val stripe = StripePaymentProcessor(merchantId = "MERCHANT_VN_01")

    println("\n[1] Goi ham public (mac dinh) va internal trong cung module:")
    println("  Merchant ID (public) : ${stripe.merchantId}")
    stripe.auditInternalLog("Kiem tra ket noi cong thanh toan tu ham main()")

    println("\n[2] Class con truy cap phuong thuc `protected` cua class cha:")
    val receipt = stripe.chargeCard(amount = 199.99)
    println("  Ket qua thanh toan   : $receipt")

    // Thử mở comment dòng dưới sẽ thấy sự khác biệt giữa `protected` của Kotlin và Java:
    // Dù `main()` nằm CÙNG PACKAGE `view` với `PaymentProcessor`, nhưng vì `main()` không phải
    // là class con của `PaymentProcessor`, nên Kotlin CẤM gọi `buildAuthHeader`:
    // stripe.buildAuthHeader(100.0) // ERROR: Cannot access 'buildAuthHeader': it is protected in 'PaymentProcessor'

    println("\n--- ___ * FINISH: Ex04VisibilityModifiers * ___ ---")
}
