package view

import java.time.LocalDateTime

/**
 * BÀI 03: TỔNG HỢP KIẾN TRÚC SPRING BOOT KOTLIN TRONG THỰC TẾ (ENTITY -> REPOSITORY -> SERVICE -> CONTROLLER)
 *
 * Bài học này là bức tranh ghép hoàn chỉnh (Blueprint) kết nối tất cả những gì bạn đã học từ Bài 01 đến Bài 11
 * vào một luồng xử lý chuẩn mực của **Spring Boot + Spring Data JPA + Bean Validation + Kotlin**.
 *
 * 1. HAI COMPILER PLUGINS BẮT BUỘC TRONG `build.gradle.kts` CỦA DỰ ÁN SPRING BOOT KOTLIN:
 *    a) **`kotlin("plugin.spring")` (tên khác: `all-open` plugin)**:
 *       - *Vấn đề*: Trong Kotlin, mọi `class` và `fun` mặc định là **`final`**! Nhưng Spring AOP
 *         (`@Configuration`, `@Service`, `@Transactional`, `@Async`, `@Cacheable`) lại dùng **CGLIB Proxy**
 *         tạo một lớp con kế thừa từ class của bạn lúc Runtime để chèn logic Transaction/Security!
 *         Nếu class là `final`, CGLIB sẽ báo lỗi: *"Cannot subclass final class UserService"*!
 *       - *Giải pháp*: Plugin `kotlin("plugin.spring")` tự động thêm từ khóa `open` vào Bytecode cho mọi class/method
 *         có gắn `@Component`, `@Service`, `@Repository`, `@Controller`, `@Configuration`, `@Transactional`
 *         mà bạn không cần tự gõ chữ `open` làm xấu code!
 *
 *    b) **`kotlin("plugin.jpa")` (tên khác: `no-arg` plugin)**:
 *       - *Vấn đề*: Hibernate/JPA bắt buộc mọi `@Entity` phải có một **Default No-Arg Constructor** (`protected` hoặc `public`)
 *         để khởi tạo đối tượng qua Reflection khi đọc từ DB, đồng thời `@Entity` không được `final` để hỗ trợ Lazy Loading Proxy.
 *       - *Giải pháp*: Plugin `kotlin("plugin.jpa")` tự động sinh thêm một constructor không tham số ẩn trong Bytecode
 *         cho các class gắn `@Entity`, `@MappedSuperclass`, `@Embeddable`!
 *
 * 2. NĂM QUY TẮC VÀNG (BEST PRACTICES) TRONG SPRING BOOT KOTLIN:
 *    - **Quy tắc 1 (Constructor Injection)**: Không dùng `@Autowired lateinit var`. Luôn khai báo dependencies
 *      trong Primary Constructor với `private val`:
 *      `class UserService(private val userRepository: UserRepository)`
 *    - **Quy tắc 2 (JPA Entity)**: **KHÔNG dùng `data class` cho `@Entity`**! Dùng `class` thường (hoặc `open class`),
 *      các trường có thể thay đổi khai báo `var`, và cài đặt `equals`/`hashCode` chỉ dựa trên khóa chính `id`.
 *    - **Quy tắc 3 (Request/Response DTO)**: **LUÔN dùng `data class` với toàn bộ thuộc tính là `val` (Immutable)**,
 *      và nhớ gắn **`@field:NotBlank`**, **`@field:Email`**, **`@field:Valid`**!
 *    - **Quy tắc 4 (Mapper)**: Thay vì tạo class `UserMapper` rườm rà hay cấu hình MapStruct phức tạp,
 *      hãy dùng **Extension Functions**: `fun UserEntity.toResponse(): UserResponse`.
 *    - **Quy tắc 5 (Null-safety trong Repository)**: Khai báo hàm trong Spring Data Repository trả về trực tiếp
 *      kiểu Nullable `fun findByEmail(email: String): UserEntity?` (thay vì `Optional<UserEntity>` của Java),
 *      sau đó kết hợp toán tử Elvis: `val user = userRepository.findByIdOrNull(id) ?: throw NotFoundException(...)`!
 */

