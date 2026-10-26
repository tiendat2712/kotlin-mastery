package view

import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * =====================================================================
 * LESSON 04 - EX05: EXTENSION FUNCTIONS & EXTENSION PROPERTIES (VŨ KHÍ TỐI THƯỢNG)
 * =====================================================================
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java, khi muốn bổ sung hàm tiện ích cho một class có sẵn (như `String`, `BigDecimal`, `LocalDate`
 *      vốn là các class `final` không thể kế thừa), bạn buộc phải tạo các class `StringUtils`, `CurrencyUtils`,
 *      `UserMapperUtils` chứa đầy hàm `public static`!
 *      Hệ quả: Khi viết code phải nhớ tên class tiện ích (`StringUtils.toSlug(title)`), và không được IDE
 *      gợi ý trực tiếp khi gõ dấu chấm `title.`!
 *    - Trong Kotlin, **Extension Function (`fun String.toSlug(): String`)** cho phép bạn "gắn thêm" phương thức
 *      mới vào bất kỳ class nào (kể cả class `final` của JDK hay thư viện bên thứ 3)!
 *    - **BÍ MẬT DƯỚI TẦNG BYTECODE**:
 *      + Extension Function **KHÔNG HỀ sửa đổi file `.class` gốc của `java.lang.String`**!
 *      + Trình biên dịch Kotlin biên dịch `fun String.toSlug(): String` thành một **hàm `public static final`**
 *        với đối tượng đứng trước dấu chấm (gọi là **Receiver `this`**) trở thành **tham số đầu tiên**:
 *        `public static final String toSlug(@NotNull String $this$toSlug)`!
 *      + Vì là hàm `static`, Extension Function được phân giải tĩnh tại thời điểm biên dịch (**Statically Resolved**).
 *      + **Extension Property (`val String.wordCount: Int get() = ...`)**: Vì không thể chèn thêm biến trường (field)
 *        vào đối tượng trên Heap, Extension Property **không có Backing Field** (chỉ được định nghĩa thông qua hàm `get()`).
 *
 * 2. SPRING KOTLIN BEST PRACTICE — MẪU THIẾT KẾ MAPPER `Entity.toDto()`:
 *    - Trong 99% các dự án Spring Boot Kotlin chuyên nghiệp, lập trình viên **KHÔNG viết class `UserMapper`**
 *      rườm rà nữa, mà định nghĩa luôn Extension Function:
 *      `fun UserEntity.toDto(): UserDto = UserDto(...)`
 *    - Khi ở tầng `@Service`, bạn chỉ cần viết: `userRepository.findAll().map { it.toDto() }`
 *      hoặc `return savedEntity.toDto()` — cực kỳ sạch sẽ, tự nhiên và chuẩn Clean Architecture!
 * =====================================================================
 */

// Mô phỏng JPA Entity ở tầng Database và Response DTO ở tầng REST API trong Spring Boot
private class UserEntity(
    val id: Long,
    val fullName: String,
    val email: String,
    val passwordHash: String, // Trường nhạy cảm KHÔNG ĐƯỢC lộ ra API!
    val balance: BigDecimal
)

private data class UserResponseDto(
    val id: Long,
    val fullName: String,
    val slugProfileUrl: String,
    val email: String,
    val formattedBalance: String
)

// =====================================================================
// CÁC HÀM & THUỘC TÍNH MỞ RỘNG (EXTENSIONS)
// =====================================================================

/**
 * 1. Extension Function trên `String`: Chuyển tiêu đề tiếng Việt/tiếng Anh thành URL Slug chuẩn SEO.
 * Dưới Bytecode: `public static final String toSlug(String $this)`
 */
fun String.toSlug(): String {
    // Bên trong Extension Function, `this` chính là đối tượng String đứng trước dấu chấm!
    return this.trim()
        .lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
}

/**
 * 2. Extension Function trên `BigDecimal`: Định dạng số tiền sang chuỗi VND có dấu phẩy ngăn cách.
 */
