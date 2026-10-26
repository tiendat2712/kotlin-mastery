package view

/**
 * =====================================================================
 * LESSON 01 - EX04: CHUỖI KÝ TỰ, STRING TEMPLATES & RAW STRINGS
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - `kotlin.String` chính là `java.lang.String` quen thuộc trên JVM (bất biến - Immutable trên Heap),
 *      nhưng được Kotlin trang bị thêm hàng loạt **Extension Functions** cực mạnh trong `kotlin.text`
 *      (`isBlank()`, `padStart()`, `substringBefore()`, `substringAfter()`, `removePrefix()`, `lines()`...).
 *    - **String Templates (`"Xin chào $name, tổng = ${a + b}"`)**:
 *      + Trong Java, bạn phải nối chuỗi bằng dấu `+` hoặc `String.format(...)`.
 *      + Trong Kotlin, trình biên dịch dịch `"Xin chào $name"` thành `invokedynamic` (`makeConcatWithConstants`
 *        từ Java 9+) hoặc `StringBuilder`, đạt hiệu năng tối ưu mà code cực kỳ sạch sẽ!
 *    - **Multiline Raw Strings (`""" ... """`)**:
 *      + Giữ nguyên xuống dòng và cho phép viết dấu ngoặc kép `"` hoặc ký tự `\` (Regex, đường dẫn Windows,
 *        JSON, SQL) mà KHÔNG cần escape `\"` hay `\\`.
 *      + Đi kèm `.trimIndent()` để tự động cắt bỏ khoảng trắng thụt lề chung ở đầu mỗi dòng.
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    - Lấy ký tự tại vị trí `i`:
 *      + Java:   `str.charAt(0)`
 *      + Kotlin: `str[0]` (nhờ Operator Overloading `get(index)`), cộng thêm `str.first()`, `str.last()`.
 *    - Cắt chuỗi thông minh:
 *      + Java:   `email.substring(0, email.indexOf('@'))` (dễ dính `IndexOutOfBoundsException` nếu không có `@`!).
 *      + Kotlin: `email.substringBefore('@')` (an toàn tuyệt đối: nếu không tìm thấy `@`, trả về nguyên chuỗi gốc!).
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Data JPA `@Query`: Dùng Raw String `""" ... """` để viết câu truy vấn JPQL hoặc Native SQL
 *      nhiều dòng sạch đẹp.
 *    - Trong Spring Boot MockMvc / WebTestClient: Dùng Raw String kết hợp String Template để tạo JSON Request Body
 *      động mà không cần nối chuỗi `\"` rối mắt!
 * =====================================================================
 */

fun main() {
    println("--- ___ * START: Ex04StringsAndTemplates * ___ ---\n")

    // =================================================================
    // 1. Indexing & Các hàm xử lý chuỗi đặc sắc của Kotlin Standard Library
    // =================================================================
    println("1. [String Indexing & Stdlib] Các tiện ích chuỗi vượt trội so với Java:")
    val email = "  dat.phan@kotlinjourney.vn  "
    val cleanEmail = email.trim()

    println("   -> Chuỗi sau khi trim(): \"$cleanEmail\"")
    println("   -> Ký tự đầu tiên cleanEmail[0]: '${cleanEmail[0]}' | first(): '${cleanEmail.first()}' | last(): '${cleanEmail.last()}'")
    println("   -> Kiểm tra isBlank(): ${"   ".isBlank()} (chỉ chứa khoảng trắng) vs isEmpty(): ${"   ".isEmpty()}")

    // Cắt chuỗi không bao giờ sợ lỗi IndexOutOfBoundsException như substring(indexOf(...)) của Java!
    val username = cleanEmail.substringBefore("@")
    val domain = cleanEmail.substringAfter("@")
    println("   -> substringBefore('@'): \"$username\" | substringAfter('@'): \"$domain\"")

    // Tạo mã hóa đơn có độ dài cố định bằng padStart (ví dụ: "42" -> "ORD-000042")
    val orderNumber = 42
    val formattedOrderCode = "ORD-" + orderNumber.toString().padStart(length = 6, padChar = '0')
    println("   -> Định dạng mã đơn hàng với padStart(): $formattedOrderCode\n")

    // =================================================================
    // 2. String Templates: $variable và ${expression}
    // =================================================================
    println("2. [String Templates] Nhúng biến và biểu thức trực tiếp vào chuỗi:")
    val productName = "MacBook Pro M4"
    val quantity = 2
    val unitPrice = 45_000_000L

    // Dùng $biến cho tên biến đơn giản, và ${biểu_thức} cho phép tính hoặc lời gọi hàm
    val summary = "Sản phẩm: $productName | Số lượng: $quantity | Thành tiền: ${quantity * unitPrice} VND"
    println("   -> $summary")
    // Mẹo: Muốn in ký tự đô-la '$' nguyên bản trước chữ cái, dùng `${'$'}` hoặc `\$`
    println("   -> In ký tự tiền tệ USD: Giá quy đổi khoảng \$${(quantity * unitPrice) / 25_000}\n")

    // =================================================================
    // 3. Multiline Raw Strings ("""...""") - Ứng dụng trong Regex, SQL & JSON
    // =================================================================
    println("3. [Raw Strings] Chuỗi nhiều dòng không cần ký tự Escape (\\n, \\\"):")

    // 3a. Đường dẫn Windows hoặc biểu thức chính quy (Regex) không cần gấp đôi dấu gạch chéo `\\`
    val windowsPath = """D:\Kotlin_Journey\lesson01-basics-and-types\src\view"""
    println("   -> Đường dẫn Windows nguyên bản: $windowsPath")

    // 3b. [Spring Data JPA Corner] Câu truy vấn SQL/JPQL nhiều dòng với trimIndent()
    val minTotal = 1_000_000L
    val statusFilter = "COMPLETED"
    val jpqlQuery = """
        SELECT o.id, o.customerName, o.totalAmount
        FROM OrderEntity o
        WHERE o.status = '$statusFilter'
          AND o.totalAmount >= $minTotal
        ORDER BY o.createdAt DESC
    """.trimIndent()

    println("   -> [Spring @Query] Câu lệnh JPQL sau khi .trimIndent():")
    println(jpqlQuery.prependIndent("      "))

    // 3c. [Spring MockMvc Test Corner] Tạo payload JSON động
    val jsonPayload = """
        {
          "orderCode": "$formattedOrderCode",
          "customerEmail": "$cleanEmail",
          "totalAmount": ${quantity * unitPrice},
          "verified": true
        }
    """.trimIndent()

    println("\n   -> [Spring MockMvc] JSON Request Body:")
    println(jsonPayload.prependIndent("      "))

    println("\n--- ___ * END: Ex04StringsAndTemplates * ___ ---")
}