// =========================================================================
// CÁC ANNOTATION MÔ PHỎNG SPRING BOOT & JAKARTA JPA/VALIDATION
// =========================================================================
@Target(AnnotationTarget.CLASS)
annotation class Entity(val tableName: String)

@Target(AnnotationTarget.CLASS)
annotation class Service

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Transactional(val readOnly: Boolean = false)

@Target(AnnotationTarget.CLASS)
annotation class RestController(val basePath: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class NotBlank(val message: String)

// =========================================================================
// 1. DOMAIN ENTITY (JPA `@Entity` — dùng `open class`, KHÔNG dùng `data class`)
// =========================================================================
@Entity(tableName = "users")
open class UserEntity(
    val id: String,
    var fullName: String,
    var email: String,
    var active: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // Trong JPA Entity, chỉ so sánh equals/hashCode theo ID để không làm hỏng Hibernate Persistence Context Set
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "UserEntity(id='$id', fullName='$fullName', email='$email', active=$active)"
}

// =========================================================================
// 2. REQUEST / RESPONSE DTOs (Dùng `data class` bất biến `val` + `@field:`)
// =========================================================================
data class CreateUserRequest(
    @field:NotBlank(message = "Họ tên không được để trống")
    val fullName: String,

    @field:NotBlank(message = "Email không được để trống")
    val email: String
)

data class UserResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val statusLabel: String
)

// Wrapper phản hồi chuẩn RESTful API với Covariant `out T`
data class ApiResponse<out T>(
    val success: Boolean,
    val data: T? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun fail(message: String): ApiResponse<Nothing> = ApiResponse(success = false, errorMessage = message)
    }
}

// =========================================================================
// 3. EXTENSION MAPPER (Thay thế các class Mapper boilerplate trong Java)
// =========================================================================
fun UserEntity.toResponse(): UserResponse = UserResponse(
    id = this.id,
    fullName = this.fullName,
    email = this.email,
    statusLabel = if (this.active) "ACTIVE" else "DEACTIVATED"
)

// =========================================================================
// 4. REPOSITORY LAYER (Trả về `UserEntity?` thay vì `Optional<UserEntity>`)
// =========================================================================
interface UserRepository {
    fun findByIdOrNull(id: String): UserEntity?
    fun existsByEmail(email: String): Boolean
    fun save(user: UserEntity): UserEntity
    fun findAll(): List<UserEntity>
}

class InMemoryUserRepository : UserRepository {
    private val storage = linkedMapOf<String, UserEntity>()

    override fun findByIdOrNull(id: String): UserEntity? = storage[id]

    override fun existsByEmail(email: String): Boolean =
        storage.values.any { it.email.equals(email, ignoreCase = true) }

    override fun save(user: UserEntity): UserEntity {
        storage[user.id] = user
        return user
    }

    override fun findAll(): List<UserEntity> = storage.values.toList()
}

// =========================================================================
// 5. SERVICE LAYER (Constructor Injection `private val` + Preconditions + Elvis `?:`)
// =========================================================================
class ResourceNotFoundException(message: String) : RuntimeException(message)
class DuplicateResourceException(message: String) : RuntimeException(message)

