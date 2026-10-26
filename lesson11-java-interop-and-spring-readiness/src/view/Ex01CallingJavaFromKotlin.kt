package view

import legacy.LegacyPaymentGateway

/**
 * BÀI 01: GỌI CODE JAVA TỪ KOTLIN — PLATFORM TYPES (`String!`), SAM CONVERSION & `-Xjsr305=strict`
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE / PLATFORM TYPES):
 *    - **Platform Types (`T!`, ví dụ `String!`, `List<String!>!`)**:
 *      + Khi Kotlin gọi một hàm Java KHÔNG gắn annotation `@Nullable` / `@NotNull`
 *        (như `LegacyPaymentGateway.findTransactionToken(orderId)`), trình biên dịch Kotlin **KHÔNG THỂ BIẾT**
 *        hàm Java đó có thể trả về `null` hay không!
 *      + Nếu bắt mọi kết quả trả về từ Java đều là `String?` thì lập trình viên sẽ phải viết `?.` hoặc `!!`
 *        ở mọi dòng code gọi thư viện chuẩn Java. Vì vậy, Kotlin thiết kế **Platform Type `String!`**
 *        (dấu `!` chỉ hiện trong thông báo của IDE/Compiler, không viết trực tiếp trong code):
 *        * Bạn có quyền coi nó là `String` (Non-null) HOẶC `String?` (Nullable).
 *        * **QUY TẮC VÀNG (Defensive Assignment)**:
 *          - Nếu bạn KHÔNG khai báo kiểu: `val token = gateway.findTransactionToken("MISSING_01")`
 *            -> biến `token` mang kiểu `String!`. Nếu nó chứa `null`, lỗi `NullPointerException` sẽ chỉ nổ ra
 *            muộn màng ở tận nơi bạn gọi `token.length`!
 *          - Nếu bạn khai báo `val token: String = gateway.findTransactionToken("MISSING_01")`
 *            -> Trình biên dịch Kotlin tự động chèn lệnh kiểm tra `Intrinsics.checkNotNullExpressionValue(...)`
 *            **NGAY TẠI DÒNG GÁN** để bắt lỗi sớm (Fail-fast)!
 *          - Nếu hàm Java có khả năng trả về `null`, hãy **LUÔN khai báo tường minh kiểu `String?`**:
 *            `val token: String? = gateway.findTransactionToken("MISSING_01")`!
 *
 *    - **JavaBeans Property Syntax & SAM Conversion**:
 *      + Các cặp hàm `getMerchantCode()` / `setMerchantCode(...)` trong Java được Kotlin tự động ánh xạ
 *        thành thuộc tính `gateway.merchantCode`.
 *      + Các Functional Interface (SAM - Single Abstract Method) của Java như `PaymentCallback` tự động
 *        nhận trực tiếp biểu thức Lambda của Kotlin `{ txId, success, msg -> ... }`.
 *
 * 2. SPRING KOTLIN CORNER — CỜ BIÊN DỊCH `-Xjsr305=strict`:
 *    - Toàn bộ mã nguồn của **Spring Framework / Spring Boot / Spring Data** (viết bằng Java) đều được
 *      đội ngũ Spring gắn đầy đủ các annotation `@NonNullApi`, `@NonNull`, `@Nullable` (chuẩn JSR-305 / Jakarta).
 *    - Khi bạn bật cờ `-Xjsr305=strict` trong `build.gradle.kts` (như project `D:\Kotlin_Journey` này đã bật sẵn!):
 *      Trình biên dịch Kotlin sẽ đọc các annotation đó của Spring và **XÓA BỎ Platform Type `T!`**,
 *      biến chúng thành kiểu `T` (Non-null) hoặc `T?` (Nullable) chính quy ngay lúc Compile-time!
 */

fun main() {
    println("--- ___ * START EX01: CALLING JAVA FROM KOTLIN & PLATFORM TYPES * ___ ---\n")

    // =========================================================================
    // 1. TRUY CẬP JAVABEANS GETTER/SETTER NHƯ PROPERTY CỦA KOTLIN
    // =========================================================================
    println("1. Khởi tạo class Java `LegacyPaymentGateway` và dùng cú pháp Property:")
    val gateway = LegacyPaymentGateway("VN_PAY_PROD")
    println("  - Đọc `gateway.merchantCode` (gọi getMerchantCode() bên dưới): ${gateway.merchantCode}")

    gateway.merchantCode = "NAPAS_GATEWAY_V2" // Gọi setMerchantCode(...) bên dưới
    println("  - Sau khi gán `gateway.merchantCode = ...`: ${gateway.merchantCode}")

    // =========================================================================
    // 2. PLATFORM TYPES (`String!`) — CÁCH PHÒNG VỆ AN TOÀN KHI GỌI HÀM JAVA
    // =========================================================================
    println("\n2. Xử lý Platform Type (`String!`) an toàn tại ranh giới Java -> Kotlin:")

    // Cách AN TOÀN NHẤT: Khai báo tường minh kiểu Nullable `String?` khi nhận kết quả từ hàm Java chưa rõ nullability
    val validToken: String? = gateway.findTransactionToken(" O001 ")
    val missingToken: String? = gateway.findTransactionToken("MISSING_O999")

    println("  - Token hợp lệ (dùng safe call `?.length`): $validToken (độ dài = ${validToken?.length})")
    println("  - Token bị thiếu (xử lý bằng Elvis `?:`): ${missingToken ?: "[KHÔNG TÌM THẤY TOKEN]"}")

    // Minh họa Fail-fast khi gán Platform Type có giá trị `null` vào biến Non-null `String`:
    try {
        // Ngay tại phép gán này, Kotlin Bytecode gọi Intrinsics.checkNotNullExpressionValue() và ném NPE ngay!
        val strictNonNullToken: String = gateway.findTransactionToken("MISSING_O999")
        println("  - Dòng này không chạy tới: $strictNonNullToken")
    } catch (e: NullPointerException) {
        println("  - [Fail-fast Check] Bắt được NPE ngay tại dòng gán vào biến Non-null `String`: ${e.message}")
    }

    // =========================================================================
    // 3. JAVA COLLECTIONS & JAVA SAM CONVERSION (TRUYỀN LAMBDA TRỰC TIẾP)
    // =========================================================================
    println("\n3. Làm việc với `List<String>` từ Java và Java SAM Interface (`PaymentCallback`):")

    // Khai báo là `List<String>` (read-only view trong Kotlin) để bảo vệ tính bất biến
    val currencies: List<String> = gateway.supportedCurrencies
    println("  - Danh sách tiền tệ hỗ trợ (`gateway.supportedCurrencies`): $currencies")

    // Truyền trực tiếp Trailing Lambda vào hàm Java nhận `PaymentCallback` (SAM Conversion)
    gateway.executeCharge("O001", 504_000L) { txId, success, message ->
        println("  - [SAM Callback Thành công] txId=$txId, success=$success, msg='$message'")
    }

    gateway.executeCharge("O002", -100L) { txId, success, message ->
        println("  - [SAM Callback Thất bại] txId=$txId, success=$success, msg='$message'")
    }

    println("\n--- ___ * END EX01 * ___ ---")
}
