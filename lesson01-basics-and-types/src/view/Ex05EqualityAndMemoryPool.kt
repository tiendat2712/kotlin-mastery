package view

/**
 * =====================================================================
 * LESSON 01 - EX05: SO SÁNH BẰNG (== VS ===) & BỘ NHỚ STRING POOL
 * =====================================================================
 *
 * 1. BẢN CHẤT (STACK VS HEAP / CÚ LỪA LỚN NHẤT KHI TỪ JAVA SANG KOTLIN):
 *    - Trong Java:
 *      + `a == b` là so sánh **Địa chỉ vùng nhớ (Referential Equality)** trên STACK.
 *        Rất nhiều lập trình viên Java mắc lỗi kinh điển: dùng `str1 == str2` để so sánh chuỗi!
 *      + `a.equals(b)` mới là so sánh **Giá trị nội dung (Structural Equality)** trên HEAP,
 *        nhưng nếu `a == null` thì gọi `a.equals(b)` sẽ nổ `NullPointerException`!
 *
 *    - Trong Kotlin (ĐẢO NGƯỢC ĐỂ AN TOÀN TUYỆT ĐỐI!):
 *      + `a == b` (2 dấu bằng) là **Structural Equality (So sánh giá trị nội dung trên HEAP)**!
 *        Dưới tầng Bytecode, `a == b` được Kotlin dịch thành:
 *        `a?.equals(b) ?: (b === null)` (tức là `Intrinsics.areEqual(a, b)`).
 *        => Kể cả khi `a` là `null`, phép `a == b` trong Kotlin VẪN CHẠY AN TOÀN, không bao giờ nổ NPE!
 *      + `a === b` (3 dấu bằng) mới là **Referential Equality (So sánh địa chỉ ô nhớ trên STACK)**!
 *        Dưới tầng Bytecode, `a === b` dịch thẳng thành `a == b` của Java!
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    | Mục đích so sánh                     | Cú pháp Java                   | Cú pháp Kotlin |
 *    |--------------------------------------|--------------------------------|----------------|
 *    | So sánh nội dung (Heap - equals)     | `Objects.equals(a, b)`         | `a == b`       |
 *    | So sánh khác nội dung                | `!Objects.equals(a, b)`        | `a != b`       |
 *    | So sánh cùng địa chỉ (Stack ref)     | `a == b`                       | `a === b`      |
 *    | So sánh khác địa chỉ                 | `a != b`                       | `a !== b`      |
 *
 * 3. SPRING KOTLIN CORNER:
 *    - Trong Spring Boot Controller / Service, khi kiểm tra `if (request.role == "ADMIN")` hoặc
 *      `if (userDto1 == userDto2)`, bạn cứ tự tin dùng `==` như so sánh số nguyên!
 *    - Lưu ý với JPA `@Entity`: Khi override `equals()` và `hashCode()` cho Entity, phép `entity1 == entity2`
 *      sẽ gọi thẳng hàm `equals()` đó.
 * =====================================================================
 */

private data class ProductDto(val id: Long, val name: String)

@Suppress("DEPRECATED_IDENTITY_EQUALS")
fun main() {
    println("--- ___ * START: Ex05EqualityAndMemoryPool * ___ ---\n")

    // =================================================================
    // 1. String Pool vs Cấp phát mới trên Heap: Kiểm chứng `==` và `===`
    // =================================================================
    println("1. [String Pool vs Heap] Phân biệt rõ `==` (nội dung) và `===` (địa chỉ ô nhớ):")

    // s1 và s2 cùng trỏ tới 1 hằng chuỗi duy nhất trong String Pool (trên Heap)
    val s1: String = "KotlinSpring"
    val s2: String = "KotlinSpring"

    // s3 được khởi tạo tường minh bằng constructor -> ép JVM cấp phát một Object MỚI trên Heap
    val s3: String = String(charArrayOf('K', 'o', 't', 'l', 'i', 'n', 'S', 'p', 'r', 'i', 'n', 'g'))

    println("   -> s1 = \"$s1\", s2 = \"$s2\", s3 = \"$s3\"")
    println("   -> [s1 == s2]  (So sánh nội dung):  ${s1 == s2}")
    println("   -> [s1 === s2] (So sánh địa chỉ):   ${s1 === s2} (Cùng trỏ vào String Pool)")
    println("   -> [s1 == s3]  (So sánh nội dung):  ${s1 == s3} (Nội dung giống hệt nhau -> dùng `==` luôn an toàn!)")
    println("   -> [s1 === s3] (So sánh địa chỉ):   ${s1 === s3} (Khác địa chỉ: s1 ở String Pool, s3 là Object mới trên Heap)\n")

    // =================================================================
    // 2. An toàn với Null khi dùng `==` trong Kotlin
    // =================================================================
    println("2. [Null-Safe Equality] Phép `a == b` không bao giờ ném NullPointerException:")
    val nullStr: String? = null
    val validStr: String? = "ADMIN"

    // Trong Java: nullStr.equals(validStr) -> Nổ NullPointerException ngay lập tức!
    // Trong Kotlin: nullStr == validStr -> Dịch thành Intrinsics.areEqual(nullStr, validStr) -> trả về false êm ru!
    println("   -> nullStr == validStr : ${nullStr == validStr}")
    println("   -> nullStr == null     : ${nullStr == null}\n")

    // =================================================================
    // 3. Bẫy Integer Cache (-128 đến 127) của JVM khi dùng `===` với kiểu Nullable `Int?`
    // =================================================================
    println("3. [JVM Integer Cache Trap] Điều gì xảy ra khi dùng `===` với `Int?` (Boxed Integer):")
    val a1: Int? = 100
    val a2: Int? = 100
    val b1: Int? = 10_000
    val b2: Int? = 10_000

    println("   -> Với số 100 (nằm trong Integer Cache -128..127):")
    println("      a1 == a2  là ${a1 == a2}  | a1 === a2 là ${a1 === a2} (JVM tái sử dụng cùng 1 Integer trong cache)")
    println("   -> Với số 10_000 (nằm ngoài Integer Cache):")
    println("      b1 == b2  là ${b1 == b2}  | b1 === b2 là ${b1 === b2} (2 đối tượng Integer khác địa chỉ trên Heap!)")
    println("   => KẾT LUẬN: Luôn dùng `==` để so sánh giá trị! Chỉ dùng `===` khi thực sự muốn kiểm tra 2 biến có trỏ tới đúng 1 instance duy nhất hay không.\n")

    // =================================================================
    // 4. So sánh 2 đối tượng DTO trên Heap
    // =================================================================
    println("4. [Object Equality] So sánh 2 đối tượng DTO khác địa chỉ nhưng cùng dữ liệu:")
    val dto1 = ProductDto(id = 101L, name = "Bàn phím cơ Keychron")
    val dto2 = ProductDto(id = 101L, name = "Bàn phím cơ Keychron")

    println("   -> dto1 == dto2  (Structural - gọi equals()): ${dto1 == dto2}")
    println("   -> dto1 === dto2 (Referential - so địa chỉ):  ${dto1 === dto2}")

    println("\n--- ___ * END: Ex05EqualityAndMemoryPool * ___ ---")
}
