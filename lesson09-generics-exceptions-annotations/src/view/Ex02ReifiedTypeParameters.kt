package view

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * BÀI 02: TYPE ERASURE, `inline fun <reified T>` VÀ GIỚI HẠN VỚI KIỂU GENERIC LỒNG NHAU
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - **Bản chất chính xác của JVM Type Erasure (Xóa kiểu lúc Runtime)**:
 *      + Trên JVM, thông tin tham số kiểu `<T>` trên các instance và hàm thông thường bị xóa khi biên dịch ra `.class`.
 *      + Cụ thể, một tham số kiểu `T` sẽ bị xóa về **Upper Bound hiệu lực (Effective Upper Bound)** của nó:
 *        * Nếu khai báo `<T>` không có ràng buộc (tức `<T : Any?>`), `T` mới bị xóa thành `java.lang.Object`.
 *        * Nếu khai báo có ràng buộc như `fun <T : Number> sum(item: T)` hoặc `fun <T : Animal> feed(item: T)`,
 *          thì trong chữ ký phương thức Bytecode, `T` được xóa thành `java.lang.Number` hoặc `view.Animal`!
 *      + Vì bên trong một hàm bình thường `fun <T> check(obj: Any)`, JVM không biết `T` cụ thể tại mỗi lần gọi là gì,
 *        nên bạn KHÔNG THỂ viết `if (obj is T)` hay `T::class.java`.
 *
 *    - **Cách giải quyết truyền thống trong Java (Class Token & Super Type Token)**:
 *      + Với kiểu đơn giản: truyền `Class<T>` thủ công (`objectMapper.readValue(json, UserDto.class)`).
 *      + Với kiểu Generic lồng nhau (`List<UserDto>`): vì `List.class` không giữ được `UserDto`, Java phải tạo một
 *        lớp con ẩn danh `new TypeReference<List<UserDto>>() {}` (hoặc `ParameterizedTypeReference` trong Spring)
 *        để lưu chữ ký generic vào metadata của lớp con (`Class.getGenericSuperclass()`).
 *
 *    - **Cơ chế của Kotlin: `inline fun <reified T>`**:
 *      Khi bạn đánh dấu hàm là `inline` VÀ tham số kiểu là `reified` (Reified = Hiện thực hóa kiểu tại điểm gọi):
 *      + Trình biên dịch Kotlin sẽ **chép (inline) thân hàm** vào ngay vị trí gọi hàm (call-site).
 *      + Tại vị trí gọi (ví dụ `parseFakeJson<UserDto>(raw)`), trình biên dịch biết rõ kiểu truyền vào là `UserDto`
 *        nên thay thế trực tiếp `is T` thành `instanceof UserDto` và `T::class.java` thành `UserDto.class` trong Bytecode!
 *
 * 2. BA LƯU Ý KỸ THUẬT QUAN TRỌNG VỀ `reified` (TRÁNH HIỂU LẦM):
 *    - **Lưu ý 1 — Giới hạn của `T::class` và ép kiểu với Generic lồng nhau (`List<String>` vs `List<Int>`)**:
 *      + Nếu bạn gọi hàm `reified T` với `T = List<String>`, thì `T::class` chỉ là `List::class`!
 *        Bản thân đối tượng `ArrayList` lúc Runtime trên JVM vẫn bị xóa kiểu phần tử bên trong.
 *      + Do đó, phép ép kiểu `(anyObj as? T)` với `T = List<String>` chỉ kiểm tra được `anyObj instanceof List`,
 *        KHÔNG kiểm tra từng phần tử bên trong có phải `String` hay không! Nếu `anyObj` là `listOf(1, 2, 3)`,
 *        phép ép kiểu vẫn lọt qua và sẽ nổ `ClassCastException` khi bạn đọc phần tử ra như `String`.
 *        Muốn kiểm tra an toàn từng phần tử, bạn phải viết: `value is List<*> && value.all { it is String }`.
 *
 *    - **Lưu ý 2 — Phân biệt `T::class` (Class Token) và `TypeReference<T>` / `typeOf<T>()` (Full Type Metadata)**:
 *      + Khi bạn viết `mapper.readValue<List<UserDto>>(json)` (Jackson) hoặc `client.body<List<UserDto>>()` (Spring),
 *        các thư viện đó KHÔNG dùng `T::class.java` (vì `T::class.java` chỉ ra `java.util.List.class`, mất sạch thông tin `UserDto`!).
 *      + Thay vào đó, hàm `inline fun <reified T>` của Jackson/Spring tự động sinh ra `object : TypeReference<T>() {}`
 *        (hoặc dùng `typeOf<T>()` của Kotlin) ngay tại điểm gọi. Bytecode của lớp ẩn danh này giữ nguyên chữ ký
 *        `ParameterizedType` (`List<UserDto>`), và lúc Runtime framework vẫn đọc metadata đó để giải mã từng `UserDto`!
 *
 *    - **Lưu ý 3 — `reified` giúp bỏ nghi thức truyền Class/TypeReference thủ công, KHÔNG làm biến mất Reflection của Framework**:
 *      + `reified` loại bỏ chi phí cú pháp (syntactic ceremony) phải truyền `UserDto::class.java` và cho phép dùng `is T` / `T::class` trực tiếp.
 *      + Tuy nhiên, bên dưới các thư viện JSON (Jackson), ORM (Hibernate) hay DI (Spring), quá trình đọc metadata,
 *        gọi constructor và gán field lúc Runtime vẫn sử dụng Reflection như bình thường.
 */

