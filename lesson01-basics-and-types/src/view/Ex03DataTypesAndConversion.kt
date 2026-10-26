package view

import java.math.BigDecimal

/**
 * =====================================================================
 * LESSON 01 - EX03: HỆ THỐNG KIỂU DỮ LIỆU, BOXING & ÉP KIỂU TƯỜNG MINH
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java, lập trình viên phải phân biệt rõ 8 kiểu nguyên thủy (`int`, `long`, `double`, `boolean`...)
 *      và các Wrapper Class (`Integer`, `Long`, `Double`, `Boolean`...).
 *    - Trong Kotlin, trên bề mặt cú pháp, **mọi thứ đều được thao tác nhất quán như Object** (`Int`, `Long`, `Double`, `Boolean`).
 *      Bạn có thể gọi trực tiếp `10.toLong()` hay `(-5).coerceAtLeast(0)`.
 *    - **NHƯNG dưới tầng JVM Bytecode, Kotlin tự động tối ưu hóa**:
 *      + Nếu biến là Non-Null (`val age: Int = 25`): trình biên dịch dịch thẳng thành kiểu nguyên thủy
 *        `int` của JVM (nằm ở Stack Frame nếu là biến cục bộ, hoặc trường `int` nguyên thủy bên trong Object trên Heap),
 *        không tốn bộ nhớ Object Header của Wrapper!
 *      + Nếu biến là Nullable (`val age: Int? = 25`) hoặc dùng làm tham số kiểu Generic (`List<Int>`):
 *        trình biên dịch mới **Box** thành đối tượng `java.lang.Integer` nằm trên **HEAP**!
 *
 * 2. HỆ THỐNG PHÂN CẤP KIỂU (KOTLIN TYPE HIERARCHY):
 *    - `Any`: Kiểu gốc (Root type) của mọi kiểu non-null trong Kotlin. Dịch ra Bytecode chính là `java.lang.Object`.
 *      (`Any?` là kiểu gốc của tất cả mọi kiểu, kể cả `null`).
 *    - `Unit`: Tương đương `void` trong Java, nhưng `Unit` là một **Singleton Object** thật sự.
 *      Nhờ vậy, trong Generic (`interface Processor<T> { fun process(): T }`), ta có thể truyền `Processor<Unit>`
 *      mà không cần tạo kiểu `Void` và `return null;` như trong Java!
 *    - `Nothing`: Kiểu đáy (Bottom type) — là kiểu con của mọi kiểu trong Kotlin, nhưng không có bất kỳ thực thể nào.
 *      Dùng cho các biểu thức/hàm không bao giờ trả về bình thường (chỉ ném Exception hoặc chạy vòng lặp vô tận).
 *
 * 3. SO SÁNH JAVA VS KOTLIN VỀ CHUYỂN ĐỔI KIỂU SỐ (TYPE CONVERSION):
 *    - Java hỗ trợ **Implicit Widening** (tự động ép kiểu ngầm định từ kiểu số nhỏ sang kiểu lớn):
 *      `int i = 10; long l = i;` -> Hợp lệ trong Java.
 *      Nhưng điều này gây ra bẫy tinh vi khi dùng với Wrapper:
 *      `Integer i = 10; Long l = 10L; l.equals(i);` -> Trả về `false`!
 *    - Kotlin **không tự động mở rộng kiểu số ngầm định khi gán biến**:
 *      `val i: Int = 10; val l: Long = i` -> LỖI BIÊN DỊCH!
 *      Bạn cần gọi hàm chuyển đổi tường minh: `val l: Long = i.toLong()`.
 *      Nhờ vậy mã nguồn rõ ràng, hạn chế lỗi mất độ chính xác hoặc so sánh nhầm kiểu Wrapper.
 *
 * 4. SPRING KOTLIN CORNER:
 *    - Trong các Entity JPA/Hibernate hoặc DTO tiền tệ của Spring Boot:
 *      + ID thường là `Long` (`toLong()`).
 *      + Tiền tệ tài chính nên ưu tiên dùng `BigDecimal` (`"199000.50".toBigDecimal()` hoặc `199000.toBigDecimal()`).
 *    - Hàm helper ném lỗi `failNotFound(message): Nothing` giúp kết hợp mượt mà với toán tử Elvis `?:`.
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex03DataTypesAndConversion * ___ ---\n")

    // =================================================================
    // 1. Primitive vs Boxed dưới góc nhìn JVM Bytecode
    // =================================================================
    println("1. [Primitive vs Boxed] Toi uu hoa bo nho cua Kotlin tren JVM:")
    val primitiveInt: Int = 42          // Bytecode: int primitiveInt = 42; (Primitive)
    val nullableInt: Int? = 42          // Bytecode: Integer nullableInt = Integer.valueOf(42); (Boxed trên HEAP)
    val readableMillion = 1_000_000_000L // Dùng dấu `_` phân tách hàng nghìn giúp dễ đọc

    // Dùng reflection `.javaClass` để kiểm chứng kiểu thực tế lúc runtime:
    println("   -> primitiveInt ($primitiveInt) co JVM type: ${primitiveInt::class.javaPrimitiveType} (Primitive int)")
    println("   -> nullableInt  ($nullableInt) co JVM class: ${nullableInt?.javaClass?.name} (Boxed Object tren Heap)")
    println("   -> So lon co dau gach duoi: $readableMillion\n")

    // =================================================================
    // 2. Ép kiểu tường minh (Explicit Conversions)
    // =================================================================
    println("2. [Explicit Conversion] Tai sao Kotlin yeu cau goi toLong(), toDouble()...")
    val quantity: Int = 5
    val unitPrice: Long = 120_000L

    // val total: Long = quantity // LỖI BIÊN DỊCH: Type mismatch: inferred type is Int but Long was expected
    val quantityAsLong: Long = quantity.toLong()
    val totalAmount: Long = quantityAsLong * unitPrice
    val averageAsDouble: Double = totalAmount.toDouble() / 3
    val financialDecimal: BigDecimal = "250000.75".toBigDecimal()
    check(totalAmount == 600_000L)

    println("   -> quantity.toLong() * unitPrice = $totalAmount")
    println("   -> Ep sang Double de chia so thuc: %.2f".format(averageAsDouble))
    println("   -> Chuyen String sang BigDecimal an toan: $financialDecimal\n")

    // =================================================================
    // 3. Ép kiểu từ chuỗi (String parsing) an toàn không sợ Exception
    // =================================================================
    println("3. [Safe String Parsing] toIntOrNull() thay the Integer.parseInt() de tranh NumberFormatException:")
    val validInput = "2026"
    val corruptedInput = "2026abc"

    check(validInput.toIntOrNull() == 2026)
    check(corruptedInput.toIntOrNull() == null)
    println("   -> \"$validInput\".toIntOrNull()    = ${validInput.toIntOrNull()}")
    println("   -> \"$corruptedInput\".toIntOrNull() = ${corruptedInput.toIntOrNull()} (Khong bi crash boi NumberFormatException!)\n")

    // =================================================================
    // 4. Phân cấp kiểu đặc biệt: Any, Unit, Nothing
    // =================================================================
    println("4. [Any, Unit, Nothing] Ba kieu dac biet trong he thong kieu Kotlin:")

    // 4a. Any (tương đương java.lang.Object)
    val anything: Any = "Chuoi Kotlin cung la con cua Any"
    println("   -> [Any] Gia tri: \"$anything\" (Class thuc te: ${anything.javaClass.name})")

    // 4b. Unit (tương đương void nhưng là một Singleton Object)
    val unitResult: Unit = logAction("Don hang #101 da duoc xac nhan")
    println("   -> [Unit] Gia tri tra ve cua ham khong co return: $unitResult (Singleton instance)")

    // 4c. Nothing (kiểu đáy không bao giờ trả về, hỗ trợ biểu thức ném lỗi)
    val configPortStr: String? = "8080"
    // Nhờ failConfigError() trả về `Nothing` (là kiểu con của String), nên biến `port` có kiểu `String` (Non-Null)!
    val port: String = configPortStr ?: failConfigError("Thieu cau hinh PORT!")
    check(port == "8080")
    println("   -> [Nothing] Bien `port` duoc suy luan la Non-Null String: $port")

    println("\n--- ___ * END: Ex03DataTypesAndConversion * ___ ---")
}

// Hàm không khai báo kiểu trả về sẽ mặc định trả về `Unit`
private fun logAction(message: String) {
    println("   -> [Log] $message")
}

// Hàm có kiểu trả về `Nothing`: báo cho trình biên dịch biết hàm này không bao giờ trả về bình thường
private fun failConfigError(reason: String): Nothing {
    throw IllegalStateException(reason)
}
