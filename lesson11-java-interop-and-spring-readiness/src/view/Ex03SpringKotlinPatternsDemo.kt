package view

import java.time.LocalDateTime

/**
 * BÀI 03: MÔ PHỎNG TỔ CHỨC LỚP THEO PHONG CÁCH SPRING BOOT KOTLIN (ENTITY -> REPOSITORY -> SERVICE -> CONTROLLER)
 *
 * [LƯU Ý QUAN TRỌNG VỀ PHẠM VI MÔ PHỎNG TRONG FILE NÀY]:
 * - Toàn bộ các annotation (`@Entity`, `@Service`, `@Transactional`, `@RestController`, `@NotBlank`) và
 *   `InMemoryUserRepository` bên dưới là **mô phỏng thuần Kotlin phục vụ học tập (pure Kotlin educational simulation)**
 *   để minh họa cách tổ chức lớp, Constructor Injection, Extension Mapper và Null-safety.
 * - File này **KHÔNG** khởi chạy Spring IoC/DI Container thật, **KHÔNG** chạy Hibernate/JPA hay Database Transaction thật,
 *   **KHÔNG** chạy bộ kiểm tra Jakarta Bean Validation tự động và **KHÔNG** mở cổng HTTP Server.
 *
 * 1. HAI COMPILER PLUGINS QUAN TRỌNG TRONG `build.gradle.kts` CỦA DỰ ÁN SPRING BOOT KOTLIN THỰC TẾ:
 *    a) **`kotlin("plugin.spring")` (tức `all-open` plugin cấu hình sẵn cho Spring)**:
 *       - *Bối cảnh*: Trong Kotlin, mọi `class` và `fun` mặc định là **`final`**. Trong khi đó, đối với các class
 *         như `@Configuration`, `@Service`, `@Transactional` (đặc biệt khi tạo class-based proxy bằng CGLIB,
 *         vốn là cơ chế proxy mặc định trong Spring Boot khi không proxy thuần qua JDK interface), Spring cần
 *         tạo lớp con kế thừa từ class của bạn lúc Runtime để đan (weave) logic Transaction/Security/AOP.
 *         Nếu class hoặc hàm là `final`, CGLIB proxy sẽ không thể kế thừa hoặc ghi đè hàm đó!
 *       - *Vai trò của plugin*: `kotlin("plugin.spring")` tự động mở (`open`) ở tầng Bytecode cho các class/method
 *         có gắn `@Component`, `@Service`, `@Repository`, `@Controller`, `@Configuration`, `@Transactional`, `@Async`, `@Cacheable`
 *         mà bạn không cần phải gõ từ khóa `open` thủ công trong mã nguồn.
 *
 *    b) **`kotlin("plugin.jpa")` (tức `no-arg` plugin cấu hình sẵn cho JPA)**:
 *       - *Bối cảnh*: Đặc tả JPA/Hibernate yêu cầu các lớp `@Entity`, `@Embeddable`, `@MappedSuperclass` phải có
 *         một **No-Arg Constructor** (`protected` hoặc `public`) để ORM khởi tạo đối tượng qua Reflection khi đọc dữ liệu từ DB.
 *       - *Vai trò của plugin*: `kotlin("plugin.jpa")` tự động sinh thêm một constructor không tham số tổng hợp (synthetic)
 *         trong Bytecode cho các lớp gắn `@Entity`, `@MappedSuperclass`, `@Embeddable` (đồng thời kết hợp tốt với `all-open`
 *         để tránh `final` làm cản trở Lazy Loading Proxy của Hibernate).
 *
 * 2. NĂM MẪU THIẾT KẾ (PATTERNS) PHỔ BIẾN TRONG SPRING BOOT KOTLIN:
 *    - **Mẫu 1 (Constructor Injection)**: Ưu tiên khai báo dependencies trực tiếp trong Primary Constructor với `private val`:
 *      `class UserService(private val userRepository: UserRepository)` (tránh lạm dụng `@Autowired lateinit var`).
 *    - **Mẫu 2 (JPA Entity & Lưu ý về `equals`/`hashCode`)**:
 *      + Tránh dùng `data class` cho JPA `@Entity` có trạng thái thay đổi hoặc quan hệ Lazy.
 *      + **Lưu ý kỹ thuật rất quan trọng về `hashCode()` của Entity**:
 *        * Trong ví dụ mô phỏng bên dưới, `UserEntity` có `val id: String` được cấp sẵn trong bộ nhớ **trước khi lưu**
 *          và không bao giờ đổi (`val`), nên viết `override fun hashCode() = id.hashCode()` là an toàn.
 *        * Tuy nhiên, trong dự án JPA thực tế dùng khóa tự tăng từ DB (`@Id @GeneratedValue var id: Long? = null`),
 *          `id` sẽ chuyển từ `null` sang số khác `null` sau khi `persist()`. Nếu dùng `id.hashCode()` trực tiếp,
 *          mã băm của đối tượng sẽ bị thay đổi khi nằm trong `HashSet` / `HashMap`! Khi đó, người ta thường dùng
 *          hằng số theo lớp (`javaClass.hashCode()`), khóa tự nhiên bất biến (`@NaturalId`), hoặc UUID sinh sẵn phía ứng dụng.
 *    - **Mẫu 3 (Request/Response DTO)**: Thường dùng `data class` với các thuộc tính `val` (bất biến nông - shallow immutability),
 *      kết hợp `@field:NotBlank`, `@field:Email`, `@field:Valid` khi làm việc với Jakarta Validation.
 *    - **Mẫu 4 (Extension Mapper)**: Dùng **Extension Functions** (`fun UserEntity.toResponse(): UserResponse`)
 *      cho các phép ánh xạ DTO <-> Entity gọn nhẹ, dễ test.
 *    - **Mẫu 5 (Null-safety trong Repository)**: Khai báo hàm truy vấn đơn lẻ trả về kiểu Nullable `UserEntity?`
 *      (hoặc dùng extension `findByIdOrNull(id)` của Spring Data Kotlin) kết hợp toán tử Elvis `?: throw ...`.
 */

