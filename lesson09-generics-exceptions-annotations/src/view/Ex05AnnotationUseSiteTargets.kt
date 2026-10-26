package view

import java.math.BigDecimal

/**
 * BÀI 05: ANNOTATION USE-SITE TARGETS (`@field:`, `@get:`, `@param:`) — "CẠM BẪY SỐ 1" KHI VIẾT SPRING BOOT KOTLIN!
 *
 * 1. BẢN CHẤT (UNDER THE HOOD / BYTECODE):
 *    - Trong Java, khi viết một DTO class, bạn khai báo tách biệt:
 *      (1) Biến trường (Field): `private final String sku;`
 *      (2) Tham số hàm tạo (Constructor Parameter): `public CreateProductRequest(String sku)`
 *      (3) Hàm đọc (Getter): `public String getSku()`
 *      Vì chúng tách rời nhau trong mã nguồn Java, bạn gắn `@NotBlank` vào đâu thì nó nằm đúng ở đó!
 *
 *    - Trong Kotlin, khi bạn khai báo cực gọn trong Primary Constructor:
 *      ```kotlin
 *      data class CreateProductRequest(val sku: String, val price: BigDecimal)
 *      ```
 *      **CHỈ MỘT DÒNG CODE ĐÓ** sẽ được trình biên dịch Kotlin sinh ra **3 THÀNH PHẦN** trong JVM Bytecode:
 *      (1) **Constructor Parameter**: `String sku` trong `public CreateProductRequest(String sku, ...)`
 *      (2) **Private Backing Field**: `private final String sku;`
 *      (3) **Public Getter Method**: `public final String getSku()` (và `setSku` nếu là `var`)
 *
 *    - **VẤN ĐỀ XẢY RA KHI GẮN ANNOTATION**:
 *      Khi bạn viết `@MyNotBlank val sku: String` mà không chỉ định đích ngắm (Use-Site Target),
 *      Kotlin sẽ gắn `@MyNotBlank` vào thành phần nào trong 3 thành phần trên?
 *      -> **Quy tắc ưu tiên mặc định của Kotlin**: Nếu Annotation đó cho phép gắn vào nhiều nơi
 *         (như `@NotBlank`, `@Min`, `@Positive`, `@Valid` của Jakarta Bean Validation cho phép gắn lên cả
 *         `FIELD`, `METHOD`, `PARAMETER`), Kotlin sẽ chọn theo thứ tự ưu tiên:
 *         **`param` (Constructor Parameter) -> `property` (Kotlin metadata) -> `field` (Java Backing Field)**!
 *      -> HẬU QUẢ: `@NotBlank val sku: String` sẽ bị gắn vào **Constructor Parameter**, trong khi
 *         Java Reflection / Hibernate Validator / Jackson đôi khi quét **Backing Field (`java.lang.reflect.Field`)**
 *         hoặc **Getter Method**, dẫn đến việc **`@NotBlank` hoặc `@Valid` KHÔNG CHẠY (Validation bị bỏ qua âm thầm)!**
 *
 * 2. CÚ PHÁP ANNOTATION USE-SITE TARGETS TRONG KOTLIN:
 *    - `@field:Ann`    -> Gắn lên biến trường Java (`private final T field`) -> **Dùng cho `@field:NotBlank`, `@field:Valid`!**
 *    - `@get:Ann`      -> Gắn lên hàm getter Java (`public final T getXxx()`) -> Dùng cho Jackson `@get:JsonProperty`
 *    - `@set:Ann`      -> Gắn lên hàm setter Java (`public final void setXxx(...)`)
 *    - `@param:Ann`    -> Gắn lên tham số hàm tạo (`Constructor Parameter`)
 *    - `@property:Ann` -> Gắn lên thuộc tính Kotlin (chỉ đọc được qua `kotlin-reflect`, Java Reflection KHÔNG thấy!)
 *
 * 3. SPRING KOTLIN CORNER (QUY TẮC SỐNG CÒN):
 *    - Khi dùng `spring-boot-starter-validation` (Jakarta Validation / Hibernate Validator) trên `data class` DTO:
 *      **LUÔN VIẾT `@field:NotBlank`, `@field:NotNull`, `@field:Size`, `@field:Min`, `@field:Valid`**!
 *      ```kotlin
 *      data class CreateProductRequest(
 *          @field:NotBlank(message = "SKU không được để trống")
 *          val sku: String,
 *          @field:Positive(message = "Giá bán phải > 0")
 *          val price: BigDecimal
 *      )
 *      ```
 */

// Định nghĩa một Annotation mô phỏng chính xác `@jakarta.validation.constraints.NotBlank`
// (Cho phép gắn trên cả PARAMETER, FIELD và FUNCTION/GETTER giống hệt Jakarta Validation)
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class FakeNotBlank(val message: String = "Không được để trống")