fun BigDecimal.formatVnd(): String {
    val formatter = DecimalFormat("#,###")
    return "${formatter.format(this)} VND"
}

/**
 * 3. Extension Property trên `String`: Đếm số từ trong chuỗi.
 * Lưu ý: Extension Property không lưu trạng thái trên Heap nên bắt buộc phải viết custom `get()`.
 */
val String.wordCount: Int
    get() = this.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size

/**
 * 4. Extension Function trên kiểu Nullable `String?`: Che dấu thông tin nhạy cảm an toàn với cả `null`!
 */
fun String?.maskEmailSafely(): String {
    if (this.isNullOrBlank() || !this.contains("@")) return "***@hidden"
    val namePart = this.substringBefore("@")
    val domainPart = this.substringAfter("@")
    val visiblePrefix = namePart.take(2)
    return "${visiblePrefix}***@$domainPart"
}

/**
 * 5. [SPRING KOTLIN IDIOM] Extension Function chuyển đổi từ JPA Entity sang Response DTO:
 */
private fun UserEntity.toDto(): UserResponseDto = UserResponseDto(
    id = this.id,
    fullName = this.fullName,
    slugProfileUrl = "/users/${this.fullName.toSlug()}-${this.id}",
    email = this.email.maskEmailSafely(),
    formattedBalance = this.balance.formatVnd()
)

fun main() {
    println("--- ___ * START: Ex05ExtensionFunctionsAndProperties * ___ ---\n")

    // =================================================================
    // 1. Gọi Extension Functions trực tiếp trên String và BigDecimal
    // =================================================================
    println("1. [Extension Functions] Gọi `.toSlug()` và `.formatVnd()` như hàm có sẵn của JDK:")
    val articleTitle = "  Kotlin Spring Boot 2026: Từ Cơ Bản Đến Thực Chiến!  "
    val coursePrice = BigDecimal("1850000")

    println("   -> Tiêu đề gốc:   \"$articleTitle\"")
    println("   -> articleTitle.toSlug():    \"${articleTitle.toSlug()}\"")
    println("   -> coursePrice.formatVnd():  \"${coursePrice.formatVnd()}\"\n")

    // =================================================================
    // 2. Sử dụng Extension Property `.wordCount`
    // =================================================================
    println("2. [Extension Property] Truy cập thuộc tính mở rộng `.wordCount` trên String:")
    println("   -> Số từ trong tiêu đề: ${articleTitle.wordCount} từ\n")

    // =================================================================
    // 3. Extension trên kiểu Nullable (`String?.maskEmailSafely()`)
    // =================================================================
    println("3. [Nullable Receiver Extension] Gọi hàm mở rộng ngay cả khi biến mang giá trị `null`:")
    val nullEmail: String? = null
    val realEmail: String? = "dat.phan@kotlinjourney.vn"
    println("   -> nullEmail.maskEmailSafely() = ${nullEmail.maskEmailSafely()}")
    println("   -> realEmail.maskEmailSafely() = ${realEmail.maskEmailSafely()}\n")

    // =================================================================
    // 4. [Spring Boot Kotlin Idiom] Ánh xạ `UserEntity.toDto()` cực gọn
    // =================================================================
    println("4. [Spring Boot Entity -> DTO Mapper] Chuyển đổi Entity sang DTO bằng `.toDto()`:")
    val dbEntity = UserEntity(
        id = 1001L,
        fullName = "Phan Trịnh Tiến Đạt",
        email = "tiendat@springkotlin.vn",
        passwordHash = "\$2a\$10\$SecretBcryptHashDoNotExpose",
        balance = BigDecimal("25600000")
    )

    // Nhìn dòng code ở tầng Service/Controller: ngắn gọn, an toàn, ẩn hoàn toàn `passwordHash`!
    val responseDto: UserResponseDto = dbEntity.toDto()
    println("   -> Kết quả `dbEntity.toDto()`:")
    println("      $responseDto")

    println("\n--- ___ * END: Ex05ExtensionFunctionsAndProperties * ___ ---")
}