// =========================================================================
// CÁC ANNOTATION MÔ PHỎNG CÚ PHÁP (PURE KOTLIN SIMULATION - KHÔNG CHẠY FRAMEWORK THẬT)
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
// 1. DOMAIN ENTITY (`open class`, KHÔNG dùng `data class`)
// =========================================================================
@Entity(tableName = "users")
open class UserEntity(
    val id: String, // ID sinh sẵn trước khi lưu và bất biến (`val`)
    var fullName: String,
    var email: String,
    var active: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // Vì `id` ở đây được gán sẵn khác null từ lúc khởi tạo và là `val` bất biến,
    // ta có thể dùng `id` cho equals/hashCode. (Nếu dùng @GeneratedValue var id: Long? = null của JPA,
    // chỉ nên so sánh khi id != null và trả về javaClass.hashCode() để giữ bất biến bucket trong Set!).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "UserEntity(id='$id', fullName='$fullName', email='$email', active=$active)"
}

// =========================================================================
// 2. REQUEST / RESPONSE DTOs (Dùng `data class` với `val` + `@field:`)
// =========================================================================
data class CreateUserRequest(
    @field:NotBlank(message = "Ho ten khong duoc de trong")
    val fullName: String,

    @field:NotBlank(message = "Email khong duoc de trong")
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
// 3. EXTENSION MAPPER (Thay thế các class Mapper boilerplate đơn giản)
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
    private val userRepository: UserRepository // Constructor Injection
) {
    private var sequence = 1

    @Transactional
    open fun registerUser(request: CreateUserRequest): UserResponse {
        val normalizedName = request.fullName.trim().replace(Regex("\\s+"), " ")
        val normalizedEmail = request.email.trim().lowercase()

        require(normalizedName.isNotEmpty()) { "Ho ten khong hop le" }
        require(normalizedEmail.contains("@")) { "Dinh dang email khong hop le: $normalizedEmail" }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw DuplicateResourceException("Email '$normalizedEmail' da duoc dang ky trong he thong!")
        }

        val generatedId = "USR-%03d".format(sequence++)
        val entity = UserEntity(
            id = generatedId,
            fullName = normalizedName,
            email = normalizedEmail
        )
        // Lưu vào kho lưu trữ và dùng Extension Function `.toResponse()` ngay trên kết quả!
        return userRepository.save(entity).toResponse()
    }

    @Transactional(readOnly = true)
    open fun getUserById(id: String): UserResponse {
        // Elvis operator `?: throw ...` thay thế gọn gàng cho `.orElseThrow(() -> ...)` của Java Optional!
        val entity = userRepository.findByIdOrNull(id.trim().uppercase())
            ?: throw ResourceNotFoundException("Khong tim thay nguoi dung voi ma: $id")
        return entity.toResponse()
    }

    @Transactional
    open fun deactivateUser(id: String): UserResponse {
        val entity = userRepository.findByIdOrNull(id.trim().uppercase())
            ?: throw ResourceNotFoundException("Khong tim thay nguoi dung voi ma: $id")
        check(entity.active) { "Tai khoan [${entity.id}] da bi vo hieu hoa tu truoc!" }
        entity.active = false
        return userRepository.save(entity).toResponse()
    }
}