// Các DTO giả lập trong ứng dụng thương mại điện tử
data class UserDto(val id: String, val username: String)
data class OrderDto(val orderId: String, val totalAmount: Long)

// 1. Tạo Logger tự động suy luận tên Class nhờ `reified T` (Pattern phổ biến trong Spring Kotlin)
class SimpleLogger(val loggerName: String) {
    fun info(message: String) = println("  [INFO] [$loggerName] - $message")
}

inline fun <reified T> loggerFor(): SimpleLogger {
    // Nhờ `reified T`, ta lấy được `T::class.qualifiedName` hoặc `T::class.java.name` ngay tại điểm gọi!
    val className = T::class.qualifiedName ?: T::class.simpleName ?: "UnknownClass"
    return SimpleLogger(className)
}

// 2. Giả lập hàm đọc JSON đơn giản cho các DTO phẳng (không lồng generic) bằng `T::class`
inline fun <reified T> parseFakeJson(json: String): T {
    println("  -> [Bytecode Inlined] Dang giai ma JSON sang kieu dich: ${T::class.simpleName} (${T::class.java.name})")

    return when (T::class) {
        UserDto::class -> {
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
        else -> throw IllegalArgumentException("Chua ho tro giai ma bang T::class cho kieu: ${T::class.simpleName}")
    }
}

// 3. Lọc danh sách hỗn hợp theo kiểu thực tế lúc Runtime (`item is T`)
// (Tương tự cách hàm `Iterable<*>.filterIsInstance<reified R>()` trong Kotlin stdlib hoạt động)
inline fun <reified T> Iterable<*>.customFilterIsInstance(): List<T> {
    val destination = mutableListOf<T>()
    for (element in this) {
        if (element is T) {
            destination.add(element)
        }
    }
    return destination
}

// 4. Minh họa giới hạn của `reified` với kiểu Generic lồng nhau (`List<String>` vs `List<Int>`)
inline fun <reified T : Any> inspectRawClassToken(): Class<*> = T::class.java

@Suppress("UNCHECKED_CAST")
inline fun <reified T> unsafeReifiedCastOrNull(value: Any): T? = value as? T

// Hàm kiểm tra an toàn cho List<T> bằng cách kiểm tra cả phần tử bên trong với `reified E`
inline fun <reified E> isListOfElements(value: Any): Boolean =
    value is List<*> && value.all { it is E }

// 5. Minh họa Super Type Token (`SimpleTypeReference<T>`) giống Jackson `TypeReference<T>` / Spring `ParameterizedTypeReference<T>`
abstract class SimpleTypeReference<T> {
    val fullGenericType: Type =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
}

inline fun <reified T> captureFullTypeToken(): SimpleTypeReference<T> =
    object : SimpleTypeReference<T>() {}

class OrderProcessingService {
    // Khởi tạo logger không cần gõ lại `OrderProcessingService::class.java`
    private val log = loggerFor<OrderProcessingService>()

    fun process(rawJson: String) {
        val order: OrderDto = parseFakeJson(rawJson)
        log.info("Da xu ly don hang ${order.orderId} voi tong tien ${order.totalAmount} VND")
    }
}

fun main() {
    println("--- ___ * START EX02: REIFIED TYPE PARAMETERS & TYPE TOKENS * ___ ---\n")

    // =========================================================================
    // 1. TẠO LOGGER KHÔNG CẦN TRUYỀN CLASS TOKEN (`MyClass::class.java`)
    // =========================================================================
    println("1. Tao Logger gon nhe voi `inline fun <reified T> loggerFor()`:")
    val service = OrderProcessingService()
    service.process("""{"orderId":"ORD-2026-001","totalAmount":504000}""")

    // =========================================================================
    // 2. GIẢI MÃ DỮ LIỆU ĐƠN GIẢN VỚI `T::class` (`parseFakeJson<T>`)
    // =========================================================================
    println("\n2. Giai ma JSON phang voi `inline fun <reified T> parseFakeJson(json)`:")
    val user1 = parseFakeJson<UserDto>("""{"id":"C001","username":"nguyen_minh_an"}""")
    println("  - Ket qua Cach 1 (Khai bao <UserDto>): $user1")

    val user2: UserDto = parseFakeJson("""{"id":"C002","username":"tran_binh"}""")
    println("  - Ket qua Cach 2 (Type Inference tu bien): $user2")
    check(user1.id == "C001" && user2.username == "tran_binh")

    // =========================================================================
    // 3. KIỂM TRA `element is T` TRÊN DANH SÁCH HỖN HỢP (`filterIsInstance<T>`)
    // =========================================================================
    println("\n3. Loc phan tu theo kieu luc Runtime (`element is T`):")
    val mixedEvents: List<Any> = listOf(
        UserDto("C001", "an.nguyen"),
        "SystemStartupEvent",
        OrderDto("O001", 504_000L),
        404,
        UserDto("C003", "chi.le"),
        OrderDto("O002", 997_500L)
    )

    val onlyUsers: List<UserDto> = mixedEvents.customFilterIsInstance<UserDto>()
    val onlyOrders: List<OrderDto> = mixedEvents.filterIsInstance<OrderDto>()

    println("  - Chi lay UserDto (${onlyUsers.size} ban ghi): $onlyUsers")
    println("  - Chi lay OrderDto (${onlyOrders.size} ban ghi): $onlyOrders")
    check(onlyUsers.size == 2 && onlyOrders.size == 2)

    // =========================================================================
    // 4. GIỚI HẠN CỦA `reified` VỚI NESTED GENERICS (`List<String>` vs `List<Int>`)
    // =========================================================================
    println("\n4. Gioi han cua `reified` khi T la kieu Generic long nhau (`List<String>` vs `List<Int>`):")
    val stringListClass = inspectRawClassToken<List<String>>()
    val intListClass = inspectRawClassToken<List<Int>>()
    println("  - T::class.java cua List<String>: ${stringListClass.name}")
    println("  - T::class.java cua List<Int>   : ${intListClass.name}")
    println("  - Hai Class Token co bang nhau khong? -> ${stringListClass == intListClass} (Vi tham so String/Int van bi erase tren List!)")
    check(stringListClass == intListClass)

    val numbersAsAny: Any = listOf(1, 2, 3)
    val wronglyCastedList: List<String>? = unsafeReifiedCastOrNull<List<String>>(numbersAsAny)
    println("  - Ep kieu `unsafeReifiedCastOrNull<List<String>>(listOf(1, 2, 3))` co thanh cong khong? -> ${wronglyCastedList != null}")
    try {
        // Khi cố đọc phần tử đầu tiên ra như String, lệnh CHECKCAST java/lang/String trong bytecode mới nổ lỗi!
        val firstElementAsString: String = wronglyCastedList!![0]
        println("  - Phan tu dau tien: $firstElementAsString")
    } catch (e: ClassCastException) {
        println("  - [Canh bao] No ClassCastException khi doc phan tu: ${e.javaClass.simpleName} (${e.message})")
    }

    // Cách kiểm tra đúng: kiểm tra `value is List<*>` VÀ kiểm tra từng phần tử bằng `reified E`
    println("  - Kiem tra an toan `isListOfElements<String>(listOf(1, 2, 3))` = ${isListOfElements<String>(numbersAsAny)}")
    println("  - Kiem tra an toan `isListOfElements<Int>(listOf(1, 2, 3))`    = ${isListOfElements<Int>(numbersAsAny)}")
    check(!isListOfElements<String>(numbersAsAny))
    check(isListOfElements<Int>(numbersAsAny))

    // =========================================================================
    // 5. CÁCH JACKSON / SPRING GIỮ NGUYÊN KIỂU LỒNG NHAU (`TypeReference<T>`)
    // =========================================================================
    println("\n5. Phan biet `T::class` (mat UserDto) vs `SimpleTypeReference<T>` (giu nguyen List<UserDto>):")
    val rawClassOnly = inspectRawClassToken<List<UserDto>>()
    val fullTypeToken = captureFullTypeToken<List<UserDto>>()
    println("  - Neu chi dung `T::class.java`           : ${rawClassOnly.typeName}")
    println("  - Neu dung `object : TypeReference<T>()` : ${fullTypeToken.fullGenericType.typeName}")
    check(fullTypeToken.fullGenericType.typeName.contains("UserDto"))

    // =========================================================================
    // 6. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // =========================================================================
    println("\n6. Goc Tu Luyen (Active Learning):")
    // [ĐỀ BÀI]:
    // Viết một hàm `inline fun <reified T> countInstancesOf(items: Iterable<*>): Int`
    // đếm số lượng phần tử thuộc kiểu `T` trong một danh sách hỗn hợp.
    // Kiểm thử với `mixedEvents` ở Mục 3: có 2 `UserDto`, 1 `String`, và 1 `Int`.
    val stringCount = countInstancesOf<String>(mixedEvents)
    val intCount = countInstancesOf<Int>(mixedEvents)
    val userCount = countInstancesOf<UserDto>(mixedEvents)
    println("  - Ket qua countInstancesOf: String=$stringCount, Int=$intCount, UserDto=$userCount")
    check(stringCount == 1 && intCount == 1 && userCount == 2)
    println("  => [PASS] Goc Tu Luyen Ex02 hoan tat!")

    println("\n--- ___ * END EX02 * ___ ---")
}

// [LỜI GIẢI THAM KHẢO CHO GÓC TỰ LUYỆN]:
inline fun <reified T> countInstancesOf(items: Iterable<*>): Int {
    var count = 0
    for (item in items) {
        if (item is T) count++
    }
    return count
}
