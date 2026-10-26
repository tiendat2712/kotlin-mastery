package view

/**
 * ============================================================================
 * BÀI 02: CUSTOM GETTERS, SETTERS & TỪ KHÓA `field` (BACKING FIELD)
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / STACK vs HEAP / BYTECODE):
 * ----------------------------------------------------------------------------
 * A. Computed Property (Thuộc tính tính toán động - KHÔNG tốn bộ nhớ HEAP):
 *    Khi bạn viết:
 *      `val fullName: String get() = "$firstName $lastName"`
 *    Trình biên dịch Kotlin phát hiện getter này không hề tham chiếu đến giá trị
 *    lưu trữ riêng của `fullName`. Vì vậy, ở tầng Bytecode:
 *      - KHÔNG có biến `private String fullName` nào được cấp phát trên HEAP!
 *      - Chỉ có duy nhất hàm `public final String getFullName()` được sinh ra.
 *
 * B. Backing Field (`field`) & Cạm bẫy Đệ quy vô tận (`StackOverflowError`):
 *    Khi bạn viết custom `set(value)` để kiểm tra hợp lệ (validation) hoặc chuẩn hóa dữ liệu:
 *      ```kotlin
 *      var price: Double = 0.0
 *          set(value) {
 *              require(value >= 0)
 *              field = value // <-- ĐÚNG: ghi trực tiếp vào ô nhớ private trên HEAP
 *          }
 *      ```
 *    TẠI SAO KHÔNG ĐƯỢC VIẾT `this.price = value` BÊN TRONG `set(value)`?
 *    -> Bởi vì trong Kotlin, mọi phép gán `this.price = value` đều được biên dịch thành
 *       lời gọi hàm `this.setPrice(value)`. Nếu bên trong `setPrice` lại gọi `this.setPrice(value)`,
 *       chương trình sẽ tự gọi lại chính nó vô tận, làm tràn bộ nhớ STACK (`StackOverflowError`)!
 *    -> Từ khóa đặc biệt `field` (chỉ tồn tại bên trong `get()` và `set()`) cho phép
 *       truy cập thẳng vào biến `private double price` thực sự nằm trên HEAP.
 *
 * 2. SO SÁNH JAVA vs KOTLIN:
 * ----------------------------------------------------------------------------
 * - Trong Java: Ban đầu bạn có thể lười biếng tạo field lộ thiên hoặc getter/setter đơn giản,
 *   sau này khi cần thêm logic kiểm tra giá trị âm vào `setPrice()`, nếu lỡ cho truy cập field
 *   trực tiếp ở nơi khác thì phải sửa toàn bộ codebase.
 * - Trong Kotlin: Mọi truy cập `product.price = 100.0` ngay từ đầu đã đi qua setter ngầm định.
 *   Khi cần bổ sung validation vào `set(value)`, bạn chỉ sửa đúng 1 chỗ trong class mà
 *   KHÔNG làm gãy bất kỳ dòng code nào đang gọi `product.price = ...`!
 *
 * 3. SPRING KOTLIN CORNER:
 * ----------------------------------------------------------------------------
 * - Computed Properties (`val isExpired: Boolean get() = ...`, `val finalAmount: Double get() = ...`)
 *   cực kỳ phổ biến trong các Response DTO của Spring Boot REST API: когда Jackson serialize
 *   object sang JSON, Jackson quét tất cả các hàm `getXxx()` và tự động xuất thêm trường
 *   `"expired": false`, `"finalAmount": 90.0` vào JSON trả về cho Frontend mà không cần lưu thừa vào DB!
 */

private class ProductAccount(
    firstNameInit: String,
    lastNameInit: String,
    initialPrice: Double
) {
    var firstName: String = firstNameInit.trim()
        set(value) {
            field = value.trim()
        }

    var lastName: String = lastNameInit.trim()
        set(value) {
            field = value.trim()
        }

    // 1. Computed Property: Không có backing field trên HEAP, tính toán mỗi khi gọi
    val fullName: String
        get() = "$firstName $lastName"

    // 2. Custom Setter với từ khóa `field` (Backing Field)
    var price: Double = 0.0
        set(value) {
            require(value >= 0.0) { "Giá sản phẩm không được âm (nhận được: $value)" }
            println("    [Setter Hook] Đang cập nhật backing field `price` từ $field -> $value")
            // LƯU Ý CỰC QUAN TRỌNG:
            // Nếu viết `price = value` ở đây -> dịch thành `setPrice(value)` -> StackOverflowError!
            // Phải dùng `field = value` để ghi thẳng vào biến private trên HEAP.
            field = value
        }

    // 3. Private Setter: Đọc tự do từ bên ngoài, nhưng chỉ thay đổi qua hàm nội bộ
    var loyaltyPoints: Int = 0
        private set

    init {
        // Gán qua property để kích hoạt luôn validation của custom setter ngay từ lúc khởi tạo!
        this.price = initialPrice
    }

    fun recordPurchase(amountSpent: Double) {
        require(amountSpent > 0) { "Số tiền mua hàng phải dương" }
        val earned = (amountSpent / 10).toInt()
        loyaltyPoints += earned
    }
}

fun main() {
    println("--- ___ * START: Ex02CustomGettersAndSetters * ___ ---")

    // ========================================================================
    // 1. Computed Property (Không tốn Backing Field trên HEAP)
    // ========================================================================
    println("\n[1] Computed Property (`fullName` tự động cập nhật khi `firstName`/`lastName` đổi):")
    val account = ProductAccount("  Trinh  ", "  Dat  ", 250.0)
    println("  Họ tên ban đầu : '${account.fullName}' (đã tự động trim khoảng trắng)")

    account.firstName = "  Nguyen Van "
    println("  Sau khi đổi họ : '${account.fullName}'")

    // ========================================================================
    // 2. Custom Setter với Backing Field `field`
    // ========================================================================
    println("\n[2] Custom Setter với từ khóa `field` và kiểm tra hợp lệ:")
    account.price = 499.0
    println("  Giá hiện tại   : ${account.price}")

    try {
        println("  Thử gán giá âm : account.price = -50.0")
        account.price = -50.0
    } catch (e: IllegalArgumentException) {
        println("  -> Bắt được ngoại lệ đúng kỳ vọng: ${e.message}")
    }

    // ========================================================================
    // 3. Private Setter bảo vệ trạng thái nội bộ
    // ========================================================================
    println("\n[3] Private Setter (`loyaltyPoints`):")
    account.recordPurchase(155.0)
    account.recordPurchase(45.0)
    println("  Điểm tích lũy sau 2 giao dịch: ${account.loyaltyPoints} điểm")

    println("\n--- ___ * FINISH: Ex02CustomGettersAndSetters * ___ ---")
}
