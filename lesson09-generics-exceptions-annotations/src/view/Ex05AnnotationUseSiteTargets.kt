package view

import java.math.BigDecimal

/**
 * BÀI 05: ANNOTATION USE-SITE TARGETS (`@field:`, `@get:`, `@param:`) — CẠM BẪY PHỔ BIẾN KHI VIẾT SPRING BOOT KOTLIN
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
 *    - **ĐIỀU GÌ XẢY RA KHI GẮN ANNOTATION KHÔNG KÈM ĐÍCH NGẮM (USE-SITE TARGET)?**
 *      Khi bạn viết `@MyNotBlank val sku: String` trong Primary Constructor mà không chỉ định Use-Site Target:
 *      + **Trong Kotlin 2.0.21 (phiên bản dùng trong repo này)**, khi `@Target` của Annotation cho phép gắn vào nhiều nơi
 *        (ví dụ `@NotBlank`, `@Min`, `@Positive`, `@Valid` của Jakarta Bean Validation hỗ trợ cả
 *        `PARAMETER`, `FIELD`, `GETTER`), trình biên dịch chọn theo thứ tự ưu tiên mặc định:
 *        **`param` (Constructor Parameter) > `property` (Kotlin metadata) > `field` (Java Backing Field)**!
 *      + **Hệ quả thực tế**: `@NotBlank val sku: String` sẽ mặc định gắn vào **Constructor Parameter** (`@param:`),
 *        trong khi các bộ kiểm tra quét theo **Field (`java.lang.reflect.Field`)** hoặc **Getter (`java.lang.reflect.Method`)**
 *        (như cấu hình mặc định khi validate thuộc tính đối tượng hoặc các thư viện Reflection) sẽ không nhìn thấy
 *        annotation trên Field/Getter nếu không kích hoạt Constructor Validation hoặc cấu hình tương ứng!
 *      + **Lưu ý về phiên bản Kotlin & cờ trình biên dịch**:
 *        Trong lộ trình phát triển của Kotlin K2 (các bản Kotlin tương lai hoặc khi bật cờ trình biên dịch như
 *        `-Xannotation-default-target=param-property`), hành vi chọn đích mặc định có thể thay đổi.
 *        Do đó, việc nắm vững `@field:`, `@get:`, `@param:` là kỹ năng thiết yếu để chẩn đoán và phòng tránh lỗi
 *        Validation / Serialization bị bỏ qua âm thầm!
 *
 * 2. CÚ PHÁP ANNOTATION USE-SITE TARGETS TRONG KOTLIN:
 *    - `@field:Ann`    -> Gắn lên biến trường Java (`private final T field`) -> Thường dùng cho `@field:NotBlank`, `@field:Valid`
 *    - `@get:Ann`      -> Gắn lên hàm getter Java (`public final T getXxx()`) -> Thường dùng cho Jackson `@get:JsonProperty` hoặc Getter Validation
 *    - `@set:Ann`      -> Gắn lên hàm setter Java (`public final void setXxx(...)`)
 *    - `@param:Ann`    -> Gắn lên tham số hàm tạo (`Constructor Parameter`) -> Dùng cho `@JsonCreator` parameter hoặc Constructor Validation
 *    - `@property:Ann` -> Gắn lên thuộc tính Kotlin (chỉ đọc được qua `kotlin-reflect`, Java Reflection chuẩn không đọc trực tiếp được)
 *
 * 3. SPRING KOTLIN CORNER (KHUYẾN NGHỊ THỰC HÀNH):
 *    - Khi dùng `spring-boot-starter-validation` (Jakarta Validation / Hibernate Validator) trên `data class` Request DTO
 *      trong Kotlin 2.0.x, bạn nên chỉ định rõ `@field:` (hoặc `@get:`) cho các ràng buộc trên thuộc tính constructor:
 *      ```kotlin
 *      data class CreateProductRequest(
 *          @field:NotBlank(message = "SKU khong duoc de trong")
 *          val sku: String,
 *          @field:Positive(message = "Gia ban phai > 0")
 *          val price: BigDecimal
 *      )
 *      ```
 */

// Định nghĩa một Annotation mô phỏng `@jakarta.validation.constraints.NotBlank`
// (Cho phép gắn trên cả PARAMETER, FIELD và PROPERTY_GETTER giống Jakarta Validation)
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class FakeNotBlank(val message: String = "Khong duoc de trong")

// 1. DTO KHÔNG CHỈ ĐỊNH USE-SITE TARGET (Quên `@field:` trong Kotlin 2.0.21):
// Do `FakeNotBlank` hỗ trợ `VALUE_PARAMETER`, Kotlin 2.0.21 mặc định gắn nó vào Constructor Parameter (`@param:`)!
// Backing Field `java.lang.reflect.Field` sẽ không có annotation này!
data class BrokenProductRequest(
    @FakeNotBlank(message = "Ma SKU khong duoc de trong")
    val sku: String,
    val price: BigDecimal
)

