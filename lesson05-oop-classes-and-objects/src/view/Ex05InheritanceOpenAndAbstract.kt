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
 *   "Tại sao các class và method trong Kotlin mặc định là `final`, và điều đó ảnh hưởng
 *    thế nào đến cơ chế tạo Proxy của Spring Boot (`@Configuration`, `@Service`,
 *    `@Transactional`, `@Cacheable`, `@Async`)?"
 *
 * Giải đáp kỹ thuật chính xác:
 *   - Trong Spring AOP, có 2 cơ chế tạo Proxy phổ biến:
 *     1. **JDK Dynamic Proxy** (dựa trên Interface): Nếu Bean cài đặt một Interface và Spring
 *        proxy theo interface đó, class cài đặt có là `final` cũng vẫn tạo được proxy.
 *     2. **CGLIB Proxy** (dựa trên Subclassing): Tạo một class con kế thừa trực tiếp từ class gốc
 *        tại runtime và ghi đè (override) các phương thức để chèn logic cắt ngang (cross-cutting
 *        concerns) như `beginTransaction()` / `commit()` / `rollback()`.
 *   - Trong **Spring Boot**, cấu hình mặc định là sử dụng **CGLIB class-based proxy**
 *     (`spring.aop.proxy-target-class=true`), bởi vì đa số `@Service`, `@Component` hiện đại
 *     không viết thêm interface rườm rà. Ngoài ra, `@Configuration(proxyBeanMethods = true)`
 *     cũng dựa vào CGLIB subclassing để đảm bảo các hàm `@Bean` trả về đúng singleton instance.
 *   - Do đó, khi dùng CGLIB proxy trực tiếp trên class Kotlin, nếu class hoặc phương thức cần
 *     can thiệp AOP bị khóa `final` (mặc định của Kotlin), CGLIB sẽ không thể kế thừa class
 *     (`Cannot subclass final class`) hoặc không thể override hàm đó để gắn advice!
 *   - Giải pháp chuẩn trong hệ sinh thái Kotlin 2.0.21 + Spring Boot (khai báo trong `build.gradle.kts`):
 *     + **`kotlin("plugin.spring")`** (preset của compiler plugin `all-open`):
 *       Tự động mở khóa `open` ở tầng Bytecode cho các class và hàm thuộc các class mang annotation
 *       của Spring (`@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`,
 *       `@Configuration`, `@Transactional`, `@Async`, `@Cacheable`...). Nhờ vậy, code Kotlin
 *       không cần gõ thủ công `open class` / `open fun` khắp nơi.
 *     + **`kotlin("plugin.jpa")`** (preset của compiler plugin `no-arg`):
 *       Tự động sinh **default no-arg constructor** ở tầng Bytecode cho các class đánh dấu
 *       `@Entity`, `@Embeddable`, `@MappedSuperclass` để Hibernate/JPA có thể khởi tạo đối tượng
 *       bằng Reflection mà bạn không cần viết một constructor rỗng bằng tay. (Lưu ý: nếu muốn
 *       Hibernate tạo lazy-loading proxy cho `@Entity` mà không gõ `open class`, ta thường cấu hình
 *       thêm `allOpen { annotation("jakarta.persistence.Entity") }`).
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
    // Không cho phép class con sửa đổi quy trình gửi chuẩn (Template Method Pattern)!
    fun executeDelivery(recipient: String, rawContent: String) {
        val formatted = formatMessage(rawContent)
        val ok = send(recipient, formatted)
        println("    -> Trang thai gui toi '$recipient': ${if (ok) "THANH CONG" else "THAT BAI"}")
    }
}

// 2. Open Class kế thừa từ Abstract Class
private open class EmailNotificationSender(
    val smtpHost: String
) : NotificationSender(channelName = "EMAIL") {

    override fun send(recipient: String, content: String): Boolean {
        println("    [SMTP $smtpHost] Dang gui toi <$recipient>: $content")
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

    println("\n[1] Su dung EmailNotificationSender (Ghi de abstract fun & open fun):")
    val emailSender: NotificationSender = EmailNotificationSender(smtpHost = "smtp.gmail.com")
    emailSender.executeDelivery("dev@kotlinjourney.com", "Chao mung ban den voi Kotlin OOP!")

    println("\n[2] Su dung MarketingEmailSender (Ke thua nhieu tang & goi `super`):")
    val marketingSender: NotificationSender = MarketingEmailSender(
        smtpHost = "smtp.sendgrid.net",
        unsubscribeUrl = "https://kotlinjourney.com/unsub"
    )
    marketingSender.executeDelivery("customer@company.vn", "Uu dai khoa hoc Spring Boot Kotlin 50%!")

    println("\n--- ___ * FINISH: Ex05InheritanceOpenAndAbstract * ___ ---")
}
