package view

/**
 * =====================================================================
 * LESSON 01 - EX05: SO SÁNH BẰNG (== VS ===) & BỘ NHỚ STRING POOL
 * =====================================================================
 *
 * 1. BẢN CHẤT (STRUCTURAL EQUALITY VS REFERENTIAL EQUALITY):
 *    - Trong Java:
 *      + `a == b` là so sánh **Địa chỉ tham chiếu (Referential Equality)** đối với kiểu Object.
 *        Rất nhiều lập trình viên Java mắc lỗi kinh điển: dùng `str1 == str2` để so sánh chuỗi!
 *      + `a.equals(b)` mới là so sánh **Giá trị nội dung (Structural Equality)**,
 *        nhưng nếu `a == null` thì gọi `a.equals(b)` sẽ nổ `NullPointerException`!
 *
 *    - Trong Kotlin (ĐẢO NGƯỢC ĐỂ ƯU TIÊN AN TOÀN VỀ GIÁ TRỊ):
 *      + `a == b` (2 dấu bằng) là **Structural Equality (So sánh giá trị nội dung qua `equals()`)**!
 *        Dưới tầng Bytecode, `a == b` được Kotlin dịch thành:
 *        `a?.equals(b) ?: (b === null)` (tức là `Intrinsics.areEqual(a, b)`).
 *        => Kể cả khi `a` là `null`, phép `a == b` trong Kotlin VẪN CHẠY AN TOÀN, không nổ NPE!
 *      + `a === b` (3 dấu bằng) mới là **Referential Equality (So sánh 2 tham chiếu có cùng trỏ tới đúng 1 đối tượng trên HEAP hay không)**!
 *        Dưới tầng Bytecode, `a === b` dịch thẳng thành `a == b` của Java!
 *
 * 2. SO SÁNH JAVA VS KOTLIN:
 *    | Mục đích so sánh                     | Cú pháp Java                   | Cú pháp Kotlin |
 *    |--------------------------------------|--------------------------------|----------------|
 *    | So sánh nội dung (equals)            | `Objects.equals(a, b)`         | `a == b`       |
 *    | So sánh khác nội dung                | `!Objects.equals(a, b)`        | `a != b`       |
 *    | So sánh cùng địa chỉ tham chiếu      | `a == b`                       | `a === b`      |
 *    | So sánh khác địa chỉ tham chiếu      | `a != b`                       | `a !== b`      |
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
    println("1. [String Pool vs Heap] Phan biet ro `==` (noi dung) va `===` (dia chi tham chieu):")

    // s1 và s2 cùng trỏ tới 1 hằng chuỗi duy nhất trong String Pool (trên Heap)
    val s1: String = "KotlinSpring"
    val s2: String = "KotlinSpring"

    // s3 được khởi tạo tường minh bằng constructor -> ép JVM cấp phát một Object MỚI trên Heap
    val s3: String = String(charArrayOf('K', 'o', 't', 'l', 'i', 'n', 'S', 'p', 'r', 'i', 'n', 'g'))

    check(s1 == s2 && s1 === s2)
    check(s1 == s3 && s1 !== s3)
    println("   -> s1 = \"$s1\", s2 = \"$s2\", s3 = \"$s3\"")
    println("   -> [s1 == s2]  (So sanh noi dung):  ${s1 == s2}")
    println("   -> [s1 === s2] (So sanh dia chi):   ${s1 === s2} (Cung tro vao String Pool)")
    println("   -> [s1 == s3]  (So sanh noi dung):  ${s1 == s3} (Noi dung giong het nhau -> dung `==` an toan!)")
    println("   -> [s1 === s3] (So sanh dia chi):   ${s1 === s3} (Khac dia chi: s1 o String Pool, s3 la Object moi tren Heap)\n")

    // =================================================================
    // 2. An toàn với Null khi dùng `==` trong Kotlin
    // =================================================================
    println("2. [Null-Safe Equality] Phep `a == b` khong nem NullPointerException:")
    val nullStr: String? = null
    val validStr: String? = "ADMIN"

    // Trong Java: nullStr.equals(validStr) -> Nổ NullPointerException ngay lập tức!
    // Trong Kotlin: nullStr == validStr -> Dịch thành Intrinsics.areEqual(nullStr, validStr) -> trả về false êm ru!
    println("   -> nullStr == validStr : ${nullStr == validStr}")
    println("   -> nullStr == null     : ${nullStr == null}\n")

    // =================================================================
    // 3. Bẫy Integer Cache (-128 đến 127) của JVM khi dùng `===` với kiểu Nullable `Int?`
    // =================================================================
    println("3. [JVM Integer Cache Trap] Dieu gi xay ra khi dung `===` voi `Int?` (Boxed Integer):")
    val a1: Int? = 100
    val a2: Int? = 100
    val b1: Int? = 10_000
    val b2: Int? = 10_000

    println("   -> Voi so 100 (nam trong Integer Cache -128..127):")
    println("      a1 == a2  la ${a1 == a2}  | a1 === a2 la ${a1 === a2} (JVM tai su dung cung 1 Integer trong cache)")
    println("   -> Voi so 10_000 (nam ngoai Integer Cache):")
    println("      b1 == b2  la ${b1 == b2}  | b1 === b2 la ${b1 === b2} (2 doi tuong Integer khac dia chi tren Heap!)")
    println("   => KET LUAN: Nen dung `==` de so sanh gia tri; chi dung `===` khi muon kiem tra 2 bien co tro toi cung 1 instance hay khong.\n")

    // =================================================================
    // 4. So sánh 2 đối tượng DTO trên Heap
    // =================================================================
    println("4. [Object Equality] So sanh 2 doi tuong DTO khac dia chi nhung cung du lieu:")
    val dto1 = ProductDto(id = 101L, name = "Ban phim co Keychron")
    val dto2 = ProductDto(id = 101L, name = "Ban phim co Keychron")

    check(dto1 == dto2 && dto1 !== dto2)
    println("   -> dto1 == dto2  (Structural - goi equals()): ${dto1 == dto2}")
    println("   -> dto1 === dto2 (Referential - so dia chi):  ${dto1 === dto2}")

    println("\n--- ___ * END: Ex05EqualityAndMemoryPool * ___ ---")
}