@Service
open class UserService(
    private val userRepository: UserRepository // Constructor Injection chuẩn Spring!
) {
    private var sequence = 1

    @Transactional
    open fun registerUser(request: CreateUserRequest): UserResponse {
        val normalizedName = request.fullName.trim().replace(Regex("\\s+"), " ")
        val normalizedEmail = request.email.trim().lowercase()

        require(normalizedName.isNotEmpty()) { "Họ tên không hợp lệ" }
        require(normalizedEmail.contains("@")) { "Định dạng email không hợp lệ: $normalizedEmail" }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw DuplicateResourceException("Email '$normalizedEmail' đã được đăng ký trong hệ thống!")
        }

        val generatedId = "USR-%03d".format(sequence++)
        val entity = UserEntity(
            id = generatedId,
            fullName = normalizedName,
            email = normalizedEmail
        )
        // Lưu DB và dùng Extension Function `.toResponse()` ngay trên kết quả!
        return userRepository.save(entity).toResponse()
    }

    @Transactional(readOnly = true)
    open fun getUserById(id: String): UserResponse {
        // Elvis operator `?: throw ...` thay thế hoàn toàn `.orElseThrow(() -> ...)` của Java Optional!
        val entity = userRepository.findByIdOrNull(id.trim().uppercase())
            ?: throw ResourceNotFoundException("Không tìm thấy người dùng với mã: $id")
        return entity.toResponse()
    }

    @Transactional
    open fun deactivateUser(id: String): UserResponse {
        val entity = userRepository.findByIdOrNull(id.trim().uppercase())
            ?: throw ResourceNotFoundException("Không tìm thấy người dùng với mã: $id")
        check(entity.active) { "Tài khoản [${entity.id}] đã bị vô hiệu hóa từ trước!" }
        entity.active = false
        return userRepository.save(entity).toResponse()
    }
}

// =========================================================================
// 6. CONTROLLER LAYER (`@RestController` + Global Exception Handling giả lập)
// =========================================================================
@RestController(basePath = "/api/v1/users")
class UserController(
    private val userService: UserService
) {
    fun createUserEndpoint(request: CreateUserRequest): ApiResponse<UserResponse> =
        executeWithExceptionHandler {
            userService.registerUser(request)
        }

    fun getUserEndpoint(id: String): ApiResponse<UserResponse> =
        executeWithExceptionHandler {
            userService.getUserById(id)
        }

    fun deactivateUserEndpoint(id: String): ApiResponse<UserResponse> =
        executeWithExceptionHandler {
            userService.deactivateUser(id)
        }

    // Mô phỏng `@RestControllerAdvice` bắt lỗi tập trung của Spring Boot
    private inline fun <T> executeWithExceptionHandler(action: () -> T): ApiResponse<T> = try {
        ApiResponse.ok(action())
    } catch (e: Exception) {
        // Nhờ `ApiResponse<out T>`, `ApiResponse<Nothing>` tự động tương thích với `ApiResponse<T>`!
        ApiResponse.fail("[${e::class.simpleName}] ${e.message}")
    }
}

fun main() {
    println("--- ___ * START EX03: SPRING BOOT KOTLIN ARCHITECTURE SIMULATION * ___ ---\n")

    // Khởi tạo Dependency Injection Container thủ công (tương đương Spring ApplicationContext)
    val userRepository: UserRepository = InMemoryUserRepository()
    val userService = UserService(userRepository)
    val userController = UserController(userService)

    // 1. Tạo mới User hợp lệ
    println("1. Gọi POST /api/v1/users (Hợp lệ):")
    val res1 = userController.createUserEndpoint(
        CreateUserRequest(fullName = "  Nguyễn   Minh An ", email = " AN.NGUYEN@Example.com ")
    )
    println("  -> Response: $res1")

    // 2. Tạo trùng Email -> Bị chặn bởi DuplicateResourceException
    println("\n2. Gọi POST /api/v1/users (Trùng Email):")
    val resDuplicate = userController.createUserEndpoint(
        CreateUserRequest(fullName = "Nguyễn Văn An Khác", email = "an.nguyen@example.com")
    )
    println("  -> Response: $resDuplicate")

    // 3. Tra cứu User theo ID (Thành công & Không tìm thấy với Elvis `?: throw`)
    println("\n3. Gọi GET /api/v1/users/{id}:")
    println("  - Tìm USR-001: ${userController.getUserEndpoint("usr-001")}")
    println("  - Tìm USR-999: ${userController.getUserEndpoint("USR-999")}")

    // 4. Vô hiệu hóa tài khoản & kiểm tra `check(entity.active)`
    println("\n4. Gọi PATCH /api/v1/users/USR-001/deactivate:")
    println("  - Lần 1 (Hợp lệ): ${userController.deactivateUserEndpoint("USR-001")}")
    println("  - Lần 2 (Vi phạm `check` state): ${userController.deactivateUserEndpoint("USR-001")}")

    println("\n--- ___ * END EX03 * ___ ---")
}
