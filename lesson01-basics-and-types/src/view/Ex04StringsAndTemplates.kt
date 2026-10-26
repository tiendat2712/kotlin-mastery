package view

/**
 * =====================================================================
 * LESSON 01 - EX04: CHUỖI KÝ TỰ, STRING TEMPLATES & RAW STRINGS
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - `kotlin.String` chính là `java.lang.String` quen thuộc trên JVM (bất biến - Immutable trên Heap),
 *      nhưng được Kotlin trang bị thêm hàng loạt **Extension Functions** hữu ích trong `kotlin.text`
 *      (`isBlank()`, `padStart()`, `substringBefore()`, `substringAfter()`, `removePrefix()`, `lines()`...).
 *    - **String Templates (`"Xin chào $name, tổng = ${a + b}"`)**:
 *      + Trong Java, bạn phải nối chuỗi bằng dấu `+` hoặc `String.format(...)`.
 *      + Trong Kotlin, trình biên dịch dịch `"Xin chào $name"` thành `invokedynamic` (`makeConcatWithConstants`
 *        từ Java 9+) hoặc `StringBuilder`, đạt hiệu năng tốt mà code rất sạch sẽ!
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
 *      + Kotlin: `email.substringBefore('@')` (an toàn: nếu không tìm thấy `@`, mặc định trả về nguyên chuỗi gốc!).
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
    println("1. [String Indexing & Stdlib] Cac tien ich chuoi vuot troi so voi Java:")
    val email = "  dat.phan@kotlinjourney.vn  "
    val cleanEmail = email.trim()

    println("   -> Chuoi sau khi trim(): \"$cleanEmail\"")
    println("   -> Ky tu dau tien cleanEmail[0]: '${cleanEmail[0]}' | first(): '${cleanEmail.first()}' | last(): '${cleanEmail.last()}'")
    println("   -> Kiem tra isBlank(): ${"   ".isBlank()} (chi chua khoang trang) vs isEmpty(): ${"   ".isEmpty()}")

    // Cắt chuỗi không sợ lỗi IndexOutOfBoundsException như substring(indexOf(...)) của Java!
    val username = cleanEmail.substringBefore("@")
    val domain = cleanEmail.substringAfter("@")
    check(username == "dat.phan" && domain == "kotlinjourney.vn")
    println("   -> substringBefore('@'): \"$username\" | substringAfter('@'): \"$domain\"")

    // Tạo mã hóa đơn có độ dài cố định bằng padStart (ví dụ: "42" -> "ORD-000042")
    val orderNumber = 42
    val formattedOrderCode = "ORD-" + orderNumber.toString().padStart(length = 6, padChar = '0')
    check(formattedOrderCode == "ORD-000042")
    println("   -> Dinh dang ma don hang voi padStart(): $formattedOrderCode\n")

    // =================================================================
    // 2. String Templates: $variable và ${expression}
    // =================================================================
    println("2. [String Templates] Nhung bien va bieu thuc truc tiep vao chuoi:")
    val productName = "MacBook Pro M4"
    val quantity = 2
    val unitPrice = 45_000_000L

    // Dùng $biến cho tên biến đơn giản, và ${biểu_thức} cho phép tính hoặc lời gọi hàm
    val summary = "San pham: $productName | So luong: $quantity | Thanh tien: ${quantity * unitPrice} VND"
    println("   -> $summary")
    // Mẹo: Muốn in ký tự đô-la '$' nguyên bản trước chữ cái, dùng `${'$'}` hoặc `\$`
    println("   -> In ky tu tien te USD: Gia quy doi khoang \$${(quantity * unitPrice) / 25_000}\n")

    // =================================================================
    // 3. Multiline Raw Strings ("""...""") - Ứng dụng trong Regex, SQL & JSON
    // =================================================================
    println("3. [Raw Strings] Chuoi nhieu dong khong can ky tu Escape (\\n, \\\"):")

    // 3a. Đường dẫn Windows hoặc biểu thức chính quy (Regex) không cần gấp đôi dấu gạch chéo `\\`
    val windowsPath = """D:\Kotlin_Journey\lesson01-basics-and-types\src\view"""
    println("   -> Duong dan Windows nguyen ban: $windowsPath")

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

    println("   -> [Spring @Query] Cau lenh JPQL sau khi .trimIndent():")
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