// 1. DTO SAI CÁCH (Quên `@field:`):
// Do `FakeNotBlank` hỗ trợ `VALUE_PARAMETER`, Kotlin mặc định gắn nó vào Constructor Parameter!
// Backing Field `java.lang.reflect.Field` sẽ KHÔNG HỀ CÓ annotation này!
data class BrokenProductRequest(
    @FakeNotBlank(message = "Mã SKU không được để trống")
    val sku: String,
    val price: BigDecimal
)

// 2. DTO ĐÚNG CHUẨN SPRING BOOT KOTLIN (Dùng `@field:` và `@get:` tường minh):
data class CorrectProductRequest(
    @field:FakeNotBlank(message = "Mã SKU không được để trống")
    val sku: String,

    @get:FakeNotBlank(message = "Getter minh họa gắn @get:")
    val price: BigDecimal
)

// Hàm giả lập bộ kiểm tra kiểu Hibernate Validator / Framework quét qua Java Reflection Fields
fun inspectJavaBytecodeAnnotations(clazz: Class<*>) {
    println("  === Kiểm tra Bytecode Reflection cho class: ${clazz.simpleName} ===")

    // 1. Quét Constructor Parameters
    val primaryCtor = clazz.declaredConstructors.first()
    primaryCtor.parameters.forEachIndexed { idx, param ->
        val ann = param.getAnnotation(FakeNotBlank::class.java)
        println("    [1. Constructor Param #$idx] : @FakeNotBlank = ${ann?.message ?: "KHÔNG CÓ"}")
    }

    // 2. Quét Java Backing Fields (`java.lang.reflect.Field`) - Nơi nhiều thư viện Java quét!
    clazz.declaredFields.forEach { field ->
        val ann = field.getAnnotation(FakeNotBlank::class.java)
        println("    [2. Java Backing Field '${field.name}'] : @FakeNotBlank = ${ann?.message ?: "KHÔNG CÓ (!)"}")
    }

    // 3. Quét Java Getter Methods (`java.lang.reflect.Method`)
    clazz.declaredMethods
        .filter { it.name.startsWith("get") }
        .forEach { method ->
            val ann = method.getAnnotation(FakeNotBlank::class.java)
            println("    [3. Java Getter Method '${method.name}()'] : @FakeNotBlank = ${ann?.message ?: "KHÔNG CÓ"}")
        }
}

// Bộ Validator đơn giản quét trên Java Field (giống cách nhiều framework xử lý field-level validation)
fun validateFieldsUsingReflection(dto: Any): List<String> {
    val errors = mutableListOf<String>()
    for (field in dto.javaClass.declaredFields) {
        val notBlankAnn = field.getAnnotation(FakeNotBlank::class.java) ?: continue
        field.isAccessible = true
        val value = field.get(dto) as? String
        if (value.isNullOrBlank()) {
            errors.add("Trường '${field.name}' vi phạm: ${notBlankAnn.message}")
        }
    }
    return errors
}

fun main() {
    println("--- ___ * START EX05: ANNOTATION USE-SITE TARGETS * ___ ---\n")

    // =========================================================================
    // 1. SOI BYTECODE REFLECTION: VÌ SAO QUÊN `@field:` LẠI NGUY HIỂM?
    // =========================================================================
    println("1. So sánh vị trí gắn Annotation trong Bytecode giữa 2 cách khai báo:")
    inspectJavaBytecodeAnnotations(BrokenProductRequest::class.java)
    println()
    inspectJavaBytecodeAnnotations(CorrectProductRequest::class.java)

    // =========================================================================
    // 2. CHỨNG MINH HẬU QUẢ KHI VALIDATE DỮ LIỆU RỖNG (`sku = "   "`)
    // =========================================================================
    println("\n2. Thử nghiệm truyền `sku = \"   \"` (rỗng) vào Validator quét Field:")

    val brokenDto = BrokenProductRequest(sku = "   ", price = BigDecimal("150000"))
    val brokenErrors = validateFieldsUsingReflection(brokenDto)
    println("  - Kết quả validate BrokenProductRequest (quên @field:): " +
            if (brokenErrors.isEmpty()) "LỌT LƯỚI VALIDATION (0 lỗi được phát hiện)!" else brokenErrors)

    val correctDto = CorrectProductRequest(sku = "   ", price = BigDecimal("150000"))
    val correctErrors = validateFieldsUsingReflection(correctDto)
    println("  - Kết quả validate CorrectProductRequest (có @field:): $correctErrors")

    println("\n--- ___ * END EX05 * ___ ---")
}