// =========================================================================
// 6. CONTROLLER LAYER (`@RestController` + Exception Handling mô phỏng)
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

    // Mô phỏng cách `@RestControllerAdvice` bắt lỗi tập trung và gói vào `ApiResponse<out T>`
    private inline fun <T> executeWithExceptionHandler(action: () -> T): ApiResponse<T> = try {
        ApiResponse.ok(action())
    } catch (e: Exception) {
        // Nhờ `ApiResponse<out T>`, `ApiResponse<Nothing>` tự động tương thích với `ApiResponse<T>`!
        ApiResponse.fail("[${e::class.simpleName}] ${e.message}")
    }
}

fun main() {
    println("--- ___ * START EX03: SPRING BOOT KOTLIN PATTERNS SIMULATION * ___ ---")
    println("  [Luu y: Day la mo phong cu phap va kien truc lop bang Kotlin thuan (In-Memory),")
    println("   KHONG khoi chay Spring IoC Container, Hibernate/JPA hay HTTP Server that.]\n")

    // Khởi tạo và lắp ghép các tầng thủ công (Manual Constructor Wiring)
    val userRepository: UserRepository = InMemoryUserRepository()
    val userService = UserService(userRepository)
    val userController = UserController(userService)

    // 1. Tạo mới User hợp lệ
    println("1. Goi createUserEndpoint (Hop le):")
    val res1 = userController.createUserEndpoint(
        CreateUserRequest(fullName = "  Nguyen   Minh An ", email = " AN.NGUYEN@Example.com ")
    )
    println("  -> Response: $res1")
    check(res1.success && res1.data?.id == "USR-001" && res1.data.email == "an.nguyen@example.com")

    // 2. Tạo trùng Email -> Bị chặn bởi DuplicateResourceException
    println("\n2. Goi createUserEndpoint (Trung Email):")
    val resDuplicate = userController.createUserEndpoint(
        CreateUserRequest(fullName = "Nguyen Van An Khac", email = "an.nguyen@example.com")
    )
    println("  -> Response: $resDuplicate")
    check(!resDuplicate.success && resDuplicate.errorMessage?.contains("DuplicateResourceException") == true)

    // 3. Tra cứu User theo ID (Thành công & Không tìm thấy với Elvis `?: throw`)
    println("\n3. Goi getUserEndpoint:")
    val foundRes = userController.getUserEndpoint("usr-001")
    val missingRes = userController.getUserEndpoint("USR-999")
    println("  - Tim USR-001: $foundRes")
    println("  - Tim USR-999: $missingRes")
    check(foundRes.success && !missingRes.success)

    // 4. Vô hiệu hóa tài khoản & kiểm tra `check(entity.active)`
    println("\n4. Goi deactivateUserEndpoint:")
    val deact1 = userController.deactivateUserEndpoint("USR-001")
    val deact2 = userController.deactivateUserEndpoint("USR-001")
    println("  - Lan 1 (Hop le): $deact1")
    println("  - Lan 2 (Vi pham `check` state): $deact2")
    check(deact1.success && deact1.data?.statusLabel == "DEACTIVATED")
    check(!deact2.success && deact2.errorMessage?.contains("IllegalStateException") == true)

    // =========================================================================
    // 5. GÓC TỰ LUYỆN (ACTIVE LEARNING)
    // =========================================================================
    println("\n5. Goc Tu Luyen (Active Learning):")
    // [ĐỀ BÀI]:
    // Viết một Extension Function `fun List<UserEntity>.toActiveResponses(): List<UserResponse>`
    // chỉ lọc ra các `UserEntity` đang `active == true` và ánh xạ sang `UserResponse` bằng `.toResponse()`.
    userController.createUserEndpoint(CreateUserRequest("Tran Binh", "binh@example.com"))
    val activeUsers = userRepository.findAll().toActiveResponses()
    println("  - Danh sach User dang ACTIVE sau khi loc & map: $activeUsers")
    check(activeUsers.size == 1 && activeUsers.first().email == "binh@example.com")
    println("  => [PASS] Goc Tu Luyen Ex03 hoan tat!")

    println("\n--- ___ * END EX03 * ___ ---")
}

// [LỜI GIẢI THAM KHẢO CHO GÓC TỰ LUYỆN]:
fun List<UserEntity>.toActiveResponses(): List<UserResponse> =
    this.filter { it.active }.map { it.toResponse() }