// 2. DTO CHỈ ĐỊNH TƯỜNG MINH `@field:` VÀ `@get:`:
data class CorrectProductRequest(
    @field:FakeNotBlank(message = "Ma SKU khong duoc de trong")
    val sku: String,

    @get:FakeNotBlank(message = "Getter minh hoa gan @get:")
    val price: BigDecimal
)

// Hàm giả lập bộ kiểm tra quét qua Java Reflection (Constructor Param vs Field vs Getter)
fun inspectJavaBytecodeAnnotations(clazz: Class<*>) {
    println("  === Kiem tra Bytecode Reflection cho class: ${clazz.simpleName} ===")

    // 1. Quét Constructor Parameters
    val primaryCtor = clazz.declaredConstructors.first()
    primaryCtor.parameters.forEachIndexed { idx, param ->
        val ann = param.getAnnotation(FakeNotBlank::class.java)
        println("    [1. Constructor Param #$idx] : @FakeNotBlank = ${ann?.message ?: "KHONG CO"}")
    }

    // 2. Quét Java Backing Fields (`java.lang.reflect.Field`) - Nơi nhiều thư viện Java quét!
    clazz.declaredFields.forEach { field ->
        val ann = field.getAnnotation(FakeNotBlank::class.java)
        println("    [2. Java Backing Field '${field.name}'] : @FakeNotBlank = ${ann?.message ?: "KHONG CO (!)"}")
    }

    // 3. Quét Java Getter Methods (`java.lang.reflect.Method`)
    clazz.declaredMethods
        .filter { it.name.startsWith("get") }
        .forEach { method ->
            val ann = method.getAnnotation(FakeNotBlank::class.java)
            println("    [3. Java Getter Method '${method.name}()'] : @FakeNotBlank = ${ann?.message ?: "KHONG CO"}")
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
            errors.add("Truong '${field.name}' vi pham: ${notBlankAnn.message}")
        }
    }
    return errors
}

fun main() {
    println("--- ___ * START EX05: ANNOTATION USE-SITE TARGETS * ___ ---\n")

    // =========================================================================
    // 1. SOI BYTECODE REFLECTION: VÌ SAO THIẾU `@field:` DỄ GÂY LỖI NGẦM?
    // =========================================================================
    println("1. So sanh vi tri gan Annotation trong Bytecode giua 2 cach khai bao:")
    inspectJavaBytecodeAnnotations(BrokenProductRequest::class.java)
    println()
    inspectJavaBytecodeAnnotations(CorrectProductRequest::class.java)

    // =========================================================================
    // 2. CHỨNG MINH HỆ QUẢ KHI VALIDATE DỮ LIỆU RỖNG (`sku = "   "`) QUA FIELD
    // =========================================================================
    println("\n2. Thu nghiem truyen `sku = \"   \"` (rong) vao Validator quet Field:")

    val brokenDto = BrokenProductRequest(sku = "   ", price = BigDecimal("150000"))
    val brokenErrors = validateFieldsUsingReflection(brokenDto)
    println("  - Ket qua validate BrokenProductRequest (thieu @field:): " +
            if (brokenErrors.isEmpty()) "LOT LUOI FIELD VALIDATION (0 loi duoc phat hien)!" else brokenErrors)
    check(brokenErrors.isEmpty())

    val correctDto = CorrectProductRequest(sku = "   ", price = BigDecimal("150000"))
    val correctErrors = validateFieldsUsingReflection(correctDto)
    println("  - Ket qua validate CorrectProductRequest (co @field:): $correctErrors")
    check(correctErrors.size == 1)

    // =========================================================================
    // 3. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // =========================================================================
    println("\n3. Goc Tu Luyen (Active Learning):")
    // [ĐỀ BÀI]:
    // Cho DTO `CustomerRegisterRequest(val email: String, val phone: String)`.
    // Hãy gắn `@field:FakeNotBlank` cho cả `email` và `phone` (xem class `FixedCustomerRegisterRequest` bên dưới)
    // sao cho khi truyền `email = ""` và `phone = "   "`, hàm `validateFieldsUsingReflection` bắt được đủ 2 lỗi!
    val invalidCustomerReq = FixedCustomerRegisterRequest(email = "", phone = "   ")
    val detectedCustomerErrors = validateFieldsUsingReflection(invalidCustomerReq)
    println("  - So loi phat hien tren FixedCustomerRegisterRequest: ${detectedCustomerErrors.size} -> $detectedCustomerErrors")
    check(detectedCustomerErrors.size == 2)
    println("  => [PASS] Goc Tu Luyen Ex05 hoan tat!")

    println("\n--- ___ * END EX05 * ___ ---")
}

// [LỜI GIẢI THAM KHẢO CHO GÓC TỰ LUYỆN]:
data class FixedCustomerRegisterRequest(
    @field:FakeNotBlank(message = "Email khong duoc de trong")
    val email: String,
    @field:FakeNotBlank(message = "So dien thoai khong duoc de trong")
    val phone: String
)
