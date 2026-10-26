package view

/**
 * BÀI 02: TYPE ERASURE VÀ VŨ KHÍ BÍ MẬT `inline fun <reified T>`
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - **Vấn đề của JVM Type Erasure (Xóa kiểu lúc Runtime)**:
 *      Trên JVM, thông tin về kiểu Generic `<T>` chỉ tồn tại lúc Compile-time để kiểm tra an toàn kiểu.
 *      Khi biên dịch ra file `.class` (Bytecode), mọi `<T>` đều bị xóa thành `java.lang.Object`.
 *      Do đó, trong một hàm bình thường `fun <T> check(obj: Any)`, tại Runtime JVM KHÔNG BIẾT `T` là gì!
 *      Đó là lý do cả trong Java lẫn hàm thường của Kotlin, bạn KHÔNG THỂ viết:
 *      + `if (obj is T)` (Lỗi: Cannot check for instance of erased type: T)
 *      + `val clazz = T::class.java` (Lỗi: Cannot use 'T' as reified type parameter)
 *
 *    - **Cách giải quyết truyền thống trong Java (Class Token)**:
 *      Trong Java (và Spring Java), bạn luôn phải truyền kèm `Class<T>` một cách thủ công:
 *      `objectMapper.readValue(json, UserDto.class)`
 *      `restTemplate.getForObject(url, UserDto.class)`
 *      `LoggerFactory.getLogger(OrderService.class)`
 *
 *    - **Phép màu của Kotlin: `inline fun <reified T>`**:
 *      Khi bạn đánh dấu hàm là `inline` VÀ tham số kiểu là `reified` (Reified = Hiện thực hóa / Giữ lại kiểu):
 *      + Trình biên dịch Kotlin sẽ **COPY (inline) toàn bộ thân hàm** vào ngay vị trí gọi hàm (call-site).
 *      + Tại vị trí gọi (ví dụ `parseFakeJson<UserDto>(raw)`), trình biên dịch BIẾT CHẮC CHẮN `T` là `UserDto`!
 *      + Vì vậy, trong đoạn bytecode được chèn trực tiếp vào call-site, Kotlin thay thế hoàn toàn `T`
 *        bằng kiểu thật `UserDto` (`instanceof UserDto`, `UserDto.class`)! Không cần Reflection chậm chạp,
 *        không tốn chi phí tạo lambda hay truyền `Class<T>`!
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Java: `readValue(json, new TypeReference<List<UserDto>>() {})` hoặc `readValue(json, UserDto.class)`.
 *    - Kotlin: `mapper.readValue<UserDto>(json)` hoặc `mapper.readValue<List<UserDto>>(json)`!
 *    - Lưu ý: Hàm `inline fun <reified T>` không thể được gọi trực tiếp từ code Java (vì `javac`
 *      không hiểu cơ chế inline reified của Kotlin compiler).
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot + Kotlin, các thư viện chính thức như `jackson-module-kotlin`, `RestClient`,
 *      `WebClient`, `JdbcTemplate` đều cung cấp sẵn các hàm mở rộng `reified`:
 *      + `restClient.get().uri("/users/1").retrieve().body<UserResponse>()` (không cần `UserResponse::class.java`!)
 *      + `jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM orders")`
 *      + Tạo SLF4J Logger cực gọn: `inline fun <reified T> T.logger() = LoggerFactory.getLogger(T::class.java)`
 */

// Các DTO giả lập trong ứng dụng thương mại điện tử
data class UserDto(val id: String, val username: String)
data class OrderDto(val orderId: String, val totalAmount: Long)

// 1. Tạo Logger tự động suy luận tên Class nhờ `reified T` (Pattern kinh điển trong Spring Kotlin)
class SimpleLogger(val loggerName: String) {
    fun info(message: String) = println("  [INFO] [$loggerName] - $message")
}

inline fun <reified T> loggerFor(): SimpleLogger {
    // Nhờ `reified T`, ta lấy được `T::class.qualifiedName` hoặc `T::class.java.name` ngay lúc runtime!
    val className = T::class.qualifiedName ?: T::class.simpleName ?: "UnknownClass"
    return SimpleLogger(className)
}

