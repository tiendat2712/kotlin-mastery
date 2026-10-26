package view

/**
 * =====================================================================
 * LESSON 03 - EX01: KIỂU BOOLEAN, TOÁN TỬ LOGIC & BẪY NULLABLE BOOLEAN (`Boolean?`)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - `Boolean` (Non-Null) dịch ra kiểu nguyên thủy `boolean` trên Stack (chỉ có 2 trạng thái: `true` hoặc `false`).
 *    - `Boolean?` (Nullable) dịch ra `java.lang.Boolean` trên Heap và có tới **3 trạng thái (Three-valued logic)**:
 *      `true`, `false`, và `null`!
 *    - **Cái bẫy kinh điển trong Java (Auto-unboxing NPE)**:
 *      + Trong Java, nếu một trường DTO là `Boolean isSubscribed = null;` (Wrapper class), khi bạn viết:
 *        `if (dto.getIsSubscribed()) { ... }`
 *        Trình biên dịch Java âm thầm tự động unbox bằng cách gọi `dto.getIsSubscribed().booleanValue()` ->
 *        **NỔ `NullPointerException` NGAY LẬP TỨC LÚC RUNTIME!**
 *      + Trong Kotlin, `if (isSubscribed)` với `isSubscribed: Boolean?` bị **CẤM BIÊN DỊCH**!
 *        Cách viết chuẩn Idiomatic Kotlin là:
 *        * `if (isSubscribed == true)` -> Chỉ vào nhánh `if` khi thực sự là `true` (nếu `false` hoặc `null` thì bỏ qua).
 *        * Hoặc `if (isSubscribed ?: false)` -> Dùng Elvis cung cấp giá trị mặc định.
 *
 * 2. QUY ƯỚC ĐẶT TÊN BOOLEAN & SINH GETTER CHO JAVA INTEROP:
 *    - Đặt tên biến Boolean luôn bắt đầu bằng tiền tố khẳng định: `isActive`, `hasPermission`, `canExecute`, `shouldRetry`.
 *    - Đặc biệt dưới tầng Bytecode:
 *      + Nếu thuộc tính tên là `val active: Boolean` -> Kotlin sinh hàm getter là `getActive()`.
 *      + Nếu thuộc tính bắt đầu bằng chữ `is` như `val isActive: Boolean` -> Kotlin giữ nguyên tên getter là `isActive()`
 *        (và setter là `setActive(...)`), khớp 100% với chuẩn JavaBeans của Jackson JSON và Spring Data!
 *
 * 3. TOÁN TỬ LOGIC NGẮN MẠCH (`&&`, `||`) VS ĐÁNH GIÁ ĐỦ (`and`, `or`, `xor`):
 *    - `&&`, `||`: Short-circuit (nếu vế trái đã quyết định kết quả thì KHÔNG chạy vế phải).
 *    - `and`, `or`, `xor`: Eager evaluation (luôn tính toán cả 2 vế). Trong đó `xor` (Exclusive OR — chỉ đúng khi
 *      2 vế khác nhau) rất hữu ích trong kiểm tra nghiệp vụ loại trừ!
 * =====================================================================
 */

private data class SubscriptionRequestDto(
    val userId: String,
    val isAutoRenew: Boolean?, // Trường tùy chọn trong JSON gửi lên (có thể bị bỏ trống = null)
    val hasPromoCode: Boolean?
)

fun main() {
    println("--- ___ * START: Ex01BooleansAndNullableBoolean * ___ ---\n")

    // =================================================================
    // 1. Quy ước đặt tên Boolean & Toán tử logic (&&, ||, !, xor)
    // =================================================================
    println("1. [Boolean Naming & Logical Operators] Quy ước đặt tên và toán tử logic:")
    val isActive = true
    val hasAdminRole = false
    val canApproveOrder = isActive && !hasAdminRole

    println("   -> isActive = $isActive | hasAdminRole = $hasAdminRole | canApproveOrder = $canApproveOrder")

    // Minh họa toán tử `xor` (Exclusive OR): Khách hàng chỉ được chọn 1 trong 2 hình thức nhận hàng
    // (hoặc giao tận nơi, hoặc tự đến cửa hàng lấy — không được chọn cả 2, cũng không được bỏ trống cả 2)
    val chooseHomeDelivery = true
    val chooseStorePickup = false
    val isValidDeliveryOption = chooseHomeDelivery xor chooseStorePickup
    println("   -> Kiểm tra chọn đúng 1 phương thức nhận hàng (true xor false): $isValidDeliveryOption\n")

    // =================================================================
    // 2. Bẫy Nullable Boolean (`Boolean?`) và Idiom `== true` trong Kotlin
    // =================================================================
    println("2. [Nullable Boolean Trap] Xử lý biến `Boolean?` (3 trạng thái: true, false, null):")
    val req1 = SubscriptionRequestDto(userId = "U-01", isAutoRenew = true, hasPromoCode = null)
    val req2 = SubscriptionRequestDto(userId = "U-02", isAutoRenew = null, hasPromoCode = false)

    for (req in listOf(req1, req2)) {
        // ❌ LỖI BIÊN DỊCH nếu viết như Java:
        // if (req.isAutoRenew) { ... } // Type mismatch: inferred type is Boolean? but Boolean was expected

        // ✅ CÁCH 1 (Idiomatic Kotlin khuyên dùng): So sánh trực tiếp `== true`
        if (req.isAutoRenew == true) {
            println("   -> [${req.userId}] Đã BẬT gia hạn tự động (isAutoRenew == true)")
        } else {
            // Vào đây khi isAutoRenew là `false` HOẶC `null`
            println("   -> [${req.userId}] KHÔNG gia hạn tự động (giá trị thực tế: ${req.isAutoRenew})")
        }

        // ✅ CÁCH 2: Dùng toàn bộ 3 nhánh với `when` nếu muốn phân biệt rõ `true`, `false` và `null`
        val promoStatus = when (req.hasPromoCode) {
            true -> "Có áp dụng mã giảm giá"
            false -> "Khách hàng chọn không dùng mã"
            null -> "Client không gửi trường hasPromoCode lên"
        }
        println("      Trạng thái mã giảm giá: $promoStatus")
    }

    println("\n--- ___ * END: Ex01BooleansAndNullableBoolean * ___ ---")
}
