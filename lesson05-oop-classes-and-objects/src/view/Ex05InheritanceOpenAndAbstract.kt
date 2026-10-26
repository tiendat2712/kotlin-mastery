package view

/**
 * ============================================================================
 * BÀI 05: KẾ THỪA (`open`, `override`, `abstract`) & BÍ MẬT SPRING AOP PROXY
 * ============================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / EFFECTIVE JAVA ITEM 19 / BYTECODE):
 * ----------------------------------------------------------------------------
 * Trong cuốn sách kinh điển *Effective Java (Item 19)*, Joshua Bloch viết:
 *   "Design and document for inheritance or else prohibit it"
 *   (Hãy thiết kế và viết tài liệu cẩn thận cho việc kế thừa, nếu không hãy cấm nó!).
 *
 * - Trong Java (`lesson04-oop-features`): Mặc định mọi class và method đều cho phép
 *   kế thừa và ghi đè (trừ khi bạn chủ động gắn từ khóa `final`). Điều này dễ dẫn tới
 *   lỗi *Fragile Base Class Problem* (class con ghi đè bừa bãi làm hỏng logic class cha).
 * - Trong Kotlin: Ngược lại hoàn toàn!
 *   + MỌI `class` và MỌI `fun` / `val` / `var` trong Kotlin mặc định đều được biên dịch ra
 *     Bytecode là **`public final`**!
 *   + Muốn cho phép một class được kế thừa, bạn BẮT BUỘC phải mở khóa bằng từ khóa `open class`.
 *   + Muốn cho phép một hàm/thuộc tính được ghi đè, bạn BẮT BUỘC phải viết `open fun` / `open val`.
 *   + Ở class con, `override` là một **từ khóa bắt buộc** (chứ không phải annotation tùy chọn
 *     `@Override` như Java).
 *   + Tương tự Java, `abstract class` và `abstract fun` thì mặc định đã mang tính `open`
 *     nên không cần gõ thêm từ khóa `open`.
 *
 * 2. SPRING KOTLIN CORNER (CỰC KỲ QUAN TRỌNG KHI ĐI LÀM & PHỎNG VẤN):
 * ----------------------------------------------------------------------------
 * Câu hỏi phỏng vấn kinh điển:
 *   "Tại sao các class trong Kotlin mặc định là `final`, mà Spring Boot (`@Configuration`,
 *    `@Service`, `@Transactional`, `@Cacheable`, `@Async`) vẫn tạo được CGLIB Proxy
 *    (vốn hoạt động bằng cách tạo class con kế thừa class gốc tại runtime)?"
 *
 * Giải đáp kỹ thuật:
 *   - Trong Spring AOP, khi bạn đánh dấu `@Transactional` lên `OrderService`, Spring không
 *     tiêm trực tiếp `OrderService` mà tạo ra một **Dynamic Subclass (CGLIB Proxy)** kế thừa
 *     `OrderService` và ghi đè các hàm để chèn code `beginTransaction()` / `commit()` / `rollback()`.
 *   - Nếu `OrderService` và các hàm của nó là `final` (mặc định của Kotlin), CGLIB sẽ nổ lỗi
 *     ngay lúc khởi động: `Cannot subclass final class` hoặc `@Transactional` bị phớt lờ!
 *   - Trước đây, lập trình viên phải gõ `open class` và `open fun` khắp nơi rất mỏi tay.
 *   - Ngày nay, mọi dự án Spring Boot + Kotlin đều khai báo plugin trong `build.gradle.kts`:
 *       `kotlin("plugin.spring")` (tên gốc: `org.jetbrains.kotlin.plugin.allopen`)
 *     Plugin này can thiệp vào trình biên dịch `kotlinc`: Bất kỳ class nào có gắn các annotation
 *     `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Configuration`,
 *     `@Transactional`, `@Async`, `@Cacheable`... sẽ được **tự động gỡ bỏ `final` (biến thành `open`)
 *     ngay trong lúc biên dịch ra Bytecode** mà code Kotlin của bạn vẫn giữ nguyên sự sạch sẽ!
 */

// 1. Abstract Class: Bản thiết kế khung (Template) cho các loại thông báo
private abstract class NotificationSender(val channelName: String) {

    // Hàm abstract: Bắt buộc class con phải override (mặc định đã là open)
    abstract fun send(recipient: String, content: String): Boolean

    // Hàm có thân nhưng đánh dấu `open`: Class con CÓ THỂ override nếu muốn tùy biến
    open fun formatMessage(content: String): String {
        return "[Channel: $channelName] $content"
    }

    // Hàm KHÔNG có `open` (mặc định là `final` dưới Bytecode):
    // Cấm tuyệt đối class con sửa đổi quy trình gửi chuẩn (Template Method Pattern)!
    fun executeDelivery(recipient: String, rawContent: String) {
        val formatted = formatMessage(rawContent)
        val ok = send(recipient, formatted)
        println("    -> Trạng thái gửi tới '$recipient': ${if (ok) "THÀNH CÔNG" else "THẤT BẠI"}")
    }
}

// 2. Open Class kế thừa từ Abstract Class
private open class EmailNotificationSender(
    val smtpHost: String
) : NotificationSender(channelName = "EMAIL") {

    override fun send(recipient: String, content: String): Boolean {
        println("    [SMTP $smtpHost] Đang gửi tới <$recipient>: $content")
        return recipient.contains("@")
    }

    // Ghi đè hàm `open` của class cha
    override fun formatMessage(content: String): String {
        return "${super.formatMessage(content)} (Sent via SMTP $smtpHost)"
    }
}

// 3. Kế thừa tiếp từ `open class EmailNotificationSender`
private class MarketingEmailSender(
    smtpHost: String,
    private val unsubscribeUrl: String
) : EmailNotificationSender(smtpHost) {

    // Tiếp tục ghi đè `formatMessage`. Nếu muốn khóa lại không cho đời cháu ghi đè nữa,
    // ta có thể viết: `final override fun formatMessage(...)`
    final override fun formatMessage(content: String): String {
        return "${super.formatMessage(content)} | Unsubscribe: $unsubscribeUrl"
    }
}

fun main() {
    println("--- ___ * START: Ex05InheritanceOpenAndAbstract * ___ ---")

    println("\n[1] Sử dụng EmailNotificationSender (Ghi đè abstract fun & open fun):")
    val emailSender: NotificationSender = EmailNotificationSender(smtpHost = "smtp.gmail.com")
    emailSender.executeDelivery("dev@kotlinjourney.com", "Chào mừng bạn đến với Kotlin OOP!")

    println("\n[2] Sử dụng MarketingEmailSender (Kế thừa nhiều tầng & gọi `super`):")
    val marketingSender: NotificationSender = MarketingEmailSender(
        smtpHost = "smtp.sendgrid.net",
        unsubscribeUrl = "https://kotlinjourney.com/unsub"
    )
    marketingSender.executeDelivery("customer@company.vn", "Ưu đãi khóa học Spring Boot Kotlin 50%!")

    println("\n--- ___ * FINISH: Ex05InheritanceOpenAndAbstract * ___ ---")
}