// 2. Giả lập Jackson ObjectMapper / Spring RestClient với `inline fun <reified T>`
// Minh họa: Người gọi chỉ cần viết `parseFakeJson<UserDto>(json)` hoặc để Kotlin tự suy luận kiểu!
inline fun <reified T> parseFakeJson(json: String): T {
    println("  -> [Bytecode Inlined] Đang giải mã JSON sang kiểu đích: ${T::class.simpleName} (${T::class.java.name})")

    // Kiểm tra kiểu đích T trực tiếp bằng `T::class`!
    return when (T::class) {
        UserDto::class -> {
            // Giả lập bóc tách JSON đơn giản
            val id = json.substringAfter("\"id\":\"").substringBefore("\"")
            val username = json.substringAfter("\"username\":\"").substringBefore("\"")
            UserDto(id, username) as T
        }
        OrderDto::class -> {
            val orderId = json.substringAfter("\"orderId\":\"").substringBefore("\"")
            val amountStr = json.substringAfter("\"totalAmount\":").substringBefore("}").trim()
            OrderDto(orderId, amountStr.toLong()) as T
        }
        String::class -> json as T
        else -> throw IllegalArgumentException("Chưa hỗ trợ giải mã cho kiểu: ${T::class.simpleName}")
    }
}

// 3. Lọc danh sách hỗn hợp theo kiểu thực tế lúc Runtime (`item is T`)
// (Đây chính là cách hàm `Iterable<*>.filterIsInstance<reified R>()` trong Kotlin stdlib được cài đặt!)
inline fun <reified T> Iterable<*>.customFilterIsInstance(): List<T> {
    val destination = mutableListOf<T>()
    for (element in this) {
        // Trong hàm thường, `element is T` sẽ báo lỗi biên dịch do Type Erasure.
        // Nhưng với `inline fun <reified T>`, trình biên dịch thay `T` bằng kiểu cụ thể tại nơi gọi!
        if (element is T) {
            destination.add(element)
        }
    }
    return destination
}

class OrderProcessingService {
    // Khởi tạo logger không cần gõ lại `OrderProcessingService::class.java`
    private val log = loggerFor<OrderProcessingService>()

    fun process(rawJson: String) {
        // Nhờ khai báo `val order: OrderDto`, Kotlin tự suy luận `<T>` của `parseFakeJson` là `OrderDto`!
        val order: OrderDto = parseFakeJson(rawJson)
        log.info("Đã xử lý đơn hàng ${order.orderId} với tổng tiền ${order.totalAmount} VNĐ")
    }
}

fun main() {
    println("--- ___ * START EX02: REIFIED TYPE PARAMETERS * ___ ---\n")

    // =========================================================================
    // 1. TẠO LOGGER KHÔNG CẦN TRUYỀN CLASS TOKEN (`MyClass::class.java`)
    // =========================================================================
    println("1. Tạo Logger gọn nhẹ với `inline fun <reified T> loggerFor()`:")
    val service = OrderProcessingService()
    service.process("""{"orderId":"ORD-2026-001","totalAmount":504000}""")

    // =========================================================================
    // 2. GIẢI MÃ DỮ LIỆU KIỂU JACKSON / RESTCLIENT (`parseFakeJson<T>`)
    // =========================================================================
    println("\n2. Giải mã JSON với `inline fun <reified T> parseFakeJson(json)`:")
    // Cách 1: Truyền tường minh kiểu `<UserDto>` ở vị trí gọi
    val user1 = parseFakeJson<UserDto>("""{"id":"C001","username":"nguyen_minh_an"}""")
    println("  - Kết quả Cách 1 (Khai báo <UserDto>): $user1")

    // Cách 2: Để Kotlin tự suy luận kiểu từ biến hứng kết quả (`val user2: UserDto = ...`)
    val user2: UserDto = parseFakeJson("""{"id":"C002","username":"tran_binh"}""")
    println("  - Kết quả Cách 2 (Type Inference từ biến): $user2")

    // =========================================================================
    // 3. KIỂM TRA `element is T` TRÊN DANH SÁCH HỖN HỢP (`filterIsInstance<T>`)
    // =========================================================================
    println("\n3. Lọc phần tử theo kiểu lúc Runtime (`element is T`):")
    val mixedEvents: List<Any> = listOf(
        UserDto("C001", "an.nguyen"),
        "SystemStartupEvent",
        OrderDto("O001", 504_000L),
        404,
        UserDto("C003", "chi.le"),
        OrderDto("O002", 997_500L)
    )

    val onlyUsers: List<UserDto> = mixedEvents.customFilterIsInstance<UserDto>()
    // Hoặc dùng hàm chuẩn có sẵn trong Kotlin stdlib: mixedEvents.filterIsInstance<OrderDto>()
    val onlyOrders: List<OrderDto> = mixedEvents.filterIsInstance<OrderDto>()

    println("  - Chỉ lấy UserDto (${onlyUsers.size} bản ghi): $onlyUsers")
    println("  - Chỉ lấy OrderDto (${onlyOrders.size} bản ghi): $onlyOrders")

    println("\n--- ___ * END EX02 * ___ ---")
}
