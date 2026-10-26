package view

import bean.Address
import bean.Customer

/**
 * =====================================================================
 * LESSON 02 - EX04: SCOPE FUNCTIONS PHẦN 1 — `let` VÀ `also` (CONTEXT: `it`)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Kotlin cung cấp 5 **Scope Functions** (`let`, `also`, `apply`, `run`, `with`) cho phép thực thi
 *      một khối lệnh trong ngữ cảnh của một đối tượng.
 *    - Cả 5 hàm này đều được đánh dấu bằng từ khóa **`inline`** trong thư viện chuẩn của Kotlin!
 *      + Nghĩa là gì? Khi biên dịch ra JVM Bytecode, trình biên dịch **sao chép thẳng nội dung khối lệnh**
 *        vào nơi gọi hàm, **KHÔNG tạo ra đối tượng Lambda ẩn danh trên Heap**!
 *      + Hiệu năng bằng 100% so với viết lệnh `if (x != null)` truyền thống trong Java!
 *
 * 2. SO SÁNH `let` VS `also` (ĐỀU DÙNG THAM SỐ `it` ĐỂ TRỎ TỚI ĐỐI TƯỢNG):
 *    - Điểm chung: Bên trong cặp ngoặc nhọn `{ ... }`, đối tượng gốc được truy cập thông qua tên mặc định là `it`
 *      (hoặc bạn có thể đặt tên tường minh như `{ email -> ... }`).
 *    - Điểm khác biệt cốt lõi nằm ở **GIÁ TRỊ TRẢ VỀ (Return Value)**:
 *      + **`let`**: Trả về **kết quả của dòng cuối cùng trong Lambda (Lambda Result)**.
 *        * Ứng dụng 1: Thực thi khối code chỉ khi biến khác `null` (`customer?.email?.let { sendEmail(it) }`).
 *        * Ứng dụng 2: Biến đổi (Map) một đối tượng đơn lẻ từ kiểu `A` sang kiểu `B` (`entity.let { toDto(it) }`).
 *      + **`also`** ("và ngoài ra cũng làm thêm việc này..."): Trả về **chính đối tượng gốc (Context Object)**.
 *        * Ứng dụng: Thực hiện tác vụ phụ (Side-effect) như ghi Log, Audit, hoặc Validate ở giữa một chuỗi
 *          gọi hàm mà không làm đứt mạch dữ liệu!
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong `@Service` của Spring Boot:
 *      ```kotlin
 *      fun createCustomer(req: CreateCustomerRequest): CustomerDto {
 *          return customerRepository.save(req.toEntity())
 *              .also { logger.info("Đã lưu khách hàng mới với ID={}", it.id) }
 *              .let { it.toDto() }
 *      }
 *      ```
 * =====================================================================
 */

private data class CustomerSummaryDto(
    val customerId: String,
    val displayTitle: String,
    val cityOrDefault: String
)

fun main() {
    println("--- ___ * START: Ex04ScopeFunctions_LetAlso * ___ ---\n")

    val activeCustomer = Customer(
        id = "CUST-501",
        name = "Đặng Quốc Khánh",
        email = "khanh.dang@enterprise.vn",
        address = Address(street = "88 Láng Hạ", city = "Hà Nội")
    )

    val guestCustomer = Customer(
        id = "CUST-502",
        name = "Khách Vãng Lai",
        email = null,
        address = null
    )

    // =================================================================
    // 1. Dùng `?.let { ... }` để thay thế `if (x != null)`
    // =================================================================
    println("1. [Safe Call + let] Chỉ thực thi khối lệnh khi giá trị khác null:")
    // Bên trong `let`, tham số `it` đã được thu hẹp kiểu thành `String` (Non-Null)!
    activeCustomer.email?.let { validEmail ->
        println("   -> [CUST-501] Đang gửi mã OTP tới email hợp lệ: $validEmail")
    }

    guestCustomer.email?.let { validEmail ->
        // Khối này sẽ bị bỏ qua hoàn toàn vì guestCustomer.email là null
        println("   -> [CUST-502] Đang gửi mã OTP tới: $validEmail")
    } ?: println("   -> [CUST-502] Khách hàng chưa có email, bỏ qua bước gửi OTP.\n")

    // =================================================================
    // 2. Dùng `let` để biến đổi (Transform) một đối tượng đơn lẻ sang kiểu mới
    // =================================================================
    println("2. [Transform with let] Biến đổi Domain Entity sang DTO trả về kết quả cuối Lambda:")
    val summaryDto: CustomerSummaryDto = activeCustomer.let { cust ->
        // Dòng cuối cùng trong khối `let` chính là giá trị trả về của toàn bộ biểu thức!
        CustomerSummaryDto(
            customerId = cust.id,
            displayTitle = "${cust.name.uppercase()} (${cust.email ?: "No Email"})",
            cityOrDefault = cust.address?.city ?: "Chưa cập nhật"
        )
    }
    println("   -> Kết quả biến đổi DTO: $summaryDto\n")

    // =================================================================
    // 3. Dùng `also` để chèn Side-effects (Logging / Auditing / Validation)
    // =================================================================
    println("3. [Side-effects with also] Ghi Audit Log mà vẫn giữ nguyên đối tượng trả về:")

    val registeredDto: CustomerSummaryDto = Customer(
        id = "CUST-777",
        name = "Vũ Hoàng Yến",
        email = "yen.vu@spring.io",
        address = Address(street = "02 Hải Triều", city = "TP.HCM")
    )
        .also { println("   -> [AUDIT STEP 1] Đã khởi tạo đối tượng Customer trên bộ nhớ: id=${it.id}") }
        .also { require(it.name.isNotBlank()) { "Tên khách hàng không được để trống!" } }
        .let { entity ->
            CustomerSummaryDto(
                customerId = entity.id,
                displayTitle = entity.name,
                cityOrDefault = entity.address?.city ?: "N/A"
            )
        }
        .also { println("   -> [AUDIT STEP 2] Đã chuyển đổi thành công sang DTO: $it") }

    println("   -> Đối tượng cuối cùng nhận được sau chuỗi `.also` và `.let`: ${registeredDto.displayTitle}")

    println("\n--- ___ * END: Ex04ScopeFunctions_LetAlso * ___ ---")
}
