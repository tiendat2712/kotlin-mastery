package view

import bean.AccountConfig
import bean.Address
import bean.Customer

/**
 * =====================================================================
 * LESSON 02 - EX05: SCOPE FUNCTIONS PHẦN 2 — `apply`, `run`, `with` (CONTEXT: `this`)
 * =====================================================================
 *
 * 1. BẢN CHẤT (RECEIVER `this` TRONG LAMBDA):
 *    - Khác với `let` và `also` (truyền đối tượng vào như tham số `it`), 3 hàm `apply`, `run`, `with`
 *      sử dụng **Lambda with Receiver** (`T.() -> R`), nghĩa là bên trong cặp ngoặc nhọn `{ ... }`,
 *      con trỏ ngữ cảnh chính là **`this`** (và bạn có thể lược bỏ chữ `this.` để gọi thẳng các thuộc tính/phương thức!).
 *    - Cả 3 hàm này cũng đều là **`inline fun`**, hoàn toàn không phát sinh chi phí tạo Lambda Object trên Heap.
 *
 * 2. BẢNG TỔNG HỢP SO SÁNH 5 SCOPE FUNCTIONS TRONG KOTLIN (BẢNG VÀNG CẦN NHỚ):
 *    +----------+------------------+------------------------+----------------+------------------------------------------------+
 *    | Hàm      | Tham chiếu Object| Giá trị trả về         | Dạng hàm       | Trường hợp sử dụng chuẩn (Idiomatic Use Case)  |
 *    +----------+------------------+------------------------+----------------+------------------------------------------------+
 *    | `let`    | `it`             | Kết quả Lambda         | Extension `T.` | Kiểm tra `?.let` tránh null; map `Entity->DTO` |
 *    | `also`   | `it`             | Chính Object gốc (`T`) | Extension `T.` | Ghi log, audit, debug ở giữa chuỗi pipeline    |
 *    | `apply`  | `this`           | Chính Object gốc (`T`) | Extension `T.` | Cấu hình các thuộc tính khả biến (`var`/setter)|
 *    | `run`    | `this`           | Kết quả Lambda         | Extension `T.` | Vừa cấu hình Object vừa tính ra kết quả mới    |
 *    | `with`   | `this`           | Kết quả Lambda         | Hàm thường     | Gọi hàng loạt hàm trên 1 Object Non-Null có sẵn|
 *    +----------+------------------+------------------------+----------------+------------------------------------------------+
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Dùng `apply` khi cấu hình các đối tượng Java truyền thống chỉ có Setter (như `JavaMailMessage`,
 *      `CorsConfiguration`, `HttpHeaders`, hoặc JPA Entity trước khi save).
 *    - Dùng `with` khi xây dựng báo cáo hoặc gộp nhiều lời gọi phương thức trên cùng một đối tượng cấu hình.
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex05ScopeFunctions_ApplyRunWith * ___ ---\n")

    // =================================================================
    // 1. `apply`: Cấu hình đối tượng (Object Configuration) và trả về chính nó
    // =================================================================
    println("1. [apply] Khởi tạo và cấu hình các thuộc tính `var` của đối tượng (trả về chính Object):")
    // Bên trong `apply`, ngữ cảnh là `this` (tức đối tượng AccountConfig vừa tạo).
    // Ta gán trực tiếp `host = ...`, `port = ...` mà không cần lặp lại tên biến `config.`!
    val productionDbConfig: AccountConfig = AccountConfig().apply {
        host = "db.prod.kotlinjourney.internal"
        port = 5432
        useSsl = true
    }
    println("   -> Cấu hình sau khi `.apply`: $productionDbConfig\n")

    // =================================================================
    // 2. `run`: Kết hợp truy cập thuộc tính (`this`) và tính toán ra kết quả mới
    // =================================================================
    println("2. [run] Vừa thao tác với thuộc tính (`this`), vừa trả về kết quả ở dòng cuối:")
    val jdbcConnectionUrl: String = productionDbConfig.run {
        val protocol = if (useSsl) "jdbc:postgresql+ssl" else "jdbc:postgresql"
        // Dòng cuối cùng là chuỗi URL trả về:
        "$protocol://$host:$port/orders_db"
    }
    println("   -> Chuỗi kết nối tính từ `.run`: $jdbcConnectionUrl")

    // Cặp bài trùng `?.run { ... } ?: run { ... }` để xử lý cả nhánh có dữ liệu và nhánh null:
    val nullableCustomer: Customer? = null
    val greetingMessage = nullableCustomer?.run {
        "Xin chào khách hàng VIP: $name (ID: $id)"
    } ?: run {
        println("   -> [Fallback run] Không tìm thấy thông tin khách hàng, tạo thông điệp mặc định...")
        "Xin chào Quý khách vãng lai!"
    }
    println("   -> Thông điệp: \"$greetingMessage\"\n")

    // =================================================================
    // 3. `with(obj) { ... }`: Nhóm nhiều thao tác trên một đối tượng Non-Null đã có sẵn
    // =================================================================
    println("3. [with] Gọi nhiều thuộc tính/phương thức trên một đối tượng Non-Null mà không lặp tên biến:")
    val verifiedCustomer = Customer(
        id = "CUST-900",
        name = "Lương Thế Vinh",
        email = "vinh.luong@math.vn",
        address = Address(street = "100 Trần Hưng Đạo", city = "Nam Định")
    )

    val shippingLabel: String = with(verifiedCustomer) {
        // Bên trong `with`, `this` chính là `verifiedCustomer`
        buildString {
            appendLine("   +------------------------------------------------+")
            appendLine("   | PHIẾU GIAO HÀNG NHANH                          |")
            appendLine("   | Mã KH   : $id")
            appendLine("   | Người nhận: $name")
            appendLine("   | Liên hệ : ${email ?: "Không có"}")
            appendLine("   | Địa chỉ : ${address?.street}, ${address?.city}")
            append("   +------------------------------------------------+")
        }
    }
    println(shippingLabel)

    println("\n--- ___ * END: Ex05ScopeFunctions_ApplyRunWith * ___ ---")
}
